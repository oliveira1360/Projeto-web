package org.example.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Balance
import org.example.entity.core.Money
import org.example.entity.core.Name
import org.example.entity.core.Points
import org.example.entity.core.Quantity
import org.example.entity.game.Game
import org.example.entity.game.GameWinnerInfo
import org.example.entity.game.Round
import org.example.entity.game.RoundInfo
import org.example.entity.game.RoundWinnerInfo
import org.example.entity.game.Scoreboard
import org.example.entity.lobby.ListPlayersInGame
import org.example.entity.player.Hand
import org.example.entity.player.PointPlayer
import org.example.entity.player.User
import org.example.game.mappers.HandMapper
import org.example.game.mappers.PlayerGameInfoMapper
import org.example.game.mappers.PlayerScoreMapper
import org.example.game.sql.GameSqlRead
import org.example.game.sql.GameSqlWrite
import org.example.game.sql.RoundSql
import org.example.game.sql.StatsSql
import org.example.game.sql.TurnSql
import org.example.mapper.UserMapper
import org.jdbi.v3.core.Handle
import java.sql.Time
import java.util.PriorityQueue

class RepositoryGameJDBI(
    private val handle: Handle,
) : RepositoryGame {
    override fun findById(id: Int): Game? {
        val matchExists =
            handle
                .createQuery(GameSqlRead.FIND_BY_ID)
                .bind("id", id)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(null) ?: return null

        val players = fetchPlayers(id)

        val rounds = fetchRounds(id, players)

        return Game(
            playersGameInfoList = players,
            rounds = rounds,
        )
    }

    override fun findAll(): List<Game> {
        val matchIds =
            handle
                .createQuery(GameSqlRead.FIND_ALL)
                .mapTo(Int::class.java)
                .list()

        return matchIds.mapNotNull { matchId ->
            findById(matchId)
        }
    }

    override fun listPlayersInGame(gameId: Int): ListPlayersInGame =
        ListPlayersInGame(
            handle
                .createQuery(GameSqlRead.LIST_PLAYERS_IN_GAME)
                .bind("gameId", gameId)
                .map(PlayerGameInfoMapper())
                .list(),
        )

    override fun getScores(gameId: Int): Scoreboard =
        handle
            .createQuery(GameSqlRead.GET_SCORES)
            .bind("gameId", gameId)
            .map(PlayerScoreMapper())
            .list()
            .let { pointPlayers ->
                val queue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })
                queue.addAll(pointPlayers)
                Scoreboard(queue)
            }

    override fun remainingTime(gameId: Int): Time =
        handle
            .createQuery(GameSqlRead.REMAINING_TIME)
            .bind("gameId", gameId)
            .mapTo(Long::class.java)
            .one()
            .let { Time(it) }

    override fun getTotalRoundsOfGame(gameId: Int): Int =
        handle
            .createQuery(GameSqlRead.GET_TOTAL_ROUNDS)
            .bind("gameId", gameId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)

    override fun insertMatch(totalRounds: Int): Int =
        handle
            .createUpdate(GameSqlWrite.INSERT_MATCH)
            .bind("totalRounds", totalRounds)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

    override fun insertMatchPlayer(
        matchId: Int,
        userId: Int,
        seatNumber: Int,
    ) {
        handle
            .createUpdate(GameSqlWrite.INSERT_MATCH_PLAYER)
            .bind("matchId", matchId)
            .bind("userId", userId)
            .bind("seatNumber", seatNumber)
            .execute()
    }

    override fun deductBalance(
        matchId: Int,
        amount: Int,
    ) {
        handle
            .createUpdate(GameSqlWrite.DEDUCT_BALANCE)
            .bind("betAmount", amount)
            .bind("matchId", matchId)
            .execute()
    }

    override fun closeGame(gameId: Int) {
        handle
            .createUpdate(GameSqlWrite.CLOSE_GAME)
            .bind("gameId", gameId)
            .execute()
    }

    override fun insertRound(gameId: Int): Int =
        handle
            .createUpdate(RoundSql.INSERT_ROUND)
            .bind("gameId", gameId)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

    override fun initTurn(
        gameId: Int,
        roundNumber: Int,
    ) {
        handle
            .createUpdate(TurnSql.INIT_TURN)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .execute()
    }

    override fun insertRoundOrder(
        gameId: Int,
        roundNumber: Int,
        position: Int,
        userId: Int,
    ) {
        handle
            .createUpdate(RoundSql.INSERT_ROUND_ORDER)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("position", position)
            .bind("userId", userId)
            .execute()
    }

    override fun deleteRoundOrder(
        gameId: Int,
        roundNumber: Int,
    ) {
        handle
            .createUpdate(RoundSql.DELETE_ROUND_ORDER)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .execute()
    }

    override fun getCurrentRoundNumber(gameId: Int): Int? =
        handle
            .createQuery(RoundSql.GET_CURRENT_ROUND_NUMBER)
            .bind("gameId", gameId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(null)

    override fun hasActiveRound(gameId: Int): Boolean {
        val r = getCurrentRoundNumber(gameId)
        return r != null && r > 0
    }

    override fun getRoundInfo(gameId: Int): RoundInfo {
        val roundNumber =
            handle
                .createQuery(RoundSql.GET_LATEST_ROUND_INFO)
                .bind("gameId", gameId)
                .mapTo(Int::class.java)
                .findOne()
                .orElse(0)

        val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

        val roundOrder = getRoundOrder(gameId)
        val turn = getCurrentPlayerTurn(gameId)
        val maxRoundNumber = getTotalRoundsOfGame(gameId)

        return RoundInfo(
            round = Round(roundNumber),
            totalRounds = Round(maxRoundNumber),
            pointsQueue = pointsQueue,
            roundOrder = roundOrder,
            turn = turn,
        )
    }

    override fun getRoundOrder(gameId: Int): List<Int> {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return emptyList()
        return handle
            .createQuery(RoundSql.GET_ROUND_ORDER)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .mapTo(Int::class.java)
            .list()
    }

    override fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand? {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return null
        return handle
            .createQuery(TurnSql.GET_PLAYER_HAND)
            .bind("userId", userId)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .map(HandMapper())
            .findOne()
            .orElse(null)
    }

    override fun getRollCount(
        userId: Int,
        gameId: Int,
    ): Int {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return 0
        return handle
            .createQuery(TurnSql.GET_ROLL_COUNT)
            .bind("gameId", gameId)
            .bind("userId", userId)
            .bind("roundNumber", roundNumber)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)
    }

    override fun updateHandAndRoll(
        userId: Int,
        gameId: Int,
        newHand: Hand,
        newRollNumber: Int,
    ): Hand {
        val roundNumber = getCurrentRoundNumber(gameId) ?: throw IllegalStateException("No round")

        return handle
            .createQuery(TurnSql.UPDATE_HAND_AND_ROLL)
            .bind("newHand", newHand.value.map { it.face.name }.toTypedArray())
            .bind("newRollNumber", newRollNumber)
            .bind("gameId", gameId)
            .bind("userId", userId)
            .bind("roundNumber", roundNumber)
            .map(HandMapper())
            .one()
    }

    override fun updateScore(
        userId: Int,
        gameId: Int,
        points: Points,
    ) {
        val roundNumber = getCurrentRoundNumber(gameId) ?: throw IllegalStateException("No round")
        handle
            .createUpdate(TurnSql.UPDATE_SCORE)
            .bind("userId", userId)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("score", points.points)
            .execute()
    }

    override fun markTurnAsFinished(
        userId: Int,
        gameId: Int,
    ) {
        val roundNumber = getCurrentRoundNumber(gameId) ?: throw IllegalStateException("No round")
        handle
            .createUpdate(TurnSql.MARK_TURN_FINISHED)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("userId", userId)
            .execute()
    }

    override fun populateEmptyTurns(
        matchId: Int,
        roundNumber: Int,
        userId: Int,
    ) {
        handle
            .createUpdate(TurnSql.INSERT_EMPTY_TURN)
            .bind("matchId", matchId)
            .bind("roundNumber", roundNumber)
            .bind("userId", userId)
            .execute()
    }

    override fun getCurrentPlayerTurn(gameId: Int): Int {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return -1
        return handle
            .createQuery(TurnSql.GET_CURRENT_TURN)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(-1)
    }

    override fun findRoundWinnerCandidate(gameId: Int): RoundWinnerInfo? {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return null

        return handle
            .createQuery(RoundSql.FIND_ROUND_WINNER_CANDIDATE)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .map { rs, ctx ->
                val playerInfo = PlayerGameInfoMapper().map(rs, ctx)
                val score = rs.getInt("score")
                val hand = playerInfo.hand
                val handValue = hand.evaluateHandValue()

                RoundWinnerInfo(
                    player = playerInfo,
                    points = Points(score),
                    handValue = handValue,
                    roundNumber = roundNumber,
                )
            }.findOne()
            .orElse(null)
    }

    override fun setRoundWinner(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        handle
            .createUpdate(RoundSql.SET_ROUND_WINNER)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("winnerId", winnerId)
            .execute()
    }

    override fun rewardPlayer(userId: Int) {
        handle
            .createUpdate(RoundSql.REWARD_WINNER)
            .bind("winnerId", userId)
            .execute()
    }

    override fun getPlayerRoundScore(
        gameId: Int,
        roundNumber: Int,
        userId: Int,
    ): Int =
        handle
            .createQuery(TurnSql.GET_WINNER_SCORE)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("winnerId", userId)
            .mapTo(Int::class.java)
            .findOne()
            .orElse(0)

    override fun updateStatsRoundWinner(
        userId: Int,
        points: Int,
    ) {
        handle
            .createUpdate(StatsSql.UPDATE_STATS_ROUND_WINNER)
            .bind("userId", userId)
            .bind("points", points)
            .execute()
    }

    override fun updateStatsRoundLosers(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        handle
            .createUpdate(StatsSql.UPDATE_STATS_ROUND_LOSER)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .bind("winnerId", winnerId)
            .execute()
    }

    override fun setRoundOrder(
        gameId: Int,
        roundNumber: Int,
        playerOrder: List<Int>,
    ) {
        handle
            .createUpdate(RoundSql.DELETE_ROUND_ORDER)
            .bind("gameId", gameId)
            .bind("roundNumber", roundNumber)
            .execute()

        playerOrder.forEachIndexed { index, userId ->
            handle
                .createUpdate(RoundSql.INSERT_ROUND_ORDER)
                .bind("gameId", gameId)
                .bind("roundNumber", roundNumber)
                .bind("position", index + 1)
                .bind("userId", userId)
                .execute()
        }
    }

    override fun findGameWinner(gameId: Int): GameWinnerInfo? =
        handle
            .createQuery(GameSqlRead.FIND_GAME_WINNER)
            .bind("gameId", gameId)
            .map { rs, ctx ->
                val playerInfo =
                    PlayerGameInfo(
                        playerId = rs.getInt("user_id"),
                        name =

                            Name(rs.getString("username")),
                        rolls = Quantity(0),
                        hand = Hand(emptyList()),
                        balance = Balance(Money(rs.getInt("balance"))),
                    )
                GameWinnerInfo(
                    player = playerInfo,
                    totalPoints = Points(rs.getInt("total_score")),
                    roundsWon = rs.getInt("rounds_won"),
                )
            }.findOne()
            .orElse(null)

    override fun setGameWinnerAndFinish(
        gameId: Int,
        winnerId: Int,
    ) {
        handle
            .createUpdate(GameSqlWrite.FINISH_MATCH_AND_SET_WINNER)
            .bind("gameId", gameId)
            .bind("winnerId", winnerId)
            .execute()
    }

    override fun getFinalScoresRaw(
        gameId: Int,
        winnerId: Int,
    ): List<Triple<Int, Int, Boolean>> =
        handle
            .createQuery(GameSqlRead.GET_FINAL_SCORES)
            .bind("gameId", gameId)
            .bind("winnerId", winnerId)
            .map { rs, _ ->
                Triple(
                    rs.getInt("user_id"),
                    rs.getInt("total_score"),
                    rs.getInt("is_winner") == 1,
                )
            }.list()

    override fun updateStatsGameWinner(
        userId: Int,
        points: Int,
    ) {
        handle
            .createUpdate(StatsSql.UPDATE_STATS_GAME_WINNER)
            .bind("userId", userId)
            .bind("points", points)
            .execute()
    }

    override fun updateStatsGameLoser(
        userId: Int,
        points: Int,
    ) {
        handle
            .createUpdate(StatsSql.UPDATE_STATS_GAME_LOSER)
            .bind("userId", userId)
            .bind("points", points)
            .execute()
    }

    override fun save(entity: Game) {}

    override fun deleteById(id: Int) {
        handle.createUpdate(GameSqlWrite.DELETE_BY_ID).bind("id", id).execute()
    }

    override fun clear() {
        handle.createUpdate(GameSqlWrite.CLEAR_ALL).execute()
    }

    private fun fetchPlayers(matchId: Int): List<User> =
        handle
            .createQuery(GameSqlRead.FIND_PLAYERS_BY_MATCH)
            .bind("matchId", matchId)
            .map(UserMapper())
            .list()

    private fun fetchRounds(
        matchId: Int,
        players: List<User>,
    ): List<RoundInfo> {
        val roundNumbers =
            handle
                .createQuery(GameSqlRead.FIND_ROUNDS_BY_MATCH)
                .bind("matchId", matchId)
                .mapTo(Int::class.java)
                .list()

        return roundNumbers.map { roundNum ->
            buildRoundInfo(matchId, roundNum, players)
        }
    }

    private fun buildRoundInfo(
        matchId: Int,
        roundNumber: Int,
        players: List<User>,
    ): RoundInfo {
        val scores = fetchRoundScores(matchId, roundNumber)
        val pointsQueue = buildPointsQueue(scores, players, matchId, roundNumber)

        return RoundInfo(
            round = Round(roundNumber),
            totalRounds = Round(getTotalRoundsOfGame(matchId)),
            pointsQueue = pointsQueue,
            roundOrder = emptyList(),
            turn = 0,
        )
    }

    private fun fetchRoundScores(
        matchId: Int,
        roundNumber: Int,
    ): List<Pair<Int, Int>> =
        handle
            .createQuery(GameSqlRead.FIND_ROUND_SCORES)
            .bind("matchId", matchId)
            .bind("roundNumber", roundNumber)
            .map { rs, _ ->
                val userId = rs.getInt("user_id")
                val score = rs.getInt("score")
                userId to score
            }.list()

    private fun buildPointsQueue(
        scores: List<Pair<Int, Int>>,
        players: List<User>,
        matchId: Int,
        roundNumber: Int,
    ): PriorityQueue<PointPlayer> {
        val queue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

        scores.forEach { (userId, score) ->
            val player = players.find { it.id == userId }
            if (player != null) {
                queue.add(
                    PointPlayer(
                        player =
                            PlayerGameInfo(
                                playerId = userId,
                                name = player.name,
                                rolls = fetchPlayerRolls(matchId, roundNumber, userId),
                                hand = fetchPlayerHand(matchId, roundNumber, userId),
                                balance = player.balance,
                            ),
                        points = Points(score),
                    ),
                )
            }
        }

        return queue
    }

    private fun fetchPlayerRolls(
        matchId: Int,
        roundNumber: Int,
        userId: Int,
    ): Quantity =
        handle
            .createQuery(TurnSql.GET_ROLL_COUNT)
            .bind("gameId", matchId)
            .bind("userId", userId)
            .bind("roundNumber", roundNumber)
            .mapTo(Int::class.java)
            .findOne()
            .map { Quantity(it) }
            .orElse(Quantity(0))

    private fun fetchPlayerHand(
        matchId: Int,
        roundNumber: Int,
        userId: Int,
    ): Hand =
        handle
            .createQuery(TurnSql.GET_PLAYER_HAND)
            .bind("userId", userId)
            .bind("gameId", matchId)
            .bind("roundNumber", roundNumber)
            .map(HandMapper())
            .findOne()
            .orElse(Hand(emptyList()))
}
