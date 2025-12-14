@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.*
import org.example.config.GameDomainConfig
import org.example.config.UsersDomainConfig
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateGameDTO
import org.example.dto.inputDto.ShuffleDTO
import org.example.entity.core.*
import org.example.entity.dice.Dice
import org.example.entity.dice.DiceFace
import org.example.entity.player.Hand
import org.example.entity.player.User
import org.example.game.*
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.token.Sha256TokenEncoder
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.*
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Clock
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
class GameControllerTest {
    private lateinit var userMem: RepositoryUserMem
    private lateinit var lobbyMem: RepositoryLobbyMem
    private lateinit var gameMem: RepositoryGameMem
    private lateinit var generalMem: RepositoryInviteMem
    private lateinit var trxManager: TransactionManager
    private lateinit var gameDomainConfig: GameDomainConfig
    private lateinit var notificationService: GameNotificationService
    private lateinit var validationService: GameValidationService
    private lateinit var roundService: RoundService
    private lateinit var playerTurnService: PlayerTurnService
    private lateinit var gameService: GameService
    private lateinit var userAuthService: UserAuthService
    private lateinit var errorHandler: HandleError
    private lateinit var gameController: GameController

    private lateinit var user1: User
    private lateinit var user2: User
    private var userCounter = 0

    @BeforeAll
    fun setupAll() {
        userMem = RepositoryUserMem()
        lobbyMem = RepositoryLobbyMem()
        gameMem = RepositoryGameMem()
        generalMem = RepositoryInviteMem()
    }

    @BeforeEach
    fun setup() {
        gameMem.clear()
        userMem.clear()
        lobbyMem.clear()

        trxManager = TransactionManagerMem(userMem, lobbyMem, gameMem, generalMem)
        gameDomainConfig = GameDomainConfig(moneyRemove = 1)
        notificationService = MockGameNotificationService()
        validationService = GameValidationService()

        val gameEndService =
            GameEndService(
                trxManager,
                notificationService,
                validationService,
            )

        roundService = RoundService(trxManager, validationService, notificationService, gameEndService)
        playerTurnService = PlayerTurnService(trxManager, validationService, notificationService, roundService)

        gameService =
            GameService(
                gameDomainConfig,
                roundService,
                playerTurnService,
                gameEndService,
            )

        val passwordEncoder = BCryptPasswordEncoder()
        val domainConfig =
            UsersDomainConfig(
                tokenSizeInBytes = 100,
                tokenTtl = Duration.ofMinutes(30),
                tokenRollingTtl = Duration.ofMinutes(60),
                maxTokensPerUser = 3,
            )
        val tokenEncoder = Sha256TokenEncoder()
        val clock = Clock.systemUTC()

        userAuthService =
            UserAuthService(
                passwordEncoder = passwordEncoder,
                tokenEncoder = tokenEncoder,
                config = domainConfig,
                trxManager = trxManager,
                clock = clock,
            )

        errorHandler = HandleError()
        gameController = GameController(gameService, userAuthService, notificationService, errorHandler)

        userCounter = 0
        user1 = createTestUser(name = "Player1", nickName = "player1", email = "player1@example.com")
        user2 = createTestUser(name = "Player2", nickName = "player2", email = "player2@example.com")
    }

    private fun createTestUser(
        name: String = "Test User",
        nickName: String = "testuser",
        email: String = "test@example.com",
    ): User {
        val uniqueEmail = Email("${userCounter++}$email")
        return trxManager.run {
            repositoryUser.createUser(
                name = Name(name),
                nickName = Name("$nickName$userCounter"),
                email = uniqueEmail,
                passwordHash = "SecurePass123!".toPasswordFromRaw(),
                imageUrl = URL("https://example.com/avatar.png"),
            )
        }
    }

    private fun createTestLobby(
        hostId: Int,
        maxPlayers: Int = 4,
    ): Int =
        trxManager.run {
            repositoryLobby
                .createLobby(
                    name = Name("Test Lobby ${System.currentTimeMillis()}"),
                    hostId = hostId,
                    maxPlayers = maxPlayers,
                    rounds = 8,
                ).id
        }

