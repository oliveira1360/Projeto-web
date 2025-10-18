package org.example.game

import org.example.entity.PlayerGameInfo
import org.example.entity.core.Points
import org.example.entity.core.toQuantity
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
    val lobbyId: Int,
    val totalRounds: Int,
    val status: String = "ACTIVE",
    val players: MutableList<Int> = mutableListOf(),
    val rounds: MutableList<RoundData> = mutableListOf(),
    val winnerId: Int? = null,
    val startedAt: Long = System.currentTimeMillis(),
)

data class RoundData(
    val gameId: Int,
    val roundNumber: Int,
    val turns: MutableMap<Int, TurnData> = mutableMapOf(),
    val winnerId: Int? = null,
)

data class TurnData(
    val userId: Int,
    val hand: Hand?,
    val rollNumber: Int = 0,
    val score: Int? = null,
)

class RepositoryGameMem : RepositoryGame {
    companion object {
        private val games = mutableMapOf<Int, GameData>()
        private var nextGameId = 1
        val userRepo = RepositoryUserMem()
    }

    override fun createGame(
        userId: Int,
        lobbyId: Int,
    ): Int {
        val gameId = nextGameId++
        val game =
            GameData(
                id = gameId,
                lobbyId = lobbyId,
                totalRounds = 12,
                players = mutableListOf(userId),
            )
        games[gameId] = game

        return gameId
    }

    override fun closeGame(
        userId: Int,
        gameId: Int,
    ) {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId")
        games[gameId] = game.copy(status = "FINISHED")
    }

    override fun listPlayersInGame(gameId: Int): ListPlayersInGame {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId")
        val currentRound = game.rounds.maxByOrNull { it.roundNumber }

        val players =
            game.players.mapNotNull { userId ->
                val user = userRepo.findById(userId) ?: return@mapNotNull null
                val turn = currentRound?.turns?.get(userId)

                PlayerGameInfo(
                    playerId = userId,
                    rolls = (turn?.rollNumber ?: 0).toQuantity(),
                    hand = turn?.hand ?: Hand(emptyList()),
                    balance = user.balance,
                )
            }

        return ListPlayersInGame(players)
    }

    override fun startRound(gameId: Int): Int {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId")
        val nextRoundNumber = (game.rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1
        val round = RoundData(gameId, nextRoundNumber)
        game.rounds.add(round)
        return game.rounds.size // todo
    }

    override fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Hand? {
        val game = games[gameId] ?: return null
        val currentRound = game.rounds.maxByOrNull { it.roundNumber } ?: return null
        return currentRound.turns[userId]?.hand
    }

    override fun shuffle(
        userId: Int,
        newHand: Hand,
        gameId: Int,
    ): Hand {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")
        val currentRound =
            game.rounds.maxByOrNull { it.roundNumber }
                ?: throw IllegalStateException("No active round")

        val existingTurn = currentRound.turns[userId]
        val newRollNumber = (existingTurn?.rollNumber ?: 0) + 1

        currentRound.turns[userId] =
            TurnData(
                userId = userId,
                hand = newHand,
                rollNumber = newRollNumber,
                score = existingTurn?.score,
            )

        return newHand
    }

    override fun calculatePoints(
        userId: Int,
        gameId: Int,
        points: Points,
    ) {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")
        val currentRound =
            game.rounds.maxByOrNull { it.roundNumber }
                ?: throw IllegalStateException("No active round")

        val turn =
            currentRound.turns[userId]
                ?: throw IllegalStateException("No turn found for user")

        currentRound.turns[userId] = turn.copy(score = points.points)
    }

    override fun getRoundWinner(gameId: Int): RoundWinnerInfo {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")
        val currentRound =
            game.rounds.maxByOrNull { it.roundNumber }
                ?: throw IllegalStateException("No rounds found")

        val winnerEntry =
            currentRound.turns.entries
                .filter { it.value.score != null }
                .maxByOrNull { it.value.score!! }
                ?: throw IllegalStateException("No completed turns found")

        val winnerId = winnerEntry.key
        val turn = winnerEntry.value

        updateRoundWinner(gameId, currentRound.roundNumber, winnerId)

        val user =
            userRepo.findById(winnerId)
                ?: throw IllegalStateException("User not found")

        val playerInfo =
            PlayerGameInfo(
                playerId = winnerId,
                rolls = turn.rollNumber.toQuantity(),
                hand = turn.hand ?: Hand(emptyList()),
                balance = user.balance,
            )

        val hand = turn.hand ?: Hand(emptyList())

        return RoundWinnerInfo(
            player = playerInfo,
            points = Points(turn.score ?: 0),
            handValue = hand.evaluateHandValue(),
            roundNumber = currentRound.roundNumber,
        )
    }

    override fun updateRoundWinner(
        gameId: Int,
        roundNumber: Int,
        winnerId: Int,
    ) {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")
        val round =
            game.rounds.find { it.roundNumber == roundNumber }
                ?: throw IllegalStateException("Round not found")

        game.rounds[game.rounds.indexOf(round)] = round.copy(winnerId = winnerId)
    }

    override fun getGameWinner(gameId: Int): GameWinnerInfo {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")

        val playerScores =
            game.players.map { playerId ->
                val totalScore =
                    game.rounds.sumOf { round ->
                        round.turns[playerId]?.score ?: 0
                    }
                val roundsWon = game.rounds.count { it.winnerId == playerId }
                Triple(playerId, totalScore, roundsWon)
            }

        val winner =
            playerScores.maxWithOrNull(
                compareBy<Triple<Int, Int, Int>> { it.second }
                    .thenBy { it.third },
            ) ?: throw IllegalStateException("No players found")

        updateGameWinner(gameId, winner.first)

        val user =
            userRepo.findById(winner.first)
                ?: throw IllegalStateException("User not found")

        val playerInfo =
            PlayerGameInfo(
                playerId = winner.first,
                rolls = 0.toQuantity(),
                hand = Hand(emptyList()),
                balance = user.balance,
            )

        return GameWinnerInfo(
            player = playerInfo,
            totalPoints = Points(winner.second),
            roundsWon = winner.third,
        )
    }

    override fun updateGameWinner(
        gameId: Int,
        winnerId: Int,
    ) {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")
        games[gameId] = game.copy(winnerId = winnerId, status = "FINISHED")
    }

    override fun getRollCount(
        userId: Int,
        gameId: Int,
    ): Int {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId - did it escape?")
        val currentRound =
            game.rounds.maxByOrNull { it.roundNumber }
                ?: return 0

        return currentRound.turns[userId]?.rollNumber
            ?: 0
    }

    override fun remainingTime(gameId: Int): Time {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId")
        val elapsed = System.currentTimeMillis() - game.startedAt
        val remaining = maxOf(0, 30000 - elapsed)
        return Time(remaining)
    }

    override fun hasActiveRound(gameId: Int): Boolean {
        val game = games[gameId] ?: return false
        return game.rounds.isNotEmpty()
    }

    override fun getRoundInfo(gameId: Int): RoundInfo {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId")

        val currentRound = game.rounds.maxByOrNull { it.roundNumber }

        if (currentRound == null) {
            val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })
            return RoundInfo(Round(0), pointsQueue)
        }

