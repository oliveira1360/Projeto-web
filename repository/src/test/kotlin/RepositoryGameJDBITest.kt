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
    fun `createGame should create game and deduct balance from all players`() {
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

            gameId = repositoryGame.createGame(user1Id, lobbyId)

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
    fun `startRound should create first round`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)

            val roundNumber = repositoryGame.startRound(gameId)

            assertEquals(1, roundNumber)
            assertTrue(repositoryGame.hasActiveRound(gameId))
        }
    }

    @Test
    fun `startRound should create multiple rounds`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)

            // Act - Start 3 rounds
            val round1 = repositoryGame.startRound(gameId)
            val round2 = repositoryGame.startRound(gameId)
            val round3 = repositoryGame.startRound(gameId)

            // Assert
            assertEquals(1, round1)
            assertEquals(2, round2)
            assertEquals(3, round3)
        }
    }

    @Test
    fun `shuffle should create and update player hand`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Act - First shuffle
            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val returnedHand = repositoryGame.shuffle(user1Id, hand1, gameId)

            // Assert
            assertNotNull(returnedHand)
            assertEquals(5, returnedHand.value.size)
            assertEquals(1, repositoryGame.getRollCount(user1Id, gameId))

            // Act - Second shuffle
            val hand2 = Hand(List(5) { Dice(DiceFace.KING) })
            repositoryGame.shuffle(user1Id, hand2, gameId)

            // Assert
            assertEquals(2, repositoryGame.getRollCount(user1Id, gameId))
        }
    }

    @Test
    fun `getPlayerHand should return player's current hand`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Setup
            val expectedHand = Hand(List(5) { Dice(DiceFace.QUEEN) })
            repositoryGame.shuffle(user1Id, expectedHand, gameId)

            // Act
            val retrievedHand = repositoryGame.getPlayerHand(user1Id, gameId)

            // Assert
            assertNotNull(retrievedHand)
            assertEquals(5, retrievedHand?.value?.size)
        }
    }

    @Test
    fun `getPlayerHand should return null when no hand exists`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Act
            val hand = repositoryGame.getPlayerHand(user1Id, gameId)

            // Assert
            assertNull(hand)
        }
    }

    @Test
    fun `calculatePoints should store player score`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Setup - Give player a hand
            val hand = Hand(List(5) { Dice(DiceFace.ACE) })
            repositoryGame.shuffle(user1Id, hand, gameId)

            // Act
            val points = Points(30)
            repositoryGame.updateScore(user1Id, gameId, points)

            // Assert - Verificar através do scoreboard
            val scoreboard = repositoryGame.getScores(gameId)
            assertTrue(scoreboard.pointsQueue.isNotEmpty())
            val playerScore = scoreboard.pointsQueue.find { it.player.playerId == user1Id }
            assertNotNull(playerScore)
            assertEquals(30, playerScore?.points?.points)
        }
    }

    @Test
    fun `getRoundWinner should return player with highest score and add 2 to balance`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Setup - Todos os jogadores fazem suas jogadas
            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(5) { Dice(DiceFace.KING) })
            val hand3 = Hand(List(5) { Dice(DiceFace.NINE) })

            repositoryGame.shuffle(user1Id, hand1, gameId)
            repositoryGame.shuffle(user2Id, hand2, gameId)
            repositoryGame.shuffle(user3Id, hand3, gameId)

            repositoryGame.updateScore(user1Id, gameId, Points(30))
            repositoryGame.updateScore(user2Id, gameId, Points(25))
            repositoryGame.updateScore(user3Id, gameId, Points(20))

            val balanceBefore =
                repositoryUser
                    .findById(user1Id)
                    ?.balance
                    ?.money
                    ?.value

            // Act
            val winner = repositoryGame.getRoundWinner(gameId)

            // Assert
            assertEquals(user1Id, winner.player.playerId)
            assertEquals(30, winner.points.points)

            // Verificar que o balance aumentou em 2
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
    fun `getRoundWinner should handle tie by dice face weights`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Setup - Ambos jogadores com mesmo score
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

            repositoryGame.shuffle(user1Id, hand1, gameId)
            repositoryGame.shuffle(user2Id, hand2, gameId)

            // Ambos têm Full House (mesmo score)
            repositoryGame.updateScore(user1Id, gameId, Points(25))
            repositoryGame.updateScore(user2Id, gameId, Points(25))

            // Act
            val winner = repositoryGame.getRoundWinner(gameId)

            // Assert - user2 deve ganhar (NINE tem peso maior que ACE)
            assertEquals(user2Id, winner.player.playerId)
        }
    }

    @Test
    fun `getScores should return all players scores`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Setup
            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(5) { Dice(DiceFace.KING) })

            repositoryGame.shuffle(user1Id, hand1, gameId)
            repositoryGame.shuffle(user2Id, hand2, gameId)

            repositoryGame.updateScore(user1Id, gameId, Points(30))
            repositoryGame.updateScore(user2Id, gameId, Points(25))

            // Act
            val scoreboard = repositoryGame.getScores(gameId)

            // Assert
            assertEquals(3, scoreboard.pointsQueue.size)
        }
    }

    @Test
    fun `listPlayersInGame should return all players`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)

            // Act
            val players = repositoryGame.listPlayersInGame(gameId)

            // Assert
            assertEquals(3, players.listPlayersInGame.size)
            assertTrue(players.listPlayersInGame.any { it.playerId == user1Id })
            assertTrue(players.listPlayersInGame.any { it.playerId == user2Id })
            assertTrue(players.listPlayersInGame.any { it.playerId == user3Id })
        }
    }

    @Test
    fun `closeGame should mark game as finished`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)

            // Act
            repositoryGame.closeGame(user1Id, gameId)

            // Assert
            val game = repositoryGame.findById(gameId)
            assertNotNull(game)
            // Verificar status via query direta se necessário
        }
    }

    @Test
    fun `getGameWinner should return player with most total points`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)

            // Simular várias rondas
            for (round in 1..3) {
                repositoryGame.startRound(gameId)

                val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
                val hand2 = Hand(List(5) { Dice(DiceFace.KING) })

                repositoryGame.shuffle(user1Id, hand1, gameId)
                repositoryGame.shuffle(user2Id, hand2, gameId)

                repositoryGame.updateScore(user1Id, gameId, Points(30))
                repositoryGame.updateScore(user2Id, gameId, Points(20))
            }

            // Act
            val gameWinner = repositoryGame.getGameWinner(gameId)

            // Assert
            assertEquals(user1Id, gameWinner.player.playerId)
            assertEquals(90, gameWinner.totalPoints.points) // 30 * 3
        }
    }

    @Test
    fun `hasActiveRound should return false initially`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)

            // Act & Assert
            assertFalse(repositoryGame.hasActiveRound(gameId))
        }
    }

    @Test
    fun `hasActiveRound should return true after starting round`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Act & Assert
            assertTrue(repositoryGame.hasActiveRound(gameId))
        }
    }

    @Test
    fun `getRoundInfo should return current round information`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Act
            val roundInfo = repositoryGame.getRoundInfo(gameId)

            // Assert
            assertEquals(1, roundInfo.round.round)
        }
    }

    @Test
    fun `getRollCount should return correct number of rolls`() {
        transactionManager.run {
            gameId = repositoryGame.createGame(user1Id, lobbyId)
            repositoryGame.startRound(gameId)

            // Initially 0
            assertEquals(0, repositoryGame.getRollCount(user1Id, gameId))

            // After first shuffle
            val hand = Hand(List(5) { Dice(DiceFace.ACE) })
            repositoryGame.shuffle(user1Id, hand, gameId)
            assertEquals(1, repositoryGame.getRollCount(user1Id, gameId))

            // After second shuffle
            repositoryGame.shuffle(user1Id, hand, gameId)
            assertEquals(2, repositoryGame.getRollCount(user1Id, gameId))
        }
    }
}
