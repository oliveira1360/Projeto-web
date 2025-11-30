package org.example.game

import org.example.entity.PlayerGameInfo
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
import org.example.user.RepositoryUserMem
import java.sql.Time
import java.util.PriorityQueue

data class GameData(
    val id: Int,
    val totalRounds: Int,
    var status: String = "ACTIVE",
    val playerIds: MutableList<Int> = mutableListOf(),
    val rounds: MutableList<RoundData> = mutableListOf(),
    var winnerId: Int? = null,
    val startedAt: Long = System.currentTimeMillis(),
)

data class RoundData(
    val roundNumber: Int,
    var winnerId: Int? = null,
    val playerOrder: MutableList<Int> = mutableListOf(),
    // Mapa: UserId -> TurnData
    val turns: MutableMap<Int, TurnData> = mutableMapOf(),
)

data class TurnData(
    val userId: Int,
    var hand: Hand? = null,
    var rollNumber: Int = 0,
    var score: Int? = null,
    var finished: Boolean = false,
)

data class PlayerStatsData(
    var totalGames: Int = 0,
    var totalWins: Int = 0,
    var totalLosses: Int = 0,
    var totalPoints: Int = 0,
    var currentStreak: Int = 0,
    var longestWinStreak: Int = 0,
)

class RepositoryGameMem : RepositoryGame {
    companion object {
        private val games = mutableMapOf<Int, GameData>()
        private var nextGameId = 1

        private val stats = mutableMapOf<Int, PlayerStatsData>()

        private val userRepo = RepositoryUserMem()
    }

    private fun getGame(gameId: Int): GameData = games[gameId] ?: throw IllegalStateException("Game $gameId not found")

    private fun getRound(
        gameId: Int,
        roundNumber: Int,
    ): RoundData =
        getGame(gameId).rounds.find { it.roundNumber == roundNumber }
            ?: throw IllegalStateException("Round $roundNumber not found")

    private fun getTurn(
        gameId: Int,
        roundNumber: Int,
        userId: Int,
    ): TurnData =
        getRound(gameId, roundNumber).turns[userId]
            ?: throw IllegalStateException("Turn not found")

    override fun findById(id: Int): Game? {
        val gameData = games[id] ?: return null

        val playersList =
            gameData.playerIds.mapNotNull { userId ->
                userRepo.findById(userId)
            }

        val roundsList =
            gameData.rounds.map { roundData ->

                val currentTurnUserId =
                    roundData.playerOrder.firstOrNull { userId ->
                        val turn = roundData.turns[userId]
                        turn == null || !turn.finished
                    } ?: -1

                val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

                gameData.playerIds.forEach { userId ->
                    val user = userRepo.findById(userId)
                    if (user != null) {
                        val totalScore =
                            gameData.rounds.sumOf { r ->
                                r.turns[userId]?.score ?: 0
                            }

                        val turnData = roundData.turns[userId]

                        val playerInfo =
                            PlayerGameInfo(
                                playerId = user.id,
                                name = user.name,
                                rolls = Quantity(turnData?.rollNumber ?: 0),
                                hand = turnData?.hand ?: Hand(emptyList()),
                                balance = user.balance,
                            )

                        pointsQueue.add(PointPlayer(playerInfo, Points(totalScore)))
                    }
                }

                RoundInfo(
                    round = Round(roundData.roundNumber),
                    pointsQueue = pointsQueue,
                    roundOrder = roundData.playerOrder,
                    turn = currentTurnUserId,
                )
            }

        return Game(
            playersGameInfoList = playersList,
            rounds = roundsList,
        )
    }

    override fun findAll(): List<Game> = emptyList()

