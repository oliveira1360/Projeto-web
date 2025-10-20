@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.GameService
import org.example.TransactionManager
import org.example.TransactionManagerMem
import org.example.config.GameDomainConfig
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateGameDTO
import org.example.dto.inputDto.ShuffleDTO
import org.example.entity.core.*
import org.example.entity.dice.Dice
import org.example.entity.dice.DiceFace
import org.example.entity.player.Hand
import org.example.entity.player.User
import org.example.game.RepositoryGameMem
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameControllerTest {
    private val userMem = RepositoryUserMem()
    private val lobbyMem = RepositoryLobbyMem()
    private val gameMem = RepositoryGameMem()
    private val generalMem = RepositoryInviteMem()
    private val trxManager: TransactionManager = TransactionManagerMem(userMem, lobbyMem, gameMem, generalMem)

    private var gameDomainConfig = GameDomainConfig(moneyRemove = 1)
    private var gameService: GameService = GameService(trxManager, gameDomainConfig)
    private val gameController = GameController(gameService)

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
                password = Password("SecurePass123!"),
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
                    name = Name("Test Lobby"),
                    hostId = hostId,
                    maxPlayers = maxPlayers,
                    rounds = 8,
                ).id
        }

    @BeforeEach
    fun cleanup() {
        trxManager.run {
            repositoryGame.clear()
            repositoryLobby.clear()
            repositoryUser.clear()
        }
        userCounter = 0

        user1 = createTestUser(name = "Player1", nickName = "player1", email = "player1@example.com")
        user2 = createTestUser(name = "Player2", nickName = "player2", email = "player2@example.com")
    }

    // ============================================
    // EASY TESTS - Game Creation
    // ============================================

    @Test
    fun `createGame should return CREATED with valid data`() {
        // given: a lobby with user1 as host
        val lobbyId = createTestLobby(user1.id)
        val input = CreateGameDTO(lobbyId)

        // when: creating game
        val resp = gameController.createGame(AuthenticatedUserDto(user1, "token"), input)

        // then: response is 201 with game details
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["gameId"])
        assertEquals("ACTIVE", body["status"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `createGame should fail with non-existent lobby`() {
        // given: invalid lobby id
        val input = CreateGameDTO(lobbyId = 9999)

        // when: creating game
        val resp = gameController.createGame(AuthenticatedUserDto(user1, "token"), input)

        // then: response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.LOBBY_NOT_FOUND, body.type)
    }

    @Test
    fun `createGame should fail when user not in lobby`() {
        // given: a lobby where user2 is not a member
        val lobbyId = createTestLobby(user1.id)
        val input = CreateGameDTO(lobbyId)

        // when: user2 tries to create game
        val resp = gameController.createGame(AuthenticatedUserDto(user2, "token"), input)

        // then: response is 409
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.USER_NOT_IN_GAME, body.type)
    }

    // ============================================
    // EASY TESTS - Close Game
    // ============================================

    @Test
    fun `closeGame should close game successfully`() {
        // given: an active game
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }

        // when: closing game
        val resp = gameController.closeGame(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 204 No Content
        assertEquals(HttpStatus.NO_CONTENT, resp.statusCode)
    }

    @Test
    fun `closeGame should fail with non-existent game`() {
        // when: closing non-existent game
        val resp = gameController.closeGame(AuthenticatedUserDto(user1, "token"), 9999)

        // then: response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - List Players
    // ============================================

    @Test
    fun `listPlayersInGame should return players list`() {
        // given: a game with one player
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }

        // when: listing players
        val resp = gameController.listPlayersInGame(gameId)

        // then: response is 200 with players
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val players = body["players"] as List<*>
        assertTrue(players.isNotEmpty())
        assertNotNull(body["_links"])
    }

    @Test
    fun `listPlayersInGame should fail for non-existent game`() {
        // when: listing players for non-existent game
        val resp = gameController.listPlayersInGame(9999)

        // then: response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Start Round
    // ============================================

    @Test
    fun `startRound should create first round`() {
        // given: a new game
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }

        // when: starting first round
        val resp = gameController.startRound(gameId)

        // then: response is 201 with round number
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(1, body["roundNumber"])
        assertTrue((body["message"] as String).contains("Round 1 started"))
        assertNotNull(body["_links"])
    }

    @Test
    fun `startRound should create multiple rounds sequentially`() {
        // given: a game
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }

        // when: starting multiple rounds
        val resp1 = gameController.startRound(gameId)
        val resp2 = gameController.startRound(gameId)

        // then: both are created
        assertEquals(HttpStatus.CREATED, resp1.statusCode)
        assertEquals(HttpStatus.CREATED, resp2.statusCode)

        val body2 = resp2.body as Map<*, *>
        assertEquals(2, body2["roundNumber"])
    }

    @Test
    fun `startRound should fail for non-existent game`() {
        // when: starting round for non-existent game
        val resp = gameController.startRound(9999)

        // then: response is 404
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Player Hand
    // ============================================

    @Test
    fun `getPlayerHand should return empty hand initially`() {
        // given: a game with started round
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // when: getting player hand before shuffle
        val resp = gameController.getPlayerHand(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 400 (empty hand)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Shuffle
    // ============================================

    @Test
    fun `shuffle should generate initial hand`() {
        // given: a game with started round
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // when: shuffling (first roll)
        val shuffleInput = ShuffleDTO(lockedDice = emptyList())
        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        // then: response is 200 with new hand
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val hand = body["hand"] as List<*>
        assertEquals(5, hand.size)
        assertNotNull(body["_links"])
    }

    @Test
    fun `shuffle should respect locked dice`() {
        // given: a game with an existing hand
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // first roll
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), ShuffleDTO(emptyList()), gameId)

        // when: shuffling with locked dice
        val shuffleInput = ShuffleDTO(lockedDice = listOf(0, 2, 4))
        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        // then: response is successful
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(5, (body["hand"] as List<*>).size)
    }

    @Test
    fun `shuffle should fail after 3 rolls`() {
        // given: a game where player already rolled 3 times
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // roll 3 times
        val shuffleInput = ShuffleDTO(emptyList())
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)
        gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        // when: trying to roll a 4th time
        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        // then: response is 403
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.TOO_MANY_ROLLS, body.type)
    }

    @Test
    fun `shuffle should fail without active round`() {
        // given: a game without started round
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }

        // when: trying to shuffle
        val shuffleInput = ShuffleDTO(emptyList())
        val resp = gameController.shuffle(AuthenticatedUserDto(user1, "token"), shuffleInput, gameId)

        // then: response is 409
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Finish Turn
    // ============================================

    @Test
    fun `finishTurn should calculate points`() {
        // given: a game with a hand
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // Create a hand with specific dice
        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        trxManager.run { repositoryGame.shuffle(user1.id, hand, gameId) }

        // when: finishing turn
        val resp = gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 200 with points
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["points"])
        assertEquals(true, body["finished"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `finishTurn should fail with empty hand`() {
        // given: a game without hand
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // when: trying to finish turn
        val resp = gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 400
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.EMPTY_HAND, body.type)
    }

    // ============================================
    // ADVANCED TESTS - Round Winner
    // ============================================

    @Test
    fun `getRoundWinner should return winner after all players finish`() {
        // given: a game with two players who finished
        val lobbyId = createTestLobby(user1.id, 2)
        trxManager.run { repositoryLobby.addPlayer(lobbyId, user2.id) }
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // both players get hands and finish
        val hand1 = Hand(List(5) { Dice(DiceFace.KING) })
        val hand2 = Hand(List(5) { Dice(DiceFace.ACE) })
        trxManager.run {
            repositoryGame.shuffle(user1.id, hand1, gameId)
            repositoryGame.shuffle(user1.id, hand2, gameId)
        }

        gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)
        gameController.finishTurn(AuthenticatedUserDto(user2, "token"), gameId)

        // when: getting round winner
        val resp = gameController.getRoundWinner(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 200 with winner
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val winner = body["winner"] as Map<*, *>
        assertNotNull(winner["playerId"])
        assertNotNull(winner["points"])
        assertNotNull(winner["handValue"])
        assertNotNull(body["_links"])
    }

    // ============================================
    // ADVANCED TESTS - Game Winner
    // ============================================

    @Test
    fun `getGameWinner should return overall winner`() {
        // given: a completed game
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        val hand = Hand(List(5) { Dice(DiceFace.KING) })
        trxManager.run { repositoryGame.shuffle(user1.id, hand, gameId) }
        gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        // when: getting game winner
        val resp = gameController.getGameWinner(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 200 with winner
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
    fun `getScoreboard should return player scores`() {
        // given: a game with players who finished
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        val hand = Hand(List(5) { Dice(DiceFace.ACE) })
        trxManager.run { repositoryGame.shuffle(user1.id, hand, gameId) }
        gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)

        // when: getting scoreboard
        val resp = gameController.getScoreboard(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 200 with scores
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        val players = body["players"] as List<*>
        assertTrue(players.isNotEmpty())
        assertNotNull(body["_links"])
    }

    @Test
    fun `remainingTime should return time left`() {
        // given: an active game
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // when: getting remaining time
        val resp = gameController.remainingTime(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 200 with time
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["remainingSeconds"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `getRoundInfo should return round information`() {
        // given: a game with started round
        val lobbyId = createTestLobby(user1.id)
        val gameId = trxManager.run { repositoryGame.createGame(user1.id, lobbyId) }
        gameController.startRound(gameId)

        // when: getting round info
        val resp = gameController.getRoundInfo(AuthenticatedUserDto(user1, "token"), gameId)

        // then: response is 200 with round info
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["round"])
        assertNotNull(body["players"])
        assertNotNull(body["_links"])
    }

    // ============================================
    // ADVANCED TESTS - HATEOAS & Edge Cases
    // ============================================

    @Test
    fun `HATEOAS links are present in all successful responses`() {
        // given: a complete game flow
        val lobbyId = createTestLobby(user1.id)
        val gameInput = CreateGameDTO(lobbyId)

        // create game
        val createResp = gameController.createGame(AuthenticatedUserDto(user1, "token"), gameInput)
        assertNotNull((createResp.body as Map<*, *>)["_links"])

        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        // list players
        val playersResp = gameController.listPlayersInGame(gameId)
        assertNotNull((playersResp.body as Map<*, *>)["_links"])

        // start round
        val roundResp = gameController.startRound(gameId)
        assertNotNull((roundResp.body as Map<*, *>)["_links"])

        // shuffle
        val shuffleResp =
            gameController.shuffle(
                AuthenticatedUserDto(user1, "token"),
                ShuffleDTO(emptyList()),
                gameId,
            )
        assertNotNull((shuffleResp.body as Map<*, *>)["_links"])
    }

    @Test
    fun `complete game workflow should work`() {
        // given: complete game workflow
        val lobbyId = createTestLobby(user1.id)

        // create game
        val createResp =
            gameController.createGame(
                AuthenticatedUserDto(user1, "token"),
                CreateGameDTO(lobbyId),
            )
        assertEquals(HttpStatus.CREATED, createResp.statusCode)
        val gameId = (createResp.body as Map<*, *>)["gameId"] as Int

        // start round
        val startResp = gameController.startRound(gameId)
        assertEquals(HttpStatus.CREATED, startResp.statusCode)

        // shuffle
        val shuffleResp =
            gameController.shuffle(
                AuthenticatedUserDto(user1, "token"),
                ShuffleDTO(emptyList()),
                gameId,
            )
        assertEquals(HttpStatus.OK, shuffleResp.statusCode)

        // finish turn
        val finishResp = gameController.finishTurn(AuthenticatedUserDto(user1, "token"), gameId)
        assertEquals(HttpStatus.OK, finishResp.statusCode)

        // get scoreboard
        val scoreResp = gameController.getScoreboard(AuthenticatedUserDto(user1, "token"), gameId)
        assertEquals(HttpStatus.OK, scoreResp.statusCode)

        // then: entire workflow succeeds
        assertTrue(true)
    }
}
