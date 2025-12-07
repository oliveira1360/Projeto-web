@file:Suppress("ktlint:standard:no-wildcard-imports")

import org.example.*
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
import org.example.game.*
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class MockGameNotificationService : GameNotificationService() {
    override fun subscribe(
        userId: Int,
        gameId: Int,
        emitter: SseEmitter,
    ) {}

    override fun notifyGame(
        gameId: Int,
        event: GameEvent,
    ) {}

    override fun closeGameConnections(gameId: Int) {}
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameServiceTest {
    private lateinit var userRepo: RepositoryUserMem
    private lateinit var lobbyRepo: RepositoryLobbyMem
    private lateinit var gameRepo: RepositoryGameMem
    private lateinit var generalRepo: RepositoryInviteMem
    private lateinit var trxManager: TransactionManagerMem
    private lateinit var gameDomainConfig: GameDomainConfig
    private lateinit var notificationService: GameNotificationService
    private lateinit var validationService: GameValidationService
    private lateinit var roundService: RoundService
    private lateinit var playerTurnService: PlayerTurnService
    private lateinit var gameService: GameService

    private lateinit var user1: User
    private lateinit var user2: User
    private lateinit var user3: User
    private lateinit var lobby: Lobby

    @BeforeAll
    fun setupAll() {
        userRepo = RepositoryUserMem()
        lobbyRepo = RepositoryLobbyMem()
        gameRepo = RepositoryGameMem()
        generalRepo = RepositoryInviteMem()
    }

    @BeforeEach
    fun setup() {
        gameRepo.clear()

        trxManager = TransactionManagerMem(userRepo, lobbyRepo, gameRepo, generalRepo)
        gameDomainConfig = GameDomainConfig(moneyRemove = 1)

        notificationService = MockGameNotificationService()
        validationService = GameValidationService()
        roundService = RoundService(trxManager, validationService, notificationService)
        playerTurnService = PlayerTurnService(trxManager, validationService, notificationService, roundService)
        gameService =
            GameService(
                trxManager,
                gameDomainConfig,
                notificationService,
                validationService,
                roundService,
                playerTurnService,
            )

        val email1 = Email("john@example.com")
        user1 = userRepo.findByEmail(email1) ?: userRepo.createUser(
            name = Name("John Doe"),
            nickName = Name("john"),
            email = email1,
            passwordHash = "SecurePass123!",
            imageUrl = URL("https://example.com/john.png"),
        )

        val email2 = Email("jane@example.com")
        user2 = userRepo.findByEmail(email2) ?: userRepo.createUser(
            name = Name("Jane Smith"),
            nickName = Name("jane"),
            email = email2,
            passwordHash = "SecurePass123!",
            imageUrl = URL("https://example.com/jane.png"),
        )

        val email3 = Email("bob@example.com")
        user3 = userRepo.findByEmail(email3) ?: userRepo.createUser(
            name = Name("Bob Wilson"),
            nickName = Name("bob"),
            email = email3,
            passwordHash = "SecurePass123!",
            imageUrl = URL("https://example.com/bob.png"),
        )

        lobby =
            lobbyRepo.createLobby(
                name = Name("Test Lobby ${System.currentTimeMillis()}"),
                hostId = user1.id,
                maxPlayers = 4,
                rounds = 12,
            )

        lobbyRepo.addPlayer(lobby.id, user2.id)
        lobbyRepo.addPlayer(lobby.id, user3.id)
    }

    // ==================== CREATE GAME TESTS ====================

    @Test
    @Order(1)
    fun `should create game successfully`() {
        val result = gameService.createGame(user1.id, lobby.id)

        assertTrue(result is Success)
        val createdGame = (result as Success).value
        assertNotNull(gameRepo.findById(createdGame.gameId))
        assertEquals("ACTIVE", createdGame.status)
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

    @Test
    @Order(4)
    fun `should fail to create game when user not in lobby`() {
        val newUser =
            userRepo.createUser(
                name = Name("New User"),
                nickName = Name("newuser"),
                email = Email("new${System.currentTimeMillis()}@example.com"),
                passwordHash = "SecurePass123!",
                imageUrl = URL("https://example.com/new.png"),
            )

        val result = gameService.createGame(newUser.id, lobby.id)
        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.UserNotInGame)
    }

    // ==================== START ROUND TESTS ====================

    @Test
    @Order(5)
    fun `should start round successfully`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        val result = gameService.startRound(createdGame.gameId)

        assertTrue(result is Success)
        val roundNumber = (result as Success).value
        assertTrue(roundNumber >= 1)
    }

    @Test
    @Order(6)
    fun `should fail to start round for non-existent game`() {
        val result = gameService.startRound(999)
        assertTrue(result is Failure)
    }

    // ==================== SHUFFLE/ROLL DICE TESTS ====================

    @Test
    @Order(7)
    fun `should shuffle all dice when no dice are locked`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val result = gameService.shuffle(user1.id, emptyList(), createdGame.gameId)

        assertTrue(result is Success)
        val hand = (result as Success).value
        assertEquals(5, hand.hand.value.size)
    }

    @Test
    @Order(8)
    fun `should lock specified dice when shuffling`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)
        gameService.shuffle(user1.id, emptyList(), createdGame.gameId)

        val result = gameService.shuffle(user1.id, listOf(0, 2, 4), createdGame.gameId)
        assertTrue(result is Success)
    }

    @Test
    @Order(9)
    fun `should fail shuffle with invalid game id`() {
        val result = gameService.shuffle(user1.id, emptyList(), 999)
        assertTrue(result is Failure)
    }

    @Test
    @Order(10)
    fun `should fail shuffle with invalid user id`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val result = gameService.shuffle(999, emptyList(), createdGame.gameId)
        assertTrue(result is Failure)
    }

    // ==================== GET PLAYER HAND TESTS ====================

    @Test
    @Order(11)
    fun `should get player hand successfully`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)
        gameService.shuffle(user1.id, emptyList(), createdGame.gameId)

        val result = gameService.getPlayerHand(user1.id, createdGame.gameId)

        assertTrue(result is Success)
        val hand = (result as Success).value
        assertEquals(5, hand.value.size)
    }

    @Test
    @Order(12)
    fun `should fail to get hand when no hand exists`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val result = gameService.getPlayerHand(user1.id, createdGame.gameId)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.EmptyHand)
    }

    @Test
    @Order(13)
    fun `should fail to get hand with invalid user id`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val result = gameService.getPlayerHand(999, createdGame.gameId)
        assertTrue(result is Failure)
    }

    // ==================== FINISH TURN AND CALCULATE POINTS TESTS ====================

    @Test
    @Order(14)
    fun `should calculate points for five of a kind`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)

        assertTrue(result is Success)
        assertEquals(HandValues.FIVE_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(15)
    fun `should calculate points for four of a kind`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.FOUR_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(16)
    fun `should calculate points for full house`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.FULL_HOUSE.value, (result as Success).value.points)
    }

    @Test
    @Order(17)
    fun `should calculate points for straight`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.STRAIGHT.value, (result as Success).value.points)
    }

    @Test
    @Order(18)
    fun `should calculate points for three of a kind`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.THREE_OF_A_KIND.value, (result as Success).value.points)
    }

    @Test
    @Order(19)
    fun `should calculate points for two pair`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.TWO_PAIR.value, (result as Success).value.points)
    }

    @Test
    @Order(20)
    fun `should calculate points for one pair`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.ONE_PAIR.value, (result as Success).value.points)
    }

    @Test
    @Order(21)
    fun `should calculate zero points for no value`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

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
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand, 1)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)
        assertTrue(result is Success)
        assertEquals(HandValues.NO_VALUE.value, (result as Success).value.points)
    }

    @Test
    @Order(22)
    fun `should fail to calculate points without hand`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val result = gameService.finishTurn(user1.id, createdGame.gameId)

        assertTrue(result is Failure)
        assertTrue((result as Failure).value is GameError.EmptyHand)
    }

    // ==================== ROUND WINNER TESTS ====================

    @Test
    @Order(24)
    fun `should fail to get round winner when no round started`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value

        val result = gameService.getRoundWinner(createdGame.gameId)
        assertTrue(result is Failure)
    }

    // ==================== GAME WINNER TESTS ====================

    @Test
    @Order(25)
    fun `should get game winner successfully`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value

        for (round in 1..3) {
            gameService.startRound(createdGame.gameId)

            val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
            val hand2 = Hand(List(4) { Dice(DiceFace.KING) } + listOf(Dice(DiceFace.QUEEN)))

            gameRepo.populateEmptyTurns(createdGame.gameId, round, user1.id)
            gameRepo.populateEmptyTurns(createdGame.gameId, round, user2.id)
            gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand1, 1)
            gameRepo.updateHandAndRoll(user2.id, createdGame.gameId, hand2, 1)

            gameService.finishTurn(user1.id, createdGame.gameId)
            gameService.finishTurn(user2.id, createdGame.gameId)

            gameService.getRoundWinner(createdGame.gameId)
        }

        val result = gameService.getGameWinner(createdGame.gameId)
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
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value

        val result = gameService.listPlayersInGame(createdGame.gameId)

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
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value
        gameService.startRound(createdGame.gameId)

        val hand1 = Hand(List(5) { Dice(DiceFace.ACE) })
        gameRepo.populateEmptyTurns(createdGame.gameId, 1, user1.id)
        gameRepo.updateHandAndRoll(user1.id, createdGame.gameId, hand1, 1)
        gameService.finishTurn(user1.id, createdGame.gameId)

        val result = gameService.getScores(createdGame.gameId)

        assertTrue(result is Success)
        val scoreboard = (result as Success).value
        assertFalse(scoreboard.pointsQueue.isEmpty())
    }

    // ==================== CLOSE GAME TESTS ====================

    @Test
    @Order(30)
    fun `should close game successfully`() {
        val createdGame = (gameService.createGame(user1.id, lobby.id) as Success).value

        val result = gameService.closeGame(user1.id, createdGame.gameId)
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
        assertThrows<IllegalArgumentException> { Name("") }
    }

    @Test
    @Order(33)
    fun `should validate user email format`() {
        assertThrows<IllegalArgumentException> { Email("invalid-email") }
    }

    @Test
    @Order(34)
    fun `should validate password strength`() {
        assertThrows<IllegalArgumentException> { Password("weak") }
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

    // ==================== HAND CALCULATION TESTS ====================

    @Test
    @Order(37)
    fun `should calculate straight ACE to TEN`() {
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
        assertEquals(HandValues.STRAIGHT, hand.evaluateHandValue())
    }

    @Test
    @Order(38)
    fun `should calculate straight KING to NINE`() {
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
        assertEquals(HandValues.STRAIGHT, hand.evaluateHandValue())
    }

    @Test
    @Order(39)
    fun `should calculate straight with unordered dice`() {
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
        assertEquals(HandValues.STRAIGHT, hand.evaluateHandValue())
    }

    @Test
    @Order(40)
    fun `should NOT calculate straight for non-consecutive dice`() {
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
        assertEquals(HandValues.NO_VALUE, hand.evaluateHandValue())
    }

    @Test
    @Order(41)
    fun `should prioritize five of a kind`() {
        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        assertEquals(HandValues.FIVE_OF_A_KIND, hand.evaluateHandValue())
    }

    @Test
    @Order(42)
    fun `should calculate four of a kind`() {
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
        assertEquals(HandValues.FOUR_OF_A_KIND, hand.evaluateHandValue())
    }

    @Test
    @Order(43)
    fun `should prioritize full house over three of a kind`() {
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
        assertEquals(HandValues.FULL_HOUSE, hand.evaluateHandValue())
    }

    @Test
    @Order(44)
    fun `should calculate three of a kind when not full house`() {
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
        assertEquals(HandValues.THREE_OF_A_KIND, hand.evaluateHandValue())
    }

    @Test
    @Order(45)
    fun `should calculate two pair`() {
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
        assertEquals(HandValues.TWO_PAIR, hand.evaluateHandValue())
    }

    @Test
    @Order(46)
    fun `should calculate one pair`() {
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
        assertEquals(HandValues.ONE_PAIR, hand.evaluateHandValue())
    }

    @Test
    @Order(47)
    fun `should calculate no value`() {
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
        assertEquals(HandValues.NO_VALUE, hand.evaluateHandValue())
    }

    @Test
    @Order(48)
    fun `verify hand value ordering - five beats four`() {
        assertTrue(HandValues.FIVE_OF_A_KIND.value > HandValues.FOUR_OF_A_KIND.value)
    }

    @Test
    @Order(49)
    fun `verify hand value ordering - four beats full house`() {
        assertTrue(HandValues.FOUR_OF_A_KIND.value > HandValues.FULL_HOUSE.value)
    }

    @Test
    @Order(50)
    fun `verify hand value ordering - full house beats straight`() {
        assertTrue(HandValues.FULL_HOUSE.value > HandValues.STRAIGHT.value)
    }

    @Test
    @Order(51)
    fun `verify hand value ordering - straight beats three`() {
        assertTrue(HandValues.STRAIGHT.value > HandValues.THREE_OF_A_KIND.value)
    }

    @Test
    @Order(52)
    fun `verify hand value ordering - three beats two pair`() {
        assertTrue(HandValues.THREE_OF_A_KIND.value > HandValues.TWO_PAIR.value)
    }

    @Test
    @Order(53)
    fun `verify hand value ordering - two pair beats one pair`() {
        assertTrue(HandValues.TWO_PAIR.value > HandValues.ONE_PAIR.value)
    }

    @Test
    @Order(54)
    fun `verify hand value ordering - one pair beats no value`() {
        assertTrue(HandValues.ONE_PAIR.value > HandValues.NO_VALUE.value)
    }

    @Test
    @Order(55)
    fun `should handle empty hand`() {
        val emptyHand = Hand(emptyList())
        assertEquals(HandValues.NO_VALUE, emptyHand.evaluateHandValue())
    }

    @Test
    @Order(56)
    fun `should handle less than 5 dice`() {
        val smallHand = Hand(List(3) { Dice(DiceFace.KING) })
        assertEquals(HandValues.THREE_OF_A_KIND, smallHand.evaluateHandValue())
    }
}