    override fun listPlayersInGame(gameId: Int): ListPlayersInGame {
        val game = getGame(gameId)
        val list =
            game.playerIds.mapNotNull { userId ->
                val user = userRepo.findById(userId) ?: return@mapNotNull null

                val currentRound = getCurrentRoundNumber(gameId)
                val turn =
                    if (currentRound != null) {
                        try {
                            getTurn(gameId, currentRound, userId)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                PlayerGameInfo(
                    playerId = user.id,
                    name = user.name,
                    rolls = Quantity(turn?.rollNumber ?: 0),
                    hand = turn?.hand ?: Hand(emptyList()),
                    balance = user.balance,
                )
            }
        return ListPlayersInGame(list)
    }

    override fun getScores(gameId: Int): Scoreboard {
        val game = getGame(gameId)
        val queue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

        game.playerIds.forEach { userId ->
            val user = userRepo.findById(userId)
            if (user != null) {
                val totalScore = game.rounds.sumOf { r -> r.turns[userId]?.score ?: 0 }

                queue.add(
                    PointPlayer(
                        player =
                            PlayerGameInfo(
                                user.id,
                                user.name,
                                Quantity(0),
                                Hand(emptyList()),
                                user.balance,
                            ),
                        points = Points(totalScore),
                    ),
                )
            }
        }
        return Scoreboard(queue)
    }

    override fun remainingTime(gameId: Int): Time {
        val game = getGame(gameId)
        val elapsed = System.currentTimeMillis() - game.startedAt
        val remaining = (30000 - elapsed).coerceAtLeast(0)
        return Time(remaining)
    }

    override fun getTotalRoundsOfGame(gameId: Int): Int = getGame(gameId).totalRounds

    override fun getCurrentRoundNumber(gameId: Int): Int? = games[gameId]?.rounds?.maxOfOrNull { it.roundNumber }

    override fun hasActiveRound(gameId: Int): Boolean = (getCurrentRoundNumber(gameId) ?: 0) > 0

    override fun getRoundInfo(gameId: Int): RoundInfo {
        val roundNumber = getCurrentRoundNumber(gameId) ?: 0
        val roundOrder = if (roundNumber > 0) getRoundOrder(gameId) else emptyList()
        val turn = getCurrentPlayerTurn(gameId)

        return RoundInfo(
            round = Round(roundNumber),
            pointsQueue = PriorityQueue(),
            roundOrder = roundOrder,
            turn = turn,
        )
    }

    override fun getRoundOrder(gameId: Int): List<Int> {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return emptyList()
        return getRound(gameId, roundNumber).playerOrder
    }

    override fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand? {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return null
        return try {
            getTurn(gameId, roundNumber, userId).hand
        } catch (e: Exception) {
            null
        }
    }

    override fun getRollCount(
        userId: Int,
        gameId: Int,
    ): Int {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return 0
        return try {
            getTurn(gameId, roundNumber, userId).rollNumber
        } catch (e: Exception) {
            0
        }
    }

    override fun getCurrentPlayerTurn(gameId: Int): Int {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return -1
        val round = getRound(gameId, roundNumber)

        for (userId in round.playerOrder) {
            val turn = round.turns[userId]
            if (turn != null && !turn.finished) {
                return userId
            }
        }
        return -1
    }

    override fun insertMatch(totalRounds: Int): Int {
        val id = nextGameId++
        games[id] = GameData(id = id, totalRounds = totalRounds)
        return id
    }

    override fun insertMatchPlayer(
        matchId: Int,
        userId: Int,
        seatNumber: Int,
    ) {
        val game = getGame(matchId)
        if (!game.playerIds.contains(userId)) {
            game.playerIds.add(userId)
        }
    }

    override fun deductBalance(
        matchId: Int,
        amount: Int,
    ) {
        val game = getGame(matchId)
        game.playerIds.forEach { userId ->
            userRepo.updateBalance(userId, -amount)
        }
    }

    override fun closeGame(gameId: Int) {
        val game = getGame(gameId)
        game.status = "FINISHED"
    }

    override fun setGameWinnerAndFinish(
        gameId: Int,
        winnerId: Int,
    ) {
        val game = getGame(gameId)
        game.winnerId = winnerId
        game.status = "FINISHED"
    }

    override fun insertRound(gameId: Int): Int {
        val game = getGame(gameId)
        val nextRound = (getCurrentRoundNumber(gameId) ?: 0) + 1
        val roundData = RoundData(roundNumber = nextRound)
        game.rounds.add(roundData)
        return nextRound
    }

    override fun initTurn(
        gameId: Int,
        roundNumber: Int,
    ) {
    }

    override fun insertRoundOrder(
        gameId: Int,
        roundNumber: Int,
        position: Int,
        userId: Int,
    ) {
        val round = getRound(gameId, roundNumber)
        round.playerOrder.add(userId)
    }

    override fun deleteRoundOrder(
        gameId: Int,
        roundNumber: Int,
    ) {
        val round = getRound(gameId, roundNumber)
        round.playerOrder.clear()
    }

    override fun setRoundOrder(
        gameId: Int,
        roundNumber: Int,
        playerOrder: List<Int>,
    ) {
        val round = getRound(gameId, roundNumber)
        round.playerOrder.clear()
        round.playerOrder.addAll(playerOrder)
    }

    override fun populateEmptyTurns(
        matchId: Int,
        roundNumber: Int,
        userId: Int,
    ) {
        val round = getRound(matchId, roundNumber)
        if (!round.turns.containsKey(userId)) {
            round.turns[userId] = TurnData(userId = userId)
        }
    }

    override fun setRoundWinner(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        val round = getRound(gameId, roundNumber)
        round.winnerId = winnerId
    }

    override fun updateHandAndRoll(
        userId: Int,
        gameId: Int,
        newHand: Hand,
        newRollNumber: Int,
    ): Hand {
        val roundNumber = getCurrentRoundNumber(gameId)!!
        val turn = getTurn(gameId, roundNumber, userId)
        turn.hand = newHand
        turn.rollNumber = newRollNumber
        return newHand
    }

    override fun updateScore(
        userId: Int,
        gameId: Int,
        points: Points,
    ) {
        val roundNumber = getCurrentRoundNumber(gameId)!!
        val turn = getTurn(gameId, roundNumber, userId)
        turn.score = points.points
    }

    override fun markTurnAsFinished(
        userId: Int,
        gameId: Int,
    ) {
        val roundNumber = getCurrentRoundNumber(gameId)!!
        val turn = getTurn(gameId, roundNumber, userId)
        turn.finished = true
    }

    override fun findRoundWinnerCandidate(gameId: Int): RoundWinnerInfo? {
        val roundNumber = getCurrentRoundNumber(gameId) ?: return null
        val round = getRound(gameId, roundNumber)

        val candidates = round.turns.values.filter { it.finished && it.score != null }
        if (candidates.isEmpty()) return null

        val winnerTurn =
            candidates
                .sortedWith(
                    compareByDescending<TurnData> { it.score }
                        .thenByDescending {
                            it.hand?.value?.sumOf { d ->
                                when (d.face.name) {
                                    "ACE" -> 1
                                    "KING" -> 2
                                    "QUEEN" -> 3
                                    "JACK" -> 4
                                    "TEN" -> 5
                                    "NINE" -> 6
                                    else -> 0
                                }
                            } ?: 0
                        },
                ).first()

        val user = userRepo.findById(winnerTurn.userId) ?: return null

        return RoundWinnerInfo(
            player =
                PlayerGameInfo(
                    user.id,
                    user.name,
                    Quantity(winnerTurn.rollNumber),
                    winnerTurn.hand!!,
                    user.balance,
                ),
            points = Points(winnerTurn.score!!),
            handValue = winnerTurn.hand!!.evaluateHandValue(),
            roundNumber = roundNumber,
        )
    }

    override fun rewardPlayer(userId: Int) {
        userRepo.updateBalance(userId, 2)
    }

    override fun getPlayerRoundScore(
        gameId: Int,
        roundNumber: Int,
        userId: Int,
    ): Int =
        try {
            getTurn(gameId, roundNumber, userId).score ?: 0
        } catch (e: Exception) {
            0
        }

    private fun getStats(userId: Int) = stats.computeIfAbsent(userId) { PlayerStatsData() }

    override fun updateStatsRoundWinner(
        userId: Int,
        points: Int,
    ) {
        val s = getStats(userId)
        s.totalPoints += points
    }

    override fun updateStatsRoundLosers(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        val round = getRound(gameId, roundNumber)
        round.turns.forEach { (uid, turn) ->
            if (uid != winnerId) {
                val s = getStats(uid)
                s.totalPoints += (turn.score ?: 0)
            }
        }
    }

    override fun findGameWinner(gameId: Int): GameWinnerInfo? {
        val game = getGame(gameId)

        val finalStats =
            game.playerIds.map { userId ->
                val totalScore = game.rounds.sumOf { it.turns[userId]?.score ?: 0 }
                val roundsWon = game.rounds.count { it.winnerId == userId }
                Triple(userId, totalScore, roundsWon)
            }

        val winnerData =
            finalStats
                .sortedWith(
                    compareByDescending<Triple<Int, Int, Int>> { it.second }
                        .thenByDescending { it.third },
                ).firstOrNull() ?: return null

        val user = userRepo.findById(winnerData.first)!!

        return GameWinnerInfo(
            player = PlayerGameInfo(user.id, user.name, Quantity(0), Hand(emptyList()), user.balance),
            totalPoints = Points(winnerData.second),
            roundsWon = winnerData.third,
        )
    }

    override fun getFinalScoresRaw(
        gameId: Int,
        winnerId: Int,
    ): List<Triple<Int, Int, Boolean>> {
        val game = getGame(gameId)
        return game.playerIds.map { userId ->
            val totalScore = game.rounds.sumOf { it.turns[userId]?.score ?: 0 }
            Triple(userId, totalScore, userId == winnerId)
        }
    }

    override fun updateStatsGameWinner(
        userId: Int,
        points: Int,
    ) {
        val s = getStats(userId)
        s.totalGames++
        s.totalWins++
        s.currentStreak++
        s.longestWinStreak = maxOf(s.longestWinStreak, s.currentStreak)
    }

    override fun updateStatsGameLoser(
        userId: Int,
        points: Int,
    ) {
        val s = getStats(userId)
        s.totalGames++
        s.totalLosses++
        s.currentStreak = 0
    }

    override fun save(entity: Game) {}

    override fun deleteById(id: Int) {
        games.remove(id)
    }

    override fun clear() {
        games.clear()
        stats.clear()
    }
}
