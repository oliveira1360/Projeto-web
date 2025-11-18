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
    ): Either<GameError, Hand> =
        trxManager.run {
            validationService.validateUserId(userId).onFailure { return@run failure(it) }
            validationService.validateGameId(gameId).onFailure { return@run failure(it) }

            val validLockedDice = lockedDice.filter { it in 0..4 }

            validationService.run { checkGameExists(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateUserInGame(userId, gameId) }.onFailure { return@run failure(it) }
            validationService.run { validateRoundInProgress(gameId) }.onFailure { return@run failure(it) }
            validationService.run { validatePlayerTurn(userId, gameId) }.onFailure { return@run failure(it) }

            val currentRollCount = repositoryGame.getRollCount(userId, gameId)
            if (currentRollCount >= 3) {
                return@run failure(GameError.TooManyRolls)
            }

            val currentHand = repositoryGame.getPlayerHand(userId, gameId)?.value?.toMutableList()

            if (currentHand == null) {
                val newHand = Hand(List(5) { createRandomDice() })
                repositoryGame.shuffle(userId, newHand, gameId)
                return@run success(newHand)
            }

            if (currentHand.size != 5) {
                return@run failure(GameError.EmptyHand)
            }

            for (diceIdx in currentHand.indices) {
                if (validLockedDice.contains(diceIdx)) continue
                currentHand[diceIdx] = createRandomDice()
            }

            val newHand = Hand(currentHand)
            val hand = repositoryGame.shuffle(userId, newHand, gameId)

            notificationService.notifyGame(
                gameId,
                GameEvent(
                    type = GameEventType.PLAYER_ROLLED,
                    gameId = gameId,
                    message = "player $userId has rolled the dice",
                    data =
                        mapOf(
                            "new hand" to hand,
                        ),
                ),
            )

            success(newHand)
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
            validationService.run { validatePlayerTurn(userId, gameId) }.onFailure { return@run failure(it) }

            val hand =
                repositoryGame.getPlayerHand(userId, gameId)
                    ?: return@run failure(GameError.EmptyHand)

            if (hand.value.isEmpty() || hand.value.size != 5) {
                return@run failure(GameError.EmptyHand)
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

            // Check if all players have finished their turns
            val players = repositoryGame.listPlayersInGame(gameId)
            val scoreboard = repositoryGame.getScores(gameId)

            if (scoreboard.pointsQueue.size >= players.listPlayersInGame.size) {
                roundService.getRoundWinner(gameId)
            }

            success(points)
        }
}
