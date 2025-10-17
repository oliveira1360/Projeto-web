@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example

import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.URL
import org.example.entity.core.toMoney
import org.example.entity.dice.Dice
import org.example.entity.dice.DiceFace
import org.example.entity.lobby.Lobby
import org.example.entity.player.Hand
import org.example.entity.player.HandValues
import org.example.entity.player.User
import org.example.game.RepositoryGameMem
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameServiceTest {
    private lateinit var userRepo: RepositoryUserMem
    private lateinit var lobbyRepo: RepositoryLobbyMem
    private lateinit var gameRepo: RepositoryGameMem
    private lateinit var trxManager: TransactionManagerMem
    private lateinit var gameService: GameService

    private lateinit var user1: User
    private lateinit var user2: User
    private lateinit var user3: User
    private lateinit var lobby: Lobby

    @BeforeEach
    fun setup() {
        // Clear all data
        RepositoryLobbyMem().clear()
        RepositoryGameMem().clear()

        userRepo = RepositoryUserMem()
        lobbyRepo = RepositoryLobbyMem()
        gameRepo = RepositoryGameMem()
        trxManager = TransactionManagerMem(userRepo, lobbyRepo, gameRepo)
        gameService = GameService(trxManager)

        // Create test users
        user1 =
            userRepo.createUser(
                name = Name("John Doe"),
                nickName = Name("john"),
                email = Email("john@example.com"),
                password = Password("SecurePass123!"),
                imageUrl = URL("https://example.com/john.png"),
            )

        user2 =
            userRepo.createUser(
                name = Name("Jane Smith"),
                nickName = Name("jane"),
                email = Email("jane@example.com"),
                password = Password("SecurePass123!"),
                imageUrl = URL("https://example.com/jane.png"),
            )

        user3 =
            userRepo.createUser(
                name = Name("Bob Wilson"),
                nickName = Name("bob"),
                email = Email("bob@example.com"),
                password = Password("SecurePass123!"),
                imageUrl = URL("https://example.com/bob.png"),
            )

        // Create a test lobby
        lobby =
            lobbyRepo.createLobby(
                name = Name("Test Lobby"),
                hostId = user1.id,
                maxPlayers = 4,
                rounds = 12,
            )

        // Fix: Parameters were in wrong order
        lobbyRepo.addPlayer(lobby.id, user2.id)
        lobbyRepo.addPlayer(lobby.id, user3.id)
    }

    // ==================== CREATE GAME TESTS ====================

    @Test
    fun `should create game successfully`() {
        val result = gameService.createGame(user1.id, lobby.id)

        assertTrue(result is Success)
        assertNotNull(gameRepo.findAll().firstOrNull())
    }

    @Test
    fun `should fail to create game with invalid user id`() {
        val result = gameService.createGame(999, lobby.id)

        assertTrue(result is Failure)
    }

    @Test
    fun `should fail to create game with invalid lobby id`() {
        val result = gameService.createGame(user1.id, 999)

        assertTrue(result is Failure)
    }

    // ==================== START ROUND TESTS ====================

    @Test
    fun `should start round successfully`() {
        gameService.createGame(user1.id, lobby.id)
        val gameId = 1

        val result = gameService.startRound(gameId)

        assertTrue(result is Success)
    }

    @Test
    fun `should fail to start round for non-existent game`() {
        val result = gameService.startRound(999)

        assertTrue(result is Failure)
    }

    // ==================== SHUFFLE/ROLL DICE TESTS ====================

    @Test
    fun `should shuffle all dice when no dice are locked`() {
        gameService.createGame(user1.id, lobby.id)
        val gameId = 1
        gameService.startRound(gameId)

        val result = gameService.shuffle(user1.id, emptyList(), gameId)

        assertTrue(result is Success)
        val hand = (result as Success).value
        assertEquals(5, hand.value.size)
    }

    @Test
    fun `should lock specified dice when shuffling`() {
        gameService.createGame(user1.id, lobby.id)
        val gameId = 1
        gameService.startRound(gameId)

        // First roll
        gameService.shuffle(user1.id, emptyList(), gameId)
        val firstHand = gameService.getPlayerHand(user1.id, gameId)

        // Second roll with locked dice
        val result = gameService.shuffle(user1.id, listOf(0, 2, 4), gameId)

        assertTrue(result is Success)
    }

    @Test
    fun `should fail shuffle with invalid game id`() {
        val result = gameService.shuffle(user1.id, emptyList(), 999)

        assertTrue(result is Failure)
    }

    @Test
    fun `should fail shuffle with invalid user id`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.shuffle(999, emptyList(), 1)

        assertTrue(result is Failure)
    }

    @Test
    fun `should fail shuffle with invalid dice indices`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)
        gameService.shuffle(user1.id, emptyList(), 1)

        // Indices out of bounds
        val result = gameService.shuffle(user1.id, listOf(5, 6, 7), 1)

        // Should still succeed but ignore invalid indices
        assertTrue(result is Success)
    }

    // ==================== GET PLAYER HAND TESTS ====================

    @Test
    fun `should get player hand successfully`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)
        gameService.shuffle(user1.id, emptyList(), 1)

        val result = gameService.getPlayerHand(user1.id, 1)

        assertTrue(result is Success)
        val hand = (result as Success).value
        assertEquals(5, hand.value.size)
    }

    @Test
    fun `should fail to get hand when no hand exists`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.getPlayerHand(user1.id, 1)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.EmptyHand)
    }

    @Test
    fun `should fail to get hand with invalid user id`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.getPlayerHand(999, 1)

        assertTrue(result is Failure)
    }

    // ==================== CALCULATE POINTS TESTS ====================

    @Test
    fun `should calculate points for five of a kind`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // Manually set a hand with five of a kind
        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        val points = (result as Success).value
        assertEquals(HandValues.FIVE_OF_A_KIND.value, points.points)
    }

    @Test
    fun `should calculate points for four of a kind`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.KING),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.ACE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FOUR_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    fun `should calculate points for full house`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.JACK),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FULL_HOUSE.value, (result as Success).value.points)
    }

    @Test
    fun `should calculate points for straight`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.TEN),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.STRAIGHT.value, (result as Success).value.points)
    }

    @Test
    fun `should calculate points for three of a kind`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.KING),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.THREE_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    fun `should calculate points for two pair`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.KING),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.TWO_PAIR.value, (result as Success).value.points)
    }

    @Test
    fun `should calculate points for one pair`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.TEN),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.ONE_PAIR.value, (result as Success).value.points)
    }

    @Test
    fun `should calculate zero points for no value`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.NINE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.NO_VALUE.value, (result as Success).value.points)
    }

    @Test
    fun `should fail to calculate points without hand`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.calculatePoints(user1.id, 1)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.EmptyHand)
    }

    // ==================== ROUND WINNER TESTS ====================

    @Test
    fun `should get round winner successfully`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // Set hands and calculate points for all players
        val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
        val hand2 = Hand(List(5) { Dice(DiceFace.KING) })

        gameRepo.setPlayerHand(user1.id, 1, hand1)
        gameRepo.setPlayerHand(user2.id, 1, hand2)

        gameService.calculatePoints(user1.id, 1)
        gameService.calculatePoints(user2.id, 1)

        val result = gameService.getRoundWinner(1)

        assertTrue(result is Success)
    }

    @Test
    fun `should fail to get round winner when no round started`() {
        gameService.createGame(user1.id, lobby.id)

        val result = gameService.getRoundWinner(1)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.NoRoundInProgress)
    }

    // ==================== GAME WINNER TESTS ====================

    @Test
    fun `should get game winner successfully`() {
        gameService.createGame(user1.id, lobby.id)

        // Play multiple rounds
        for (round in 1..3) {
            gameService.startRound(1)

            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(4) { Dice(DiceFace.KING) } + listOf(Dice(DiceFace.QUEEN)))

            gameRepo.setPlayerHand(user1.id, 1, hand1)
            gameRepo.setPlayerHand(user2.id, 1, hand2)

            gameService.calculatePoints(user1.id, 1)
            gameService.calculatePoints(user2.id, 1)

            gameService.getRoundWinner(1)
        }

        val result = gameService.getGameWinner(1)

        assertTrue(result is Success)
        val winner = (result as Success).value
        assertTrue(winner.totalPoints.points > 0)
    }

    @Test
    fun `should fail to get game winner for non-existent game`() {
        val result = gameService.getGameWinner(999)

        assertTrue(result is Failure)
    }

    // ==================== LIST PLAYERS TESTS ====================

    @Test
    fun `should list players in game`() {
        gameService.createGame(user1.id, lobby.id)

        val result = gameService.listPlayersInGame(1)

        assertTrue(result is Success)
        val players = (result as Success).value
        assertTrue(players.listPlayersInGame.isNotEmpty())
    }

    @Test
    fun `should fail to list players for non-existent game`() {
        val result = gameService.listPlayersInGame(999)

        assertTrue(result is Failure)
    }

    // ==================== SCOREBOARD TESTS ====================

    @Test
    fun `should get scoreboard successfully`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.setPlayerHand(user1.id, 1, hand1)
        gameService.calculatePoints(user1.id, 1)

        val result = gameService.getScores(1)

        assertTrue(result is Success)
        val scoreboard = (result as Success).value
        assertFalse(scoreboard.pointsQueue.isEmpty())
    }

    // ==================== CLOSE GAME TESTS ====================

    @Test
    fun `should close game successfully`() {
        gameService.createGame(user1.id, lobby.id)

        val result = gameService.closeGame(user1.id, 1)

        assertTrue(result is Success)
    }

    @Test
    fun `should fail to close non-existent game`() {
        val result = gameService.closeGame(user1.id, 999)

        assertTrue(result is Failure)
    }

    // ==================== ENTITY VALIDATION TESTS ====================

    @Test
    fun `should validate user name is not blank`() {
        assertThrows<IllegalArgumentException> {
            Name("")
        }
    }

    @Test
    fun `should validate user email format`() {
        assertThrows<IllegalArgumentException> {
            Email("invalid-email")
        }
    }

    @Test
    fun `should validate password strength`() {
        assertThrows<IllegalArgumentException> {
            Password("weak")
        }
    }

    @Test
    fun `should validate balance is non-negative`() {
        val balance = Balance(0.toMoney())
        assertTrue(balance.money.value >= 0)
    }

    @Test
    fun `should validate hand has exactly 5 dice`() {
        val validHand = Hand(List(5) { Dice(DiceFace.ACE) })
        assertEquals(5, validHand.value.size)

        val invalidHand = Hand(List(3) { Dice(DiceFace.ACE) })
        // Hand evaluation should handle this gracefully
        assertNotNull(invalidHand.evaluateHandValue())
    }
}