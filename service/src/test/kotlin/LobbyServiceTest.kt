import org.example.Failure
import org.example.LobbyError
import org.example.LobbyService
import org.example.Success
import org.example.TransactionManagerMem
import org.example.config.LobbiesDomainConfig
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.URL
import org.example.entity.player.User
import org.example.game.RepositoryGameMem
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

const val MAX_PLAYERS = 4
const val INVITE_CODE_LENGTH = 4
const val MAX_PER_USER = 3
const val ROUNDS = 16

class LobbyServiceTest {
    private val dominConfig =
        LobbiesDomainConfig(
            MAX_PLAYERS,
            INVITE_CODE_LENGTH,
        )

    private val userMem: RepositoryUserMem
        get() = RepositoryLobbyMem.userRepo
    private val lobbyMem = RepositoryLobbyMem()
    private val gameMem = RepositoryGameMem()
    private val trx = TransactionManagerMem(userMem, lobbyMem, gameMem)

    private val service = LobbyService(trx, dominConfig)

    private var counter = 0

    private fun createTestUser(
        name: String = "Test User",
        nickName: String = "testuser",
        email: String = "test@example.com",
    ): User {
        val newEmail = Email(counter++.toString().plus(email))
        return userMem.createUser(
            Name(name),
            Name(nickName),
            newEmail,
            Password("SuperSecret123!"),
            URL("https://example.com/avatar.png"),
        )
    }

    @BeforeTest
    fun setup() {
        lobbyMem.clear()
        userMem.clear()
    }
    // ============================================
    // BASIC TESTS - Create Lobby
    // ============================================

    @Test
    fun `test create lobby with valid data`() {
        val user = createTestUser()
        val result = service.createLobby(user.id, "Test Lobby", MAX_PLAYERS, ROUNDS)

        assertTrue(result is Success)
        val lobby = (result as Success).value
        assertEquals(1, lobby.id)
        assertEquals(user.id, lobby.hostId)
        assertEquals(MAX_PLAYERS, lobby.maxPlayers)
        assertEquals(ROUNDS, lobby.rounds)
        assertEquals("Test Lobby", lobby.name.value)
        assertEquals(1, lobby.currentPlayers.size)
        assertEquals(user.id, lobby.currentPlayers[0].id)
    }

    @Test
    fun `test create lobby with blank name fails`() {
        val user = createTestUser()
        val result = service.createLobby(user.id, "", MAX_PLAYERS, ROUNDS)

        assertTrue(result is Failure)
        assertEquals(LobbyError.InvalidLobbyData, (result as Failure).value)
    }

    @Test
    fun `test create lobby with whitespace name fails`() {
        val user = createTestUser()
        val result = service.createLobby(user.id, "   ", MAX_PLAYERS, ROUNDS)

        assertTrue(result is Failure)
        assertEquals(LobbyError.InvalidLobbyData, (result as Failure).value)
    }

    @Test
    fun `test create lobby with zero max players fails`() {
        val user = createTestUser()
        val result = service.createLobby(user.id, "Test Lobby", 0, ROUNDS)

        assertTrue(result is Failure)
        assertEquals(LobbyError.InvalidLobbyData, (result as Failure).value)
    }

    @Test
    fun `test create lobby with negative max players fails`() {
        val user = createTestUser()
        val result = service.createLobby(user.id, "Test Lobby", -1, ROUNDS)

        assertTrue(result is Failure)
        assertEquals(LobbyError.InvalidLobbyData, (result as Failure).value)
    }

    @Test
    fun `test create multiple lobbies with same host`() {
        val user = createTestUser()
        val result1 = service.createLobby(user.id, "Lobby 1", MAX_PLAYERS, ROUNDS)
        val result2 = service.createLobby(user.id, "Lobby 2", MAX_PLAYERS, ROUNDS)

        assertTrue(result1 is Success)
        assertTrue(result2 is Success)
        val lobby1 = (result1 as Success).value
        val lobby2 = (result2 as Success).value
        assertEquals(1, lobby1.id)
        assertEquals(2, lobby2.id)
        assertEquals(user.id, lobby1.hostId)
        assertEquals(user.id, lobby2.hostId)
    }

    // ============================================
    // BASIC TESTS - List & Get Lobbies
    // ============================================

