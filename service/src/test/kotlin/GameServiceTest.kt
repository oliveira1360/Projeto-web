import org.example.Failure
import org.example.GameError
import org.example.game.GameService
import org.example.Success
import org.example.TransactionManagerMem
import org.example.config.GameDomainConfig
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
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameServiceTest {
    private var userRepo: RepositoryUserMem = RepositoryUserMem()
    private var lobbyRepo: RepositoryLobbyMem = RepositoryLobbyMem()
    private var gameRepo: RepositoryGameMem = RepositoryGameMem()
    private var generalRepo: RepositoryInviteMem = RepositoryInviteMem()
    private var trxManager: TransactionManagerMem = TransactionManagerMem(userRepo, lobbyRepo, gameRepo, generalRepo)
    private var gameDomainConfig = GameDomainConfig(moneyRemove = 1)
    private var gameService: GameService = GameService(trxManager, gameDomainConfig)

    private var user1: User
    private var user2: User
    private var user3: User
    private var lobby: Lobby

    init {
        val email1 = Email("john@example.com")
        user1 = userRepo.findByEmail(email1)
            ?: userRepo.createUser(
                name = Name("John Doe"),
                nickName = Name("john"),
                email = email1,
                password = Password("SecurePass123!"),
                imageUrl = URL("https://example.com/john.png"),
            )

        val email2 = Email("jane@example.com")
        user2 = userRepo.findByEmail(email2)
            ?: userRepo.createUser(
                name = Name("Jane Smith"),
                nickName = Name("jane"),
                email = email2,
                password = Password("SecurePass123!"),
                imageUrl = URL("https://example.com/jane.png"),
            )

        val email3 = Email("bob@example.com")
        user3 = userRepo.findByEmail(email3)
            ?: userRepo.createUser(
                name = Name("Bob Wilson"),
                nickName = Name("bob"),
                email = email3,
                password = Password("SecurePass123!"),
                imageUrl = URL("https://example.com/bob.png"),
            )

        lobby = lobbyRepo.findAll().find { it.hostId == user1.id }
            ?: lobbyRepo.createLobby(
                name = Name("Test Lobby"),
                hostId = user1.id,
                maxPlayers = 4,
                rounds = 12,
            )

        if (!lobbyRepo.isUserInLobby(user2.id, lobby.id)) {
            lobbyRepo.addPlayer(lobby.id, user2.id)
        }
        if (!lobbyRepo.isUserInLobby(user3.id, lobby.id)) {
            lobbyRepo.addPlayer(lobby.id, user3.id)
        }
    }

    // ==================== CREATE GAME TESTS ====================

    @Test
    @Order(1)
    fun `should create game successfully`() {
        val result = gameService.createGame(user1.id, lobby.id)

        assertTrue(result is Success)
        assertNotNull(gameRepo.findAll().firstOrNull())
    }

    @Test
    @Order(2)
    fun `should fail to create game with invalid user id`() {
        val result = gameService.createGame(999, lobby.id)

        assertTrue(result is Failure)
    }

    @Test
    @Order(3)
    fun `should fail to create game with invalid lobby id`() {
        val result = gameService.createGame(user1.id, 999)

        assertTrue(result is Failure)
    }

    // ==================== START ROUND TESTS ====================

    @Test
    @Order(4)
    fun `should start round successfully`() {
        gameService.createGame(user1.id, lobby.id)
        val gameId = 1

        val result = gameService.startRound(gameId)

        assertTrue(result is Success)
    }

    @Test
    @Order(5)
    fun `should fail to start round for non-existent game`() {
        val result = gameService.startRound(999)

        assertTrue(result is Failure)
    }

    // ==================== SHUFFLE/ROLL DICE TESTS ====================

    @Test
    @Order(6)
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
    @Order(7)
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
    @Order(8)
    fun `should fail shuffle with invalid game id`() {
        val result = gameService.shuffle(user1.id, emptyList(), 999)

        assertTrue(result is Failure)
    }

    @Test
    @Order(9)
    fun `should fail shuffle with invalid user id`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.shuffle(999, emptyList(), 1)

        assertTrue(result is Failure)
    }

    @Test
    @Order(10)
    fun `should fail shuffle with invalid dice indices`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)
        gameService.shuffle(user1.id, emptyList(), 1)

        // Indices out of bounds
        val result = gameService.shuffle(user1.id, listOf(5, 6, 7), 1)

        assertTrue(result is Success)
    }

    // ==================== GET PLAYER HAND TESTS ====================

    @Test
    @Order(11)
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
    @Order(12)
    fun `should fail to get hand when no hand exists`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.getPlayerHand(user1.id, 1)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.EmptyHand)
    }

    @Test
    @Order(13)
    fun `should fail to get hand with invalid user id`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.getPlayerHand(999, 1)

        assertTrue(result is Failure)
    }

    // ==================== CALCULATE POINTS TESTS ====================

    @Test
    @Order(14)
    fun `should calculate points for five of a kind`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // Manually set a hand with five of a kind
        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        val points = (result as Success).value
        assertEquals(HandValues.FIVE_OF_A_KIND.value, points.points)
    }

    @Test
    @Order(15)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FOUR_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(16)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FULL_HOUSE.value, (result as Success).value.points)
    }

    @Test
    @Order(17)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.STRAIGHT.value, (result as Success).value.points)
    }

    @Test
    @Order(18)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.THREE_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(19)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.TWO_PAIR.value, (result as Success).value.points)
    }

    @Test
    @Order(20)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.ONE_PAIR.value, (result as Success).value.points)
    }

    @Test
    @Order(21)
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.NO_VALUE.value, (result as Success).value.points)
    }

    @Test
    @Order(22)
    fun `should fail to calculate points without hand`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.EmptyHand)
    }

    // ==================== ROUND WINNER TESTS ====================

    @Test
    @Order(23)
    fun `should get round winner successfully`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // Set hands and calculate points for all players
        val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
        val hand2 = Hand(List(5) { Dice(DiceFace.KING) })

        gameRepo.setPlayerHand(user1.id, 1, hand1)
        gameRepo.setPlayerHand(user2.id, 1, hand2)

        gameService.finishTurn(user1.id, 1)
        gameService.finishTurn(user2.id, 1)

        val result = gameService.getRoundWinner(1)

        assertTrue(result is Success)
    }

    @Test
    @Order(24)
    fun `should fail to get round winner when no round started`() {
        gameService.createGame(user1.id, lobby.id)

        val result = gameService.getRoundWinner(1)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.RoundNotStarted)
    }

    // ==================== GAME WINNER TESTS ====================

    @Test
    @Order(25)
    fun `should get game winner successfully`() {
        gameService.createGame(user1.id, lobby.id)

        // Play multiple rounds
        for (round in 1..3) {
            gameService.startRound(1)

            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(4) { Dice(DiceFace.KING) } + listOf(Dice(DiceFace.QUEEN)))

            gameRepo.setPlayerHand(user1.id, 1, hand1)
            gameRepo.setPlayerHand(user2.id, 1, hand2)

            gameService.finishTurn(user1.id, 1)
            gameService.finishTurn(user2.id, 1)

            gameService.getRoundWinner(1)
        }

        val result = gameService.getGameWinner(1)

        assertTrue(result is Success)
        val winner = (result as Success).value
        assertTrue(winner.totalPoints.points > 0)
    }

    @Test
    @Order(26)
    fun `should fail to get game winner for non-existent game`() {
        val result = gameService.getGameWinner(999)

        assertTrue(result is Failure)
    }

    // ==================== LIST PLAYERS TESTS ====================

    @Test
    @Order(27)
    fun `should list players in game`() {
        gameService.createGame(user1.id, lobby.id)

        val result = gameService.listPlayersInGame(1)

        assertTrue(result is Success)
        val players = (result as Success).value
        assertTrue(players.listPlayersInGame.isNotEmpty())
    }

    @Test
    @Order(28)
    fun `should fail to list players for non-existent game`() {
        val result = gameService.listPlayersInGame(999)

        assertTrue(result is Failure)
    }

    // ==================== SCOREBOARD TESTS ====================

    @Test
    @Order(29)
    fun `should get scoreboard successfully`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.setPlayerHand(user1.id, 1, hand1)
        gameService.finishTurn(user1.id, 1)

        val result = gameService.getScores(1)

        assertTrue(result is Success)
        val scoreboard = (result as Success).value
        assertFalse(scoreboard.pointsQueue.isEmpty())
    }

    // ==================== CLOSE GAME TESTS ====================

    @Test
    @Order(30)
    fun `should close game successfully`() {
        gameService.createGame(user1.id, lobby.id)

        val result = gameService.closeGame(user1.id, 1)

        assertTrue(result is Success)
    }

    @Test
    @Order(31)
    fun `should fail to close non-existent game`() {
        val result = gameService.closeGame(user1.id, 999)

        assertTrue(result is Failure)
    }

    // ==================== ENTITY VALIDATION TESTS ====================

    @Test
    @Order(32)
    fun `should validate user name is not blank`() {
        assertThrows<IllegalArgumentException> {
            Name("")
        }
    }

    @Test
    @Order(33)
    fun `should validate user email format`() {
        assertThrows<IllegalArgumentException> {
            Email("invalid-email")
        }
    }

    @Test
    @Order(34)
    fun `should validate password strength`() {
        assertThrows<IllegalArgumentException> {
            Password("weak")
        }
    }

    @Test
    @Order(35)
    fun `should validate balance is non-negative`() {
        val balance = Balance(0.toMoney())
        assertTrue(balance.money.value >= 0)
    }

    @Test
    @Order(36)
    fun `should validate hand has exactly 5 dice`() {
        val validHand = Hand(List(5) { Dice(DiceFace.ACE) })
        assertEquals(5, validHand.value.size)

        val invalidHand = Hand(List(3) { Dice(DiceFace.ACE) })
        assertNotNull(invalidHand.evaluateHandValue())
    }

    // ==================== COMPREHENSIVE HAND CALCULATION TESTS ====================

    @Test
    @Order(37)
    fun `should calculate points for straight - ACE to TEN`() {
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.STRAIGHT.value, (result as Success).value.points)
    }

    @Test
    @Order(38)
    fun `should calculate points for straight - KING to NINE`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.KING),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.NINE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.STRAIGHT.value, (result as Success).value.points)
    }

    @Test
    @Order(39)
    fun `should calculate points for straight - unordered dice`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // Straight but dice are shuffled in hand
        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.KING),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.STRAIGHT.value, (result as Success).value.points)
    }

    @Test
    @Order(40)
    fun `should NOT calculate straight for non-consecutive dice`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // Has ACE, KING, QUEEN, JACK, NINE
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

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        // Should be NO_VALUE, not STRAIGHT
        assertEquals(HandValues.NO_VALUE.value, (result as Success).value.points)
    }

    @Test
    @Order(41)
    fun `should prioritize five of a kind over everything`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FIVE_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(42)
    fun `should calculate four of a kind correctly`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.ACE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FOUR_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(43)
    fun `should prioritize full house over three of a kind`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // 3 KINGS + 2 ACES = Full House
        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.KING),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.ACE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.FULL_HOUSE.value, (result as Success).value.points)
    }

    @Test
    @Order(44)
    fun `should calculate three of a kind when not full house`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.KING),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.THREE_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(45)
    fun `should calculate two pair correctly`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.ACE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.TWO_PAIR.value, (result as Success).value.points)
    }

    @Test
    @Order(46)
    fun `should calculate one pair correctly`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.NINE),
                    Dice(DiceFace.QUEEN),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.ACE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.ONE_PAIR.value, (result as Success).value.points)
    }

    @Test
    @Order(47)
    fun `should calculate no value for random dice`() {
        gameService.createGame(user1.id, lobby.id)
        gameService.startRound(1)

        // All different, not consecutive
        val hand =
            Hand(
                listOf(
                    Dice(DiceFace.ACE),
                    Dice(DiceFace.KING),
                    Dice(DiceFace.JACK),
                    Dice(DiceFace.TEN),
                    Dice(DiceFace.NINE),
                ),
            )
        gameRepo.setPlayerHand(user1.id, 1, hand)

        val result = gameService.finishTurn(user1.id, 1)

        assertTrue(result is Success)
        assertEquals(HandValues.NO_VALUE.value, (result as Success).value.points)
    }

    @Test
    @Order(48)
    fun `should verify hand value ordering - five of a kind beats four of a kind`() {
        assertTrue(HandValues.FIVE_OF_A_KIND.value > HandValues.FOUR_OF_A_KIND.value)
    }

    @Test
    @Order(49)
    fun `should verify hand value ordering - four of a kind beats full house`() {
        assertTrue(HandValues.FOUR_OF_A_KIND.value > HandValues.FULL_HOUSE.value)
    }

    @Test
    @Order(50)
    fun `should verify hand value ordering - full house beats straight`() {
        assertTrue(HandValues.FULL_HOUSE.value > HandValues.STRAIGHT.value)
    }

    @Test
    @Order(51)
    fun `should verify hand value ordering - straight beats three of a kind`() {
        assertTrue(HandValues.STRAIGHT.value > HandValues.THREE_OF_A_KIND.value)
    }

    @Test
    @Order(52)
    fun `should verify hand value ordering - three of a kind beats two pair`() {
        assertTrue(HandValues.THREE_OF_A_KIND.value > HandValues.TWO_PAIR.value)
    }

    @Test
    @Order(53)
    fun `should verify hand value ordering - two pair beats one pair`() {
        assertTrue(HandValues.TWO_PAIR.value > HandValues.ONE_PAIR.value)
    }

    @Test
    @Order(54)
    fun `should verify hand value ordering - one pair beats no value`() {
        assertTrue(HandValues.ONE_PAIR.value > HandValues.NO_VALUE.value)
    }

    @Test
    @Order(55)
    fun `should handle edge case - empty hand evaluates to no value`() {
        val emptyHand = Hand(emptyList())
        assertEquals(HandValues.NO_VALUE, emptyHand.evaluateHandValue())
    }

    @Test
    @Order(56)
    fun `should handle edge case - less than 5 dice still evaluates`() {
        // 3 of a kind with only 3 dice
        val smallHand = Hand(List(3) { Dice(DiceFace.KING) })
        assertEquals(HandValues.THREE_OF_A_KIND, smallHand.evaluateHandValue())
    }
}
