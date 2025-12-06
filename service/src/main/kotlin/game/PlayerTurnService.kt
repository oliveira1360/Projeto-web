package org.example.game

import jakarta.inject.Named
import org.example.Either
import org.example.GameEvent
import org.example.GameEventType
import org.example.GameNotificationService
import org.example.TransactionManager
import org.example.entity.core.Points
import org.example.entity.dice.createRandomDice
import org.example.entity.player.Hand
import org.example.failure
import org.example.onFailure
import org.example.success

const val MAX_ROLL_COUNT = 3

data class HandWithValues(
    val hand: Hand,
    val rollNumber: Number,
)

@Named
class PlayerTurnService(
    private val trxManager: TransactionManager,
    private val validationService: GameValidationService,
    private val notificationService: GameNotificationService,
    private val roundService: RoundService,
) {
    fun getPlayerHand(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Hand> =
        trxManager.run {
            validationService.validateUserId(userId).onFailure { return@run failure(it) }
            validationService.validateGameId(gameId).onFailure { return@run failure(it) }
            validationService.run { checkGameExists(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateUserInGame(userId, gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateRoundInProgress(gameId) }.onFailure { return@run failure(it) }

            val hand =
                repositoryGame.getPlayerHand(userId, gameId)
                    ?: return@run failure(GameError.EmptyHand)

            success(hand)
        }

    fun shuffle(
        userId: Int,
        lockedDice: List<Int>,
        gameId: Int,
    ): Either<GameError, HandWithValues> =
        trxManager.run {
            validationService.validateUserId(userId).onFailure { return@run failure(it) }
            validationService.validateGameId(gameId).onFailure { return@run failure(it) }

            val validLockedDice = lockedDice.filter { it in 0..4 }

            validationService.run { checkGameExists(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateUserInGame(userId, gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateRoundInProgress(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validatePlayerTurn(userId, gameId) }.onFailure { return@run failure(it) }

            val currentRollCount = repositoryGame.getRollCount(userId, gameId)
            if (currentRollCount >= MAX_ROLL_COUNT) {
                return@run failure(GameError.TooManyRolls)
            }

            val currentHand = repositoryGame.getPlayerHand(userId, gameId)?.value?.toMutableList()
            val newRollCount = currentRollCount + 1

            if (currentHand == null) {
                val newHand = Hand(List(5) { createRandomDice() })
                repositoryGame.updateHandAndRoll(userId, gameId, newHand, newRollCount)
                return@run success(HandWithValues(newHand, newRollCount))
            }

            if (currentHand.size != 5) {
                return@run failure(GameError.EmptyHand)
            }

            for (diceIdx in currentHand.indices) {
                if (validLockedDice.contains(diceIdx)) continue
                currentHand[diceIdx] = createRandomDice()
            }

            val newHand = Hand(currentHand)
            repositoryGame.updateHandAndRoll(userId, gameId, newHand, newRollCount)

            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.PLAYER_ROLLED,
                    gameId = gameId,
                    message = "player $userId has rolled the dice",
                    data =
                        mapOf(
                            "new hand" to newHand.value,
                        ),
                ),
            )

            success(HandWithValues(newHand, newRollCount))
        }

    fun finishTurn(
        userId: Int,
        gameId: Int,
    ): Either<GameError, Points> =
        trxManager.run {
            validationService.validateUserId(userId).onFailure { return@run failure(it) }
            validationService.validateGameId(gameId).onFailure { return@run failure(it) }
            validationService.run { checkGameExists(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateUserInGame(userId, gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateRoundInProgress(gameId) }.onFailure { return@run failure(it) }
            // validationService.run { validatePlayerTurn(userId, gameId) }.onFailure { return@run failure(it) }

            val hand = repositoryGame.getPlayerHand(userId, gameId) ?: return@run failure(GameError.EmptyHand)

            if (hand.value.isEmpty() || hand.value.size != 5) {
                return@run failure(GameError.EmptyHand)
            }
            if (repositoryGame.getCurrentPlayerTurn(gameId) != userId) {
                return@run failure(GameError.NotPlayerTurn)
            }

            if (repositoryGame.getCurrentPlayerTurn(gameId) == -1) {
                return@run failure(GameError.UnauthorizedAction)
            }

            val score = hand.calculateScore()
            val points = Points(score)

            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.PLAYER_FINISHED_TURN,
                    gameId = gameId,
                    message = "Player $userId finished turn with $score",
                    data =
                        mapOf(
                            "playerId" to userId,
                            "points" to points.points,
                            "handValue" to score,
                        ),
                ),
            )

            repositoryGame.updateScore(userId, gameId, points)
            repositoryGame.markTurnAsFinished(userId, gameId)

            // Check if all players have finished their turns
            val players = repositoryGame.listPlayersInGame(gameId)
            val scoreboard = repositoryGame.getScores(gameId)

            if (scoreboard.pointsQueue.size >= players.listPlayersInGame.size) {
                roundService.getRoundWinner(gameId)
            }

            success(points)
        }
}