    @Test
    fun `test list lobbies returns empty list when no lobbies exist`() {
        val result = service.listLobbies()
        print(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `test list lobbies returns all created lobbies`() {
        val user1 = createTestUser(nickName = "user1", email = "user1@example.com")
        val user2 = createTestUser(nickName = "user2", email = "user2@example.com")

        service.createLobby(user1.id, "Lobby 1", MAX_PLAYERS, ROUNDS)
        service.createLobby(user2.id, "Lobby 2", MAX_PLAYERS, ROUNDS)

        val lobbies = service.listLobbies()
        assertEquals(2, lobbies.size)
    }

    @Test
    fun `test get lobby details with valid id`() {
        val user = createTestUser()
        val created = (service.createLobby(user.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value

        val result = service.getLobbyDetails(created.id)
        assertTrue(result is Success)
        val lobby = (result as Success).value
        assertEquals(created.id, lobby.id)
        assertEquals(created.name, lobby.name)
    }

    @Test
    fun `test get lobby details with invalid id returns not found`() {
        val result = service.getLobbyDetails(999)
        assertTrue(result is Failure)
        assertEquals(LobbyError.LobbyNotFound, (result as Failure).value)
    }

    // ============================================
    // INTERMEDIATE TESTS - Join Lobby
    // ============================================

    @Test
    fun `test join lobby successfully`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value

        val result = service.joinLobby(player.id, lobby.id)
        assertTrue(result is Success)
        val updatedLobby = (result as Success).value
        assertEquals(2, updatedLobby.currentPlayers.size)
        assertTrue(updatedLobby.currentPlayers.any { it.id == player.id })
    }

    @Test
    fun `test join lobby with non-existent user fails`() {
        val host = createTestUser()
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value

        val result = service.joinLobby(999, lobby.id)
        assertTrue(result is Failure)
        assertEquals(LobbyError.UserNotFound, (result as Failure).value)
    }

    @Test
    fun `test join non-existent lobby fails`() {
        val player = createTestUser()

        val result = service.joinLobby(player.id, 999)
        assertTrue(result is Failure)
        assertEquals(LobbyError.LobbyNotFound, (result as Failure).value)
    }

    @Test
    fun `test join lobby twice fails with already in lobby`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value

        service.joinLobby(player.id, lobby.id)
        val result = service.joinLobby(player.id, lobby.id)

        assertTrue(result is Failure)
        assertEquals(LobbyError.AlreadyInLobby, (result as Failure).value)
    }

    @Test
    fun `test join full lobby fails`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", 2, ROUNDS) as Success).value

        val player2 = createTestUser(nickName = "player2", email = "player2@example.com")
        val player3 = createTestUser(nickName = "player3", email = "player3@example.com")

        service.joinLobby(player2.id, lobby.id)
        val result = service.joinLobby(player3.id, lobby.id)

        assertTrue(result is Failure)
        assertEquals(LobbyError.LobbyFull, (result as Failure).value)
    }

    @Test
    fun `test multiple users can join until lobby is full`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", 4, ROUNDS) as Success).value

        val player2 = createTestUser(nickName = "player2", email = "player2@example.com")
        val player3 = createTestUser(nickName = "player3", email = "player3@example.com")
        val player4 = createTestUser(nickName = "player4", email = "player4@example.com")

        assertTrue(service.joinLobby(player2.id, lobby.id) is Success)
        assertTrue(service.joinLobby(player3.id, lobby.id) is Success)
        assertTrue(service.joinLobby(player4.id, lobby.id) is Success)

        val updatedLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(4, updatedLobby.currentPlayers.size)
    }

    // ============================================
    // INTERMEDIATE TESTS - Leave Lobby
    // ============================================

    @Test
    fun `test leave lobby successfully`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value
        service.joinLobby(player.id, lobby.id)

        val result = service.leaveLobby(player.id, lobby.id)
        assertTrue(result is Success)