    // ============================================
    // EASY TESTS - Game Creation
    // ============================================

    @Test
    @Order(1)
    fun `createGame should return CREATED with valid data`() {
        val lobbyId = createTestLobby(user1.id)
        val input = CreateGameDTO(lobbyId)

        val resp = gameController.createGame(AuthenticatedUserDto(user1, "token"), input)

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["gameId"])
        assertEquals("ACTIVE", body["status"])
        assertNotNull(body["_links"])
    }

    @Test
    @Order(2)
    fun `createGame should fail with non-existent lobby`() {
        val input = CreateGameDTO(lobbyId = 9999)

        val resp = gameController.createGame(AuthenticatedUserDto(user1, "token"), input)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @Test
    @Order(3)
    fun `createGame should fail when user not in lobby`() {
        val lobbyId = createTestLobby(user1.id)
        val input = CreateGameDTO(lobbyId)

        val resp = gameController.createGame(AuthenticatedUserDto(user2, "token"), input)

        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
    }

    // ============================================
    // EASY TESTS - Close Game
    // ============================================

    @Test
    @Order(4)
    fun `closeGame should close game successfully`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        val resp = gameController.closeGame(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.NO_CONTENT, resp.statusCode)
    }

    @Test
    @Order(5)
    fun `closeGame should fail with non-existent game`() {
        val resp = gameController.closeGame(AuthenticatedUserDto(user1, "token"), 9999)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - List Players
    // ============================================

    @Test
    @Order(6)
    fun `listPlayersInGame should return players list`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        val resp = gameController.listPlayersInGame(gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val players = body["players"] as List<*>
        assertTrue(players.isNotEmpty())
        assertNotNull(body["_links"])
    }

    @Test
    @Order(7)
    fun `listPlayersInGame should fail for non-existent game`() {
        val resp = gameController.listPlayersInGame(9999)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Start Round
    // ============================================

    @Test
    @Order(8)
    fun `startRound should create first round`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        val resp = gameController.startRound(gameId)

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertTrue((body["roundNumber"] as Int) >= 1)
        assertTrue((body["message"] as String).contains("Round"))
        assertNotNull(body["_links"])
    }

    @Test
    @Order(9)
    fun `startRound should create multiple rounds sequentially`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        val resp1 = gameController.startRound(gameId)
        val resp2 = gameController.startRound(gameId)

        assertEquals(HttpStatus.CREATED, resp1.statusCode)
        assertEquals(HttpStatus.CREATED, resp2.statusCode)

        val body2 = resp2.body as Map<*, *>
        assertTrue((body2["roundNumber"] as Int) >= 2)
    }

    @Test
    @Order(10)
    fun `startRound should fail for non-existent game`() {
        val resp = gameController.startRound(9999)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Player Hand
    // ============================================

    @Test
    @Order(11)
    fun `getPlayerHand should return empty hand initially`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val resp = gameController.getPlayerHand(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Shuffle
    // ============================================

    @Test
    @Order(12)
    fun `shuffle should generate initial hand`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val shuffleInput = ShuffleDTO(lockedDice = emptyList())
        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val hand = body["hand"] as List<*>
        assertEquals(5, hand.size)
        assertNotNull(body["_links"])
    }

    @Test
    @Order(13)
    fun `shuffle should respect locked dice`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        gameController.shuffle(AuthenticatedUserDto(user1, "token"), ShuffleDTO(emptyList()), gameId)

        val shuffleInput = ShuffleDTO(lockedDice = listOf(0, 2, 4))
        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(5, (body["hand"] as List<*>).size)
    }

    @Test
    @Order(14)
    fun `shuffle should fail after 3 rolls`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val shuffleInput = ShuffleDTO(emptyList())
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Finish Turn
    // ============================================

    @Test
    @Order(16)
    fun `finishTurn should calculate points`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        trxManager.run {
            repositoryGame.populateEmptyTurns(gameId, 1, user1.id)
            repositoryGame.updateHandAndRoll(user1.id, gameId, hand, 1)
        }

        val resp = gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["points"])
        assertEquals(true, body["finished"])
        assertNotNull(body["_links"])
    }

    @Test
    @Order(17)
    fun `finishTurn should fail with empty hand`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val resp = gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    // ============================================
    // ADVANCED TESTS - Round Winner
    // ============================================

    // ============================================
    // ADVANCED TESTS - Game Winner
    // ============================================

    @Test
    @Order(19)
    fun `getGameWinner should return overall winner`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val hand = Hand(List(5) { Dice(DiceFace.KING) })
        trxManager.run {
            repositoryGame.populateEmptyTurns(gameId, 1, user1.id)
            repositoryGame.updateHandAndRoll(user1.id, gameId, hand, 1)
        }

        gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        val resp = gameController.getGameWinner(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val winner = body["winner"] as Map<*, *>
        assertNotNull(winner["playerId"])
        assertNotNull(winner["totalPoints"])
        assertNotNull(winner["roundsWon"])
        assertNotNull(body["_links"])
    }

    // ============================================
    // ADVANCED TESTS - Scoreboard & Time
    // ============================================

    @Test
    @Order(20)
    fun `getScoreboard should return player scores`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        trxManager.run {
            repositoryGame.populateEmptyTurns(gameId, 1, user1.id)
            repositoryGame.updateHandAndRoll(user1.id, gameId, hand, 1)
        }

        gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        val resp = gameController.getScoreboard(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val players = body["players"] as List<*>
        assertTrue(players.isNotEmpty())
        assertNotNull(body["_links"])
    }

    @Test
    @Order(21)
    fun `remainingTime should return time left`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val resp = gameController.remainingTime(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["remainingSeconds"])
        assertNotNull(body["_links"])
    }

    @Test
    @Order(22)
    fun `getRoundInfo should return round information`() {
        val lobbyId = createTestLobby(user1.id)
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), CreateGameDTO(lobbyId))
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int
        gameController.startRound(gameId)

        val resp = gameController.getRoundInfo(AuthenticatedUserDto(user1, "token"), gameId)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["round"])
        assertNotNull(body["players"])
        assertNotNull(body["_links"])
    }

    // ============================================
    // ADVANCED TESTS - HATEOAS & Complete Workflow
    // ============================================

    @Test
    @Order(23)
    fun `HATEOAS links are present in all successful responses`() {
        val lobbyId = createTestLobby(user1.id)
        val gameInput = CreateGameDTO(lobbyId)

        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), gameInput)
        assertNotNull((createResp.body as Map<*, *>)["_links"])

        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        val playersResp = gameController.listPlayersInGame(gameId)
        assertNotNull((playersResp.body as Map<*, *>)["_links"])

        val roundResp = gameController.startRound(gameId)
        assertNotNull((roundResp.body as Map<*, *>)["_links"])

        val shuffleResp =
            gameController.shuffle(
                AuthenticatedUserDto(user1, "token"),
                ShuffleDTO(emptyList()),
                gameId,
            )
        assertNotNull((shuffleResp.body as Map<*, *>)["_links"])
    }

    @Test
    @Order(24)
    fun `complete game workflow should work`() {
        val lobbyId = createTestLobby(user1.id)

        val createResp =
            gameController.createGame(
                AuthenticatedUserDto(user1, "token"),
                CreateGameDTO(lobbyId),
            )
        assertEquals(HttpStatus.CREATED, createResp.statusCode)
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        val startResp = gameController.startRound(gameId)
        assertEquals(HttpStatus.CREATED, startResp.statusCode)

        val shuffleResp =
            gameController.shuffle(
                AuthenticatedUserDto(user1, "token"),
                ShuffleDTO(emptyList()),
                gameId,
            )
        assertEquals(HttpStatus.OK, shuffleResp.statusCode)

        val finishResp = gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)
        assertEquals(HttpStatus.OK, finishResp.statusCode)

        val scoreResp = gameController.getScoreboard(AuthenticatedUserDto(user1, "token"), gameId)
        assertEquals(HttpStatus.OK, scoreResp.statusCode)

        assertTrue(true)
    }
}
