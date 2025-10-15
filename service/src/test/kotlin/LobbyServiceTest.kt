import org.example.LobbiesDomainConfig
import org.example.LobbyService
import org.example.Success
import org.example.TransactionManagerMem
import org.example.entity.Balance
import org.example.entity.Email
import org.example.entity.Lobby
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.example.entity.toMoney
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import kotlin.test.Test
import kotlin.test.assertTrue

const val MAX_PLAYERS = 4
const val INVITE_CODE_LENGTH = 4
const val MAX_PER_USER = 3
const val ROUNDS = 16

class LobbyServiceTest {
    val dominConfig =
        LobbiesDomainConfig(
            MAX_PLAYERS,
            INVITE_CODE_LENGTH,
            MAX_PER_USER,
        )

    val userMem: RepositoryUserMem
        get() = RepositoryLobbyMem.userRepo
    val lobbyMem = RepositoryLobbyMem()
    val trx = TransactionManagerMem(userMem, lobbyMem)

    val service =
        LobbyService(
            trx,
            dominConfig,
        )

    val user =
        User(
            id = 1,
            name = Name("Diogo Oliveira"),
            nickName = Name("diogo"),
            imageUrl = URL("https://example.com/avatar.png"),
            email = Email("diogo.oliveira@example.com"),
            password = Password("SuperSecret123!"),
            balance = Balance(500.toMoney()),
        )

    @Test
    fun testCreateLobby() {
        val createdUser = userMem.createUser(user.name, user.nickName, user.email, user.password, user.imageUrl)
        val result = service.createLobby(createdUser.id, "DEFAULT", MAX_PLAYERS, ROUNDS)
        var lobby: Lobby
        if (result is Success<Lobby>) {
            lobby = result.value
        } else {
            error("Expected Success<Lobby>")
        }
        assert(lobby.id == 1)
        assert(lobby.hostId == createdUser.id)
        assert(lobby.maxPlayers == MAX_PLAYERS)
        assert(lobby.currentPlayers.size < MAX_PER_USER)
        assert(lobby.name.value == "DEFAULT")
    }

    @Test
    fun testJoinLobby() {
        val createdUser = userMem.createUser(user.name, user.nickName, Email("other@email.ext"), user.password, user.imageUrl)
        val resultUpdate = service.joinLobby(createdUser.id, 1)
        var lobby: Lobby
        if (resultUpdate is Success<Lobby>) {
            lobby = resultUpdate.value
        } else {
            error("Expected Success<Lobby>")
        }
        assert(lobby.id == 1)
        assert(lobby.hostId == 1)
        assert(lobby.maxPlayers == MAX_PLAYERS)
        assert(lobby.currentPlayers.size < MAX_PLAYERS)
    }

    @Test
    fun testleaveLobby() {
        val user = userMem.findById(2) ?: error("User not found")
        val resultUpdate = service.leaveLobby(user.id, 1)
        var lobby: Lobby
        if (resultUpdate is Success<Lobby>) {
            lobby = resultUpdate.value
        } else {
            error("Expected Success<Lobby>")
        }
        assert(lobby.id == 1)
        assert(lobby.hostId == 1)
        assertTrue {
            (service.getLobbyDetails(1) as Success<Lobby>).let {
                it.value.currentPlayers.none { u ->
                    u.id == user.id
                }
            }
        }
    }
}