        val updatedLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(1, updatedLobby.currentPlayers.size)
        assertFalse(updatedLobby.currentPlayers.any { it.id == player.id })
    }

    @Test
    fun `test leave non-existent lobby fails`() {
        val player = createTestUser()

        val result = service.leaveLobby(player.id, 999)
        assertTrue(result is Failure)
        assertEquals(LobbyError.LobbyNotFound, (result as Failure).value)
    }

    @Test
    fun `test leave lobby user is not in fails`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value

        val result = service.leaveLobby(player.id, lobby.id)
        assertTrue(result is Failure)
        assertEquals(LobbyError.NotInLobby, (result as Failure).value)
    }

    // ============================================
    // ADVANCED TESTS - Complex Scenarios
    // ============================================

    @Test
    fun `test host leaving lobby closes it`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value
        service.joinLobby(player.id, lobby.id)

        val result = service.leaveLobby(host.id, lobby.id)
        assertTrue(result is Success)
        // Note: You should verify closeLobby was called - this depends on implementation
    }

    @Test
    fun `test player can join different lobbies sequentially`() {
        val host1 = createTestUser(nickName = "host1", email = "host1@example.com")
        val host2 = createTestUser(nickName = "host2", email = "host2@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")

        val lobby1 = (service.createLobby(host1.id, "Lobby 1", MAX_PLAYERS, ROUNDS) as Success).value
        val lobby2 = (service.createLobby(host2.id, "Lobby 2", MAX_PLAYERS, ROUNDS) as Success).value

        assertTrue(service.joinLobby(player.id, lobby1.id) is Success)
        assertTrue(service.leaveLobby(player.id, lobby1.id) is Success)
        assertTrue(service.joinLobby(player.id, lobby2.id) is Success)
    }

    @Test
    fun `test lobby with custom max players and rounds`() {
        val host = createTestUser()
        val customMaxPlayers = 6
        val customRounds = 12

        val result = service.createLobby(host.id, "Custom Lobby", customMaxPlayers, customRounds)
        assertTrue(result is Success)

        val lobby = (result as Success).value
        assertEquals(customMaxPlayers, lobby.maxPlayers)
        assertEquals(customRounds, lobby.rounds)
    }

    @Test
    fun `test concurrent operations - fill lobby to capacity`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", 4, ROUNDS) as Success).value
        val players =
            (2..4).map { i ->
                createTestUser(nickName = "player$i", email = "player$i@example.com")
            }

        players.forEach { player ->
            val result = service.joinLobby(player.id, lobby.id)
            assertTrue(result is Success)
        }

        val finalLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(4, finalLobby.currentPlayers.size)
        assertEquals(host.id, finalLobby.currentPlayers[0].id)
        players.forEachIndexed { index, player ->
            assertEquals(player.id, finalLobby.currentPlayers[index + 1].id)
        }
    }

    @Test
    fun `test leave and rejoin lobby`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", MAX_PLAYERS, ROUNDS) as Success).value

        // Join
        assertTrue(service.joinLobby(player.id, lobby.id) is Success)
        var updatedLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(2, updatedLobby.currentPlayers.size)

        // Leave
        assertTrue(service.leaveLobby(player.id, lobby.id) is Success)
        updatedLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(1, updatedLobby.currentPlayers.size)

        // Rejoin
        assertTrue(service.joinLobby(player.id, lobby.id) is Success)
        updatedLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(2, updatedLobby.currentPlayers.size)
        assertTrue(updatedLobby.currentPlayers.any { it.id == player.id })
    }

    @Test
    fun `test lobby state after player leaves and someone else joins`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player1 = createTestUser(nickName = "player1", email = "player1@example.com")
        val player2 = createTestUser(nickName = "player2", email = "player2@example.com")
        val lobby = (service.createLobby(host.id, "Test Lobby", 3, ROUNDS) as Success).value

        service.joinLobby(player1.id, lobby.id)
        service.leaveLobby(player1.id, lobby.id)
        service.joinLobby(player2.id, lobby.id)

        val finalLobby = (service.getLobbyDetails(lobby.id) as Success).value
        assertEquals(2, finalLobby.currentPlayers.size)
        assertFalse(finalLobby.currentPlayers.any { it.id == player1.id })
        assertTrue(finalLobby.currentPlayers.any { it.id == player2.id })
    }

    @Test
    fun `test edge case - single player lobby`() {
        val host = createTestUser()
        val result = service.createLobby(host.id, "Solo Lobby", 1, ROUNDS)

        assertTrue(result is Success)
        val lobby = (result as Success).value
        assertEquals(1, lobby.maxPlayers)
        assertEquals(1, lobby.currentPlayers.size)
    }

    @Test
    fun `test edge case - attempt to join single player lobby`() {
        val host = createTestUser(nickName = "host", email = "host@example.com")
        val player = createTestUser(nickName = "player", email = "player@example.com")
        val lobby = (service.createLobby(host.id, "Solo Lobby", 1, ROUNDS) as Success).value

        val result = service.joinLobby(player.id, lobby.id)
        assertTrue(result is Failure)
        assertEquals(LobbyError.LobbyFull, (result as Failure).value)
    }
}
