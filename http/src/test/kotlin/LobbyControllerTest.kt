@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.*
import org.example.config.GameDomainConfig
import org.example.config.LobbiesDomainConfig
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateLobbyDTO
import org.example.entity.core.*
import org.example.entity.player.User
import org.example.game.GameService
import org.example.game.GameValidationService
import org.example.game.PlayerTurnService
import org.example.game.RepositoryGameMem
import org.example.game.RoundService
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

const val MAX_PLAYERS = 4

class LobbyControllerTest {
    private val lobbiesDomainConfig =
        LobbiesDomainConfig(
            maxPlayersPerLobby = 6,
            maxLobbiesPerUser = 1,
        )

    private val userMem: RepositoryUserMem
        get() = RepositoryLobbyMem.userRepo
    private val lobbyMem = RepositoryLobbyMem()
    private val lobbyNotificationService = LobbyNotificationService()
    private val gameMem = RepositoryGameMem()
    private val generalMem = RepositoryInviteMem()
    private val trxManager = TransactionManagerMem(userMem, lobbyMem, gameMem, generalMem)

    val gameDomainConfig = GameDomainConfig(moneyRemove = 1)
    val notificationService = MockGameNotificationService()
    val validationService = GameValidationService()
    val roundService = RoundService(trxManager, validationService, notificationService)
    val playerTurnService = PlayerTurnService(trxManager, validationService, notificationService, roundService)
    var gameService =
        GameService(
            trxManager,
            gameDomainConfig,
            notificationService,
            validationService,
            roundService,
            playerTurnService,
        )

    private val lobbyService = LobbyService(trxManager, lobbiesDomainConfig, lobbyNotificationService, gameService)
    private val controllerEvents = LobbyController(lobbyService, lobbyNotificationService)

