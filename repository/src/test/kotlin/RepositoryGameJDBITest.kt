package org.example.game

import org.example.TransactionManagerJdbi
import org.example.entity.core.Points
import org.example.entity.core.toName
import org.example.entity.dice.Dice
import org.example.entity.dice.DiceFace
import org.example.entity.player.Hand
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RepositoryGameJDBITest {
    private lateinit var jdbi: Jdbi
    private lateinit var transactionManager: TransactionManagerJdbi

    // Using existing test data users (from insert-test-data.sql)
    private val user1Id = 1 // john_doe
    private val user2Id = 2 // jane_smith
    private val user3Id = 3 // mike_wilson
    private var lobbyId: Int = 0
    private var gameId: Int = 0

    @BeforeEach
    fun setup() {
        val dbUrl = System.getenv("DB_TEST_URL") ?: "jdbc:postgresql://localhost:5432/db"
        val dbUser = System.getenv("DB_TEST_USER") ?: "dbuser"
        val dbPassword = System.getenv("DB_TEST_PASSWORD") ?: "2025-daw-leic52d-2025-leic52d-14"

        jdbi = Jdbi.create(dbUrl, dbUser, dbPassword)
        transactionManager = TransactionManagerJdbi(jdbi)

        transactionManager.run {
            repositoryGame.clear()
        }

        lobbyId =
            transactionManager.run {
                repositoryUser.updateBalance(user1Id, 1000)
                repositoryUser.updateBalance(user2Id, 1000)
                repositoryUser.updateBalance(user3Id, 1000)

                val lobby =
                    repositoryLobby.createLobby(
                        hostId = user1Id,
                        name = "Test Game Lobby".toName(),
                        maxPlayers = 3,
                        rounds = 6,
                    )

                repositoryLobby.addPlayer(lobby.id, user2Id)
                repositoryLobby.addPlayer(lobby.id, user3Id)

                lobby.id
            }
    }

    @AfterEach
    fun cleanup() {
        transactionManager.run {
            repositoryGame.clear()

            try {
                repositoryLobby.deleteById(lobbyId)
            } catch (e: Exception) {
            }
        }
    }

    @Test
    fun `insertMatch should create game and deduct balance from all players`() {
        transactionManager.run {
            val initialBalance1 =
                repositoryUser
                    .findById(user1Id)
                    ?.balance
                    ?.money
                    ?.value ?: 0
            val initialBalance2 =
                repositoryUser
                    .findById(user2Id)
                    ?.balance
                    ?.money
                    ?.value ?: 0
            val initialBalance3 =
                repositoryUser
                    .findById(user3Id)
                    ?.balance
                    ?.money
                    ?.value ?: 0

            gameId = repositoryGame.insertMatch(totalRounds = 6)

            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertMatchPlayer(gameId, user2Id, 2)
            repositoryGame.insertMatchPlayer(gameId, user3Id, 3)
            repositoryGame.deductBalance(gameId, 12)

            assertTrue(gameId > 0, "Game ID should be positive")

            val game = repositoryGame.findById(gameId)
            assertNotNull(game)

            val players = repositoryGame.listPlayersInGame(gameId)
            assertEquals(3, players.listPlayersInGame.size)

            val updatedUser1 = repositoryUser.findById(user1Id)
            val updatedUser2 = repositoryUser.findById(user2Id)
            val updatedUser3 = repositoryUser.findById(user3Id)

            assertEquals(initialBalance1 - 12, updatedUser1?.balance?.money?.value)
            assertEquals(initialBalance2 - 12, updatedUser2?.balance?.money?.value)
            assertEquals(initialBalance3 - 12, updatedUser3?.balance?.money?.value)
        }
    }

    @Test
    fun `insertRound should create first round`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)

            val roundNumber = repositoryGame.insertRound(gameId)

            assertEquals(1, roundNumber)
            assertTrue(repositoryGame.hasActiveRound(gameId))
        }
    }

    @Test
    fun `insertRound should create multiple rounds`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)

            val round1 = repositoryGame.insertRound(gameId)
            val round2 = repositoryGame.insertRound(gameId)
            val round3 = repositoryGame.insertRound(gameId)

            assertEquals(1, round1)
            assertEquals(2, round2)
            assertEquals(3, round3)
        }
    }

    @Test
    fun `updateHandAndRoll should create and update player hand`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val returnedHand = repositoryGame.updateHandAndRoll(user1Id, gameId, hand1, 1)

            assertNotNull(returnedHand)
            assertEquals(5, returnedHand.value.size)
            assertEquals(1, repositoryGame.getRollCount(user1Id, gameId))

            val hand2 = Hand(List(5) { Dice(DiceFace.KING) })
            repositoryGame.updateHandAndRoll(user1Id, gameId, hand2, 2)

            assertEquals(2, repositoryGame.getRollCount(user1Id, gameId))
        }
    }

    @Test
    fun `getPlayerHand should return player's current hand`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val expectedHand = Hand(List(5) { Dice(DiceFace.QUEEN) })
            repositoryGame.updateHandAndRoll(user1Id, gameId, expectedHand, 1)

            val retrievedHand = repositoryGame.getPlayerHand(user1Id, gameId)

            assertNotNull(retrievedHand)
            assertEquals(5, retrievedHand?.value?.size)
        }
    }

    @Test
    fun `getPlayerHand should return null when no hand exists`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val hand = repositoryGame.getPlayerHand(user1Id, gameId)

            assertNull(hand)
        }
    }

    @Test
    fun `updateScore should store player score`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val hand = Hand(List(5) { Dice(DiceFace.ACE) })
            repositoryGame.updateHandAndRoll(user1Id, gameId, hand, 1)

            val points = Points(30)
            repositoryGame.updateScore(user1Id, gameId, points)

            repositoryGame.markTurnAsFinished(user1Id, gameId)

            val scoreboard = repositoryGame.getScores(gameId)
            assertTrue(scoreboard.pointsQueue.isNotEmpty())
            val playerScore = scoreboard.pointsQueue.find { it.player.playerId == user1Id }
            assertNotNull(playerScore)
            assertEquals(30, playerScore?.points?.points)
        }
    }

    @Test
    fun `findRoundWinnerCandidate should return player with highest score and add 2 to balance`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertMatchPlayer(gameId, user2Id, 2)
            repositoryGame.insertMatchPlayer(gameId, user3Id, 3)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(5) { Dice(DiceFace.KING) })
            val hand3 = Hand(List(5) { Dice(DiceFace.NINE) })

            repositoryGame.updateHandAndRoll(user1Id, gameId, hand1, 1)
            repositoryGame.updateHandAndRoll(user2Id, gameId, hand2, 1)
            repositoryGame.updateHandAndRoll(user3Id, gameId, hand3, 1)

            repositoryGame.updateScore(user1Id, gameId, Points(30))
            repositoryGame.updateScore(user2Id, gameId, Points(25))
            repositoryGame.updateScore(user3Id, gameId, Points(20))

            repositoryGame.markTurnAsFinished(user1Id, gameId)
            repositoryGame.markTurnAsFinished(user2Id, gameId)
            repositoryGame.markTurnAsFinished(user3Id, gameId)

            val balanceBefore =
                repositoryUser
                    .findById(user1Id)
                    ?.balance
                    ?.money
                    ?.value

            val winner = repositoryGame.findRoundWinnerCandidate(gameId)

            assertNotNull(winner)
            assertEquals(user1Id, winner?.player?.playerId)
            assertEquals(30, winner?.points?.points)

            repositoryGame.setRoundWinner(gameId, 1, user1Id)
            repositoryGame.rewardPlayer(user1Id)

            val balanceAfter =
                repositoryUser
                    .findById(user1Id)
                    ?.balance
                    ?.money
                    ?.value
            assertEquals(balanceBefore!! + 2, balanceAfter)
        }
    }

    @Test
    fun `findRoundWinnerCandidate should handle tie by dice face weights`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertMatchPlayer(gameId, user2Id, 2)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val hand1 =
                Hand(
                    listOf(
                        Dice(DiceFace.ACE),
                        Dice(DiceFace.ACE),
                        Dice(DiceFace.ACE),
                        Dice(DiceFace.KING),
                        Dice(DiceFace.KING),
                    ),
                )
            val hand2 =
                Hand(
                    listOf(
                        Dice(DiceFace.NINE),
                        Dice(DiceFace.NINE),
                        Dice(DiceFace.NINE),
                        Dice(DiceFace.TEN),
                        Dice(DiceFace.TEN),
                    ),
                )

            repositoryGame.updateHandAndRoll(user1Id, gameId, hand1, 1)
            repositoryGame.updateHandAndRoll(user2Id, gameId, hand2, 1)

            repositoryGame.updateScore(user1Id, gameId, Points(25))
            repositoryGame.updateScore(user2Id, gameId, Points(25))

            repositoryGame.markTurnAsFinished(user1Id, gameId)
            repositoryGame.markTurnAsFinished(user2Id, gameId)

            val winner = repositoryGame.findRoundWinnerCandidate(gameId)

            assertNotNull(winner)
            assertEquals(user2Id, winner?.player?.playerId)
        }
    }

    @Test
    fun `getScores should return all players scores`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertMatchPlayer(gameId, user2Id, 2)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(5) { Dice(DiceFace.KING) })

            repositoryGame.updateHandAndRoll(user1Id, gameId, hand1, 1)
            repositoryGame.updateHandAndRoll(user2Id, gameId, hand2, 1)

            repositoryGame.updateScore(user1Id, gameId, Points(30))
            repositoryGame.updateScore(user2Id, gameId, Points(25))

            repositoryGame.markTurnAsFinished(user1Id, gameId)
            repositoryGame.markTurnAsFinished(user2Id, gameId)

            val scoreboard = repositoryGame.getScores(gameId)

            assertEquals(2, scoreboard.pointsQueue.size)
        }
    }

    @Test
    fun `listPlayersInGame should return all players`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertMatchPlayer(gameId, user2Id, 2)
            repositoryGame.insertMatchPlayer(gameId, user3Id, 3)

            val players = repositoryGame.listPlayersInGame(gameId)

            assertEquals(3, players.listPlayersInGame.size)
            assertTrue(players.listPlayersInGame.any { it.playerId == user1Id })
            assertTrue(players.listPlayersInGame.any { it.playerId == user2Id })
            assertTrue(players.listPlayersInGame.any { it.playerId == user3Id })
        }
    }

    @Test
    fun `closeGame should mark game as finished`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)

            repositoryGame.closeGame(gameId)

            val game = repositoryGame.findById(gameId)
            assertNotNull(game)
        }
    }

    @Test
    fun `findGameWinner should return player with most total points`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 3)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertMatchPlayer(gameId, user2Id, 2)

            for (round in 1..3) {
                repositoryGame.insertRound(gameId)
                repositoryGame.initTurn(gameId, round)

                val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
                val hand2 = Hand(List(5) { Dice(DiceFace.KING) })

                repositoryGame.updateHandAndRoll(user1Id, gameId, hand1, 1)
                repositoryGame.updateHandAndRoll(user2Id, gameId, hand2, 1)

                repositoryGame.updateScore(user1Id, gameId, Points(30))
                repositoryGame.updateScore(user2Id, gameId, Points(20))

                repositoryGame.markTurnAsFinished(user1Id, gameId)
                repositoryGame.markTurnAsFinished(user2Id, gameId)

                repositoryGame.setRoundWinner(gameId, round, user1Id)
            }

            repositoryGame.setGameWinnerAndFinish(gameId, user1Id)

            val gameWinner = repositoryGame.findGameWinner(gameId)

            assertNotNull(gameWinner)
            assertEquals(user1Id, gameWinner?.player?.playerId)
        }
    }

    @Test
    fun `hasActiveRound should return false initially`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)

            assertFalse(repositoryGame.hasActiveRound(gameId))
        }
    }

    @Test
    fun `hasActiveRound should return true after starting round`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)

            assertTrue(repositoryGame.hasActiveRound(gameId))
        }
    }

    @Test
    fun `getRoundInfo should return current round information`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)

            val roundInfo = repositoryGame.getRoundInfo(gameId)

            assertEquals(1, roundInfo.round.round)
        }
    }

    @Test
    fun `getRollCount should return correct number of rolls`() {
        transactionManager.run {
            gameId = repositoryGame.insertMatch(totalRounds = 6)
            repositoryGame.insertMatchPlayer(gameId, user1Id, 1)
            repositoryGame.insertRound(gameId)
            repositoryGame.initTurn(gameId, 1)

            assertEquals(0, repositoryGame.getRollCount(user1Id, gameId))

            val hand = Hand(List(5) { Dice(DiceFace.ACE) })
            repositoryGame.updateHandAndRoll(user1Id, gameId, hand, 1)
            assertEquals(1, repositoryGame.getRollCount(user1Id, gameId))

            repositoryGame.updateHandAndRoll(user1Id, gameId, hand, 2)
            assertEquals(2, repositoryGame.getRollCount(user1Id, gameId))
        }
    }
}