        val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

        currentRound.turns.forEach { (userId, turn) ->
            if (turn.score != null) {
                val user = userRepo.findById(userId)
                if (user != null) {
                    val playerInfo =
                        PlayerGameInfo(
                            playerId = userId,
                            rolls = turn.rollNumber.toQuantity(),
                            hand = turn.hand ?: Hand(emptyList()),
                            balance = user.balance,
                        )
                    pointsQueue.add(PointPlayer(playerInfo, Points(turn.score)))
                }
            }
        }

        return RoundInfo(Round(currentRound.roundNumber), pointsQueue)
    }

    override fun getScores(gameId: Int): Scoreboard {
        val game = games[gameId] ?: throw IllegalArgumentException("Game not found: $gameId")

        val pointsQueue = PriorityQueue<PointPlayer>(compareByDescending { it.points.points })

        game.players.forEach { playerId ->
            val user = userRepo.findById(playerId)
            if (user != null) {
                val totalScore =
                    game.rounds.sumOf { round ->
                        round.turns[playerId]?.score ?: 0
                    }

                val playerInfo =
                    PlayerGameInfo(
                        playerId = playerId,
                        rolls = 0.toQuantity(),
                        hand = Hand(emptyList()),
                        balance = user.balance,
                    )
                pointsQueue.add(PointPlayer(playerInfo, Points(totalScore)))
            }
        }

        return Scoreboard(pointsQueue)
    }

    override fun findById(id: Int): Game? {
        val gameData = games[id] ?: return null
        return Game(
            playersGameInfoList = emptyList(),
            rounds = emptyList(),
        )
    }

    override fun findAll(): List<Game> = games.values.map { findById(it.id)!! }

    override fun save(entity: Game) {
        TODO()
    }

    override fun deleteById(id: Int) {
        games.remove(id)
    }

    override fun clear() {
        games.clear()
        nextGameId = 1
    }

    fun setPlayerHand(
        userId: Int,
        gameId: Int,
        hand: Hand,
    ) {
        val game = games[gameId] ?: throw IllegalStateException("Game not found: $gameId")
        val currentRound =
            game.rounds.maxByOrNull { it.roundNumber }
                ?: throw IllegalStateException("No active round")

        val existingTurn = currentRound.turns[userId]
        currentRound.turns[userId] =
            TurnData(
                userId = userId,
                hand = hand,
                rollNumber = existingTurn?.rollNumber ?: 1,
                score = existingTurn?.score,
            )
    }
}