    private lateinit var user1: User
    private lateinit var user2: User
    private var userCounter = 0

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
                passwordHash = "SecurePass123!",
                imageUrl = URL("https://example.com/avatar.png"),
            )
        }
    }

    @BeforeEach
    fun cleanup() {
        trxManager.run {
            repositoryLobby.clear()
            repositoryUser.clear()
        }
        userCounter = 0

        // Cria dois utilizadores com dados consistentes
        user1 =
            trxManager.run {
                repositoryUser.createUser(
                    name = Name("John Doe"),
                    nickName = Name("john"),
                    email = Email("john@example.com"),
                    passwordHash = "SecurePass123!",
                    imageUrl = URL("https://example.com/john.png"),
                )
            }

        user2 =
            trxManager.run {
                repositoryUser.createUser(
                    name = Name("Jane Smith"),
                    nickName = Name("jane"),
                    email = Email("jane@example.com"),
                    passwordHash = "SecurePass123!",
                    imageUrl = URL("https://example.com/jane.png"),
                )
            }

        // Cria duas lobbies
        trxManager.run {
            repositoryLobby.createLobby(
                name = Name("Test Lobby 1"),
                hostId = user1.id,
                maxPlayers = 4,
                rounds = 8,
            )
        }

        trxManager.run {
            repositoryLobby.createLobby(
                name = Name("Test Lobby 2"),
                hostId = user2.id,
                maxPlayers = 4,
                rounds = 8,
            )
        }
    }

    // ============================================
    // EASY TESTS - Basic Listing
    // ============================================

    @Test
    fun `listLobbies should return empty list when no lobbies exist`() {
        // given: no lobbies
        trxManager.run { repositoryLobby.clear() }

        // when: listing lobbies
        val resp = controllerEvents.listLobbies()

        // then: the response is 200 with empty list
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val lobbies = body["lobbies"] as List<*>
        assertEquals(0, lobbies.size)
        assertNotNull(body["_links"])
    }

    @Test
    fun `listLobbies should return all lobbies`() {
        // when: listing lobbies
        val resp = controllerEvents.listLobbies()

        // then: the response is 200 with two lobbies
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val lobbies = body["lobbies"] as List<*>

        assertEquals(2, lobbies.size)

        val lobby1 = lobbies[0] as Map<*, *>
        assertNotNull(lobby1["lobbyId"])
        assertEquals("Test Lobby 1", lobby1["name"])
        assertEquals(4, lobby1["maxPlayers"])
        assertEquals(1, lobby1["currentPlayers"])

        val lobby2 = lobbies[1] as Map<*, *>
        assertNotNull(lobby2["lobbyId"])
        assertEquals("Test Lobby 2", lobby2["name"])

        assertNotNull(body["_links"])
    }

    @Test
    fun `listLobbies should include links for navigation`() {
        // when: listing lobbies
        val resp = controllerEvents.listLobbies()

        // then: response contains HATEOAS links
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val links = body["_links"] as Map<*, *>
        assertNotNull(links["self"])
    }

    // ============================================
    // EASY TESTS - Basic Lobby Creation
    // ============================================

    @Test
    fun `createLobby should return CREATED with valid data`() {
        // given: a new lobby input
        val input =
            CreateLobbyDTO(
                name = "New Test Lobby",
                maxPlayers = MAX_PLAYERS,
                rounds = 8,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 201 with lobby details
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("New Test Lobby", body["name"])
        assertEquals(MAX_PLAYERS, body["maxPlayers"])
        assertEquals(1, body["currentPlayers"])
        assertNotNull(body["lobbyId"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `createLobby should fail with blank name`() {
        // given: a lobby with blank name
        val input =
            CreateLobbyDTO(
                name = "",
                maxPlayers = MAX_PLAYERS,
                rounds = 8,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 400
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.INVALID_LOBBY_DATA, body.type)
    }

    @Test
    fun `createLobby should fail with whitespace-only name`() {
        // given: a lobby with whitespace name
        val input =
            CreateLobbyDTO(
                name = "   ",
                maxPlayers = MAX_PLAYERS,
                rounds = 8,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 400
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `createLobby should fail with zero max players`() {
        // given: a lobby with 0 max players
        val input =
            CreateLobbyDTO(
                name = "Invalid Lobby",
                maxPlayers = 0,
                rounds = 8,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 400
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `createLobby should fail with negative max players`() {
        // given: a lobby with negative max players
        val input =
            CreateLobbyDTO(
                name = "Invalid Lobby",
                maxPlayers = -5,
                rounds = 8,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 400
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    // ============================================
    // EASY TESTS - Get Lobby Details
    // ============================================

    @Test
    fun `getLobbyDetails should return lobby info`() {
        // given: an existing lobby
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // when: getting lobby details
        val resp = controllerEvents.getLobbyDetails(lobbyId)

        // then: the response is 200 with lobby details
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(lobbyId, body["lobbyId"])
        assertEquals("Test Lobby 1", body["name"])
        assertEquals(4, body["maxPlayers"])
        assertEquals(1, body["currentPlayers"])
        assertEquals(8, body["rounds"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `getLobbyDetails should return 404 for non-existent lobby`() {
        // given: a non-existent lobby id
        val nonExistentId = 9999

        // when: getting lobby details
        val resp = controllerEvents.getLobbyDetails(nonExistentId)

        // then: the response is 404 with problem details
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.LOBBY_NOT_FOUND, body.type)
        assertEquals("Lobby Not Found", body.title)
        assertEquals(404, body.status)
    }

    @Test
    fun `getLobbyDetails should return 404 for negative lobby id`() {
        // when: getting details with negative id
        val resp = controllerEvents.getLobbyDetails(-1)

        // then: the response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Join Lobby
    // ============================================

    @Test
    fun `joinLobby should add player to lobby`() {
        // given: a lobby and a second user
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // when: user2 joins the lobby
        val resp =
            controllerEvents.joinLobby(
                user = AuthenticatedUserDto(user2, "token2"),
                lobbyId = lobbyId,
            )

        // then: the response is 202
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(lobbyId, body["lobbyId"])
        assertEquals("Successfully joined lobby", body["message"])
        assertNotNull(body["_links"])

        // and: the lobby now has 2 players
        val detailsResp = controllerEvents.getLobbyDetails(lobbyId)
        val detailsBody = detailsResp.body as Map<*, *>
        assertEquals(2, detailsBody["currentPlayers"])
    }

    @Test
    fun `joinLobby should return 404 for non-existent lobby`() {
        // when: user tries to join non-existent lobby
        val resp =
            controllerEvents.joinLobby(
                user = AuthenticatedUserDto(user2, "token2"),
                lobbyId = 9999,
            )

        // then: the response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.LOBBY_NOT_FOUND, body.type)
    }

    @Test
    fun `joinLobby should return 409 when lobby is full`() {
        // given: a lobby with max 2 players
        val smallLobby =
            trxManager.run {
                repositoryLobby.createLobby(
                    name = Name("Small Lobby"),
                    hostId = user1.id,
                    maxPlayers = 2,
                    rounds = 4,
                )
            }

        // and: user2 joins
        trxManager.run {
            repositoryLobby.addPlayer(smallLobby.id, user2.id)
        }

        // and: a third user
        val user3 = createTestUser(name = "Bob", nickName = "bob", email = "bob@example.com")

        // when: user3 tries to join the full lobby
        val resp =
            controllerEvents.joinLobby(
                user = AuthenticatedUserDto(user3, "token3"),
                lobbyId = smallLobby.id,
            )

        // then: the response is 409
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.LOBBY_FULL, body.type)
        assertEquals("Lobby Full", body.title)
    }

    @Test
    fun `joinLobby should return 409 when user already in lobby`() {
        // given: a lobby
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // and: user2 joins
        controllerEvents.joinLobby(
            user = AuthenticatedUserDto(user2, "token2"),
            lobbyId = lobbyId,
        )

        // when: user2 tries to join again
        val resp =
            controllerEvents.joinLobby(
                user = AuthenticatedUserDto(user2, "token2"),
                lobbyId = lobbyId,
            )

        // then: the response is 409
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.ALREADY_IN_LOBBY, body.type)
    }

    @Test
    fun `joinLobby should allow multiple players to join sequentially`() {
        // given: a lobby
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // and: multiple users
        val user3 = createTestUser(name = "User3", nickName = "user3", email = "user3@example.com")
        val user4 = createTestUser(name = "User4", nickName = "user4", email = "user4@example.com")

        // when: users join sequentially
        val resp2 = controllerEvents.joinLobby(AuthenticatedUserDto(user2, "token2"), lobbyId)
        val resp3 = controllerEvents.joinLobby(AuthenticatedUserDto(user3, "token3"), lobbyId)
        val resp4 = controllerEvents.joinLobby(AuthenticatedUserDto(user4, "token4"), lobbyId)

        // then: all joins are successful
        assertEquals(HttpStatus.ACCEPTED, resp2.statusCode)
        assertEquals(HttpStatus.ACCEPTED, resp3.statusCode)
        assertEquals(HttpStatus.ACCEPTED, resp4.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Leave Lobby
    // ============================================

    @Test
    fun `leaveLobby should return 404 for non-existent lobby`() {
        // when: user tries to leave non-existent lobby
        val resp =
            controllerEvents.leaveLobby(
                user = AuthenticatedUserDto(user2, "token2"),
                lobbyId = 9999,
            )

        // then: the response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.LOBBY_NOT_FOUND, body.type)
    }

    @Test
    fun `leaveLobby should return 409 when user not in lobby`() {
        // given: a lobby that user2 hasn't joined
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // when: user2 tries to leave
        val resp =
            controllerEvents.leaveLobby(
                user = AuthenticatedUserDto(user2, "token2"),
                lobbyId = lobbyId,
            )

        // then: the response is 409
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.NOT_IN_LOBBY, body.type)
    }

    // ============================================
    // ADVANCED TESTS - Complex Scenarios
    // ============================================

    @Test
    fun `user can leave and rejoin lobby`() {
        // given: a lobby
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // when: user2 joins, leaves, then rejoins
        controllerEvents.joinLobby(AuthenticatedUserDto(user2, "token2"), lobbyId)
        controllerEvents.leaveLobby(AuthenticatedUserDto(user2, "token2"), lobbyId)
        val rejoinResp = controllerEvents.joinLobby(AuthenticatedUserDto(user2, "token2"), lobbyId)

        // then: rejoin is successful
        assertEquals(HttpStatus.ACCEPTED, rejoinResp.statusCode)

        // and: lobby has 2 players again
        val detailsResp = controllerEvents.getLobbyDetails(lobbyId)
        val detailsBody = detailsResp.body as Map<*, *>
        assertEquals(2, detailsBody["currentPlayers"])
    }

    /*
    @Test
    fun `lobby capacity is properly enforced with join and leave operations`() {
        // given: a lobby with max 3 players
        val lobby =
            trxManager.run {
                repositoryLobby.createLobby(
                    name = Name("Capacity Test"),
                    hostId = user1.id,
                    maxPlayers = 3,
                    rounds = 4,
                )
            }

        // and: additional users
        val user3 = createTestUser(name = "User3", nickName = "user3", email = "user3@example.com")
        val user4 = createTestUser(name = "User4", nickName = "user4", email = "user4@example.com")

        // when: filling the lobby
        controllerEvents.joinLobby(AuthenticatedUserDto(user2, "token2"), lobby.id)
        controllerEvents.joinLobby(AuthenticatedUserDto(user3, "token3"), lobby.id)

        // then: lobby is full
        val fullResp = controllerEvents.joinLobby(AuthenticatedUserDto(user4, "token4"), lobby.id)
        assertEquals(HttpStatus.NOT_FOUND, fullResp.statusCode)
    }


     */
    @Test
    fun `multiple lobbies can coexist with different players`() {
        // given: multiple lobbies and users
        val user3 = createTestUser(name = "User3", nickName = "user3", email = "user3@example.com")
        val user4 = createTestUser(name = "User4", nickName = "user4", email = "user4@example.com")

        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobby1Id = lobbies[0].id
        val lobby2Id = lobbies[1].id

        // when: different users join different lobbies
        controllerEvents.joinLobby(AuthenticatedUserDto(user3, "token3"), lobby1Id)
        controllerEvents.joinLobby(AuthenticatedUserDto(user4, "token4"), lobby2Id)

        // then: both lobbies have 2 players
        val lobby1Details = controllerEvents.getLobbyDetails(lobby1Id).body as Map<*, *>
        val lobby2Details = controllerEvents.getLobbyDetails(lobby2Id).body as Map<*, *>

        assertEquals(2, lobby1Details["currentPlayers"])
        assertEquals(2, lobby2Details["currentPlayers"])
    }

    @Test
    fun `createLobby with minimum valid values`() {
        // given: minimum valid lobby configuration
        val input =
            CreateLobbyDTO(
                name = "M",
                maxPlayers = 1,
                rounds = 1,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 201
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("M", body["name"])
        assertEquals(1, body["maxPlayers"])
        assertEquals(1, body["rounds"])
    }

    @Test
    fun `createLobby with maximum expected values`() {
        // given: large lobby configuration
        val input =
            CreateLobbyDTO(
                name = "Epic Battle Royale Championship Tournament 2024",
                maxPlayers = 100,
                rounds = 50,
            )

        // when: creating the lobby
        val resp =
            controllerEvents.createLobby(
                user = AuthenticatedUserDto(user1, "12345678"),
                body = input,
            )

        // then: the response is 201
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("Epic Battle Royale Championship Tournament 2024", body["name"])
        assertEquals(100, body["maxPlayers"])
        assertEquals(50, body["rounds"])
    }

    @Test
    fun `all lobby operations maintain data consistency`() {
        // given: a lobby
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val lobbyId = lobbies[0].id

        // when: performing sequence of operations
        val user3 = createTestUser(name = "User3", nickName = "user3", email = "user3@example.com")

        // Initial state: 1 player
        var details = controllerEvents.getLobbyDetails(lobbyId).body as Map<*, *>
        assertEquals(1, details["currentPlayers"])

        // After join: 2 players
        controllerEvents.joinLobby(AuthenticatedUserDto(user2, "token2"), lobbyId)
        details = controllerEvents.getLobbyDetails(lobbyId).body as Map<*, *>
        assertEquals(2, details["currentPlayers"])

        // After another join: 3 players
        controllerEvents.joinLobby(AuthenticatedUserDto(user3, "token3"), lobbyId)
        details = controllerEvents.getLobbyDetails(lobbyId).body as Map<*, *>
        assertEquals(3, details["currentPlayers"])

        // After leave: 2 players
        controllerEvents.leaveLobby(AuthenticatedUserDto(user2, "token2"), lobbyId)
        details = controllerEvents.getLobbyDetails(lobbyId).body as Map<*, *>
        assertEquals(2, details["currentPlayers"])

        // then: all operations maintain consistency
        assertTrue(true)
    }

    @Test
    fun `stress test - rapid join and leave operations`() {
        // given: a lobby and multiple users
        val lobby =
            trxManager.run {
                repositoryLobby.createLobby(
                    name = Name("Stress Test Lobby"),
                    hostId = user1.id,
                    maxPlayers = 10,
                    rounds = 8,
                )
            }

        val users =
            (1..5).map { i ->
                createTestUser(name = "StressUser$i", nickName = "stress$i", email = "stress$i@example.com")
            }

        // when: rapid join operations
        users.forEach { user ->
            val resp = controllerEvents.joinLobby(AuthenticatedUserDto(user, "token"), lobby.id)
            assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        }

        // then: all users joined successfully
        val afterJoinDetails = controllerEvents.getLobbyDetails(lobby.id).body as Map<*, *>
        assertEquals(6, afterJoinDetails["currentPlayers"]) // host + 5 users

        // when: rapid leave operations
        users.take(3).forEach { user ->
            val resp = controllerEvents.leaveLobby(AuthenticatedUserDto(user, "token"), lobby.id)
            assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        }

        // then: correct number of players remain
        val afterLeaveDetails = controllerEvents.getLobbyDetails(lobby.id).body as Map<*, *>
        assertEquals(3, afterLeaveDetails["currentPlayers"]) // host + 2 remaining users
    }

    @Test
    fun `edge case - single player lobby cannot accept more players`() {
        // given: a lobby with exactly 1 max player
        val soloLobby =
            trxManager.run {
                repositoryLobby.createLobby(
                    name = Name("Solo Lobby"),
                    hostId = user1.id,
                    maxPlayers = 1,
                    rounds = 4,
                )
            }

        // when: another user tries to join
        val resp =
            controllerEvents.joinLobby(
                user = AuthenticatedUserDto(user2, "token2"),
                lobbyId = soloLobby.id,
            )

        // then: join fails with lobby full
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.LOBBY_FULL, body.type)
    }

    @Test
    fun `HATEOAS links are present in all successful responses`() {
        // given: various operations
        val input = CreateLobbyDTO(name = "HATEOAS Test", maxPlayers = 4, rounds = 8)

        // when: creating lobby
        val createResp = controllerEvents.createLobby(AuthenticatedUserDto(user1, "token"), input)
        val createBody = createResp.body as Map<*, *>
        assertNotNull(createBody["_links"], "Create lobby should have _links")

        // when: listing lobbies
        val listResp = controllerEvents.listLobbies()
        val listBody = listResp.body as Map<*, *>
        assertNotNull(listBody["_links"], "List lobbies should have _links")

        // when: getting details
        val lobbies = trxManager.run { repositoryLobby.findAll() }
        val detailsResp = controllerEvents.getLobbyDetails(lobbies[0].id)
        val detailsBody = detailsResp.body as Map<*, *>
        assertNotNull(detailsBody["_links"], "Lobby details should have _links")

        // when: joining lobby
        val joinResp = controllerEvents.joinLobby(AuthenticatedUserDto(user2, "token2"), lobbies[0].id)
        val joinBody = joinResp.body as Map<*, *>
        assertNotNull(joinBody["_links"], "Join lobby should have _links")

        // when: leaving lobby
        val leaveResp = controllerEvents.leaveLobby(AuthenticatedUserDto(user2, "token2"), lobbies[0].id)
        val leaveBody = leaveResp.body as Map<*, *>
        assertNotNull(leaveBody["_links"], "Leave lobby should have _links")
    }
}
