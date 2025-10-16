import org.example.Success
import org.example.TransactionManagerMem
import org.example.UserAuthService
import org.example.config.UsersDomainConfig
import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.URL
import org.example.entity.core.toMoney
import org.example.entity.player.User
import org.example.game.RepositoryGameMem
import org.example.lobby.RepositoryLobbyMem
import org.example.token.Sha256TokenEncoder
import org.example.user.RepositoryUserMem
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class UserAuthServiceTest {
    val dominConfig =
        UsersDomainConfig(
            tokenSizeInBytes = 100,
            tokenTtl = Duration.ofMinutes(1),
            tokenRollingTtl = Duration.ofMinutes(5),
            maxTokensPerUser = 100,
        )

    val userMem = RepositoryUserMem()
    val lobbyMem = RepositoryLobbyMem()
    val tokenEncoder = Sha256TokenEncoder()
    val clock = Clock.systemUTC()
    val passwordEncoder = BCryptPasswordEncoder()
    val gameMem = RepositoryGameMem()
    val trx = TransactionManagerMem(userMem, lobbyMem, gameMem)

    val service =
        UserAuthService(
            passwordEncoder = passwordEncoder,
            tokenEncoder = tokenEncoder,
            config = dominConfig,
            trxManager = trx,
            clock = clock,
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
    fun testCreateUser() {
        val result =
            service.createUser(
                user.name,
                user.nickName,
                user.email,
                user.password,
                user.imageUrl,
            )
        when (result) {
            is Success -> {
                assertTrue {
                    userMem.users.find {
                        it.id == result.value.id
                    } != null
                }
            }
            else -> fail()
        }
    }

    @Test
    fun testGetUserByEmail() {
        userMem.createUser(
            Name("Other User"),
            Name("other"),
            Email("email@other.com"),
            Password("OtherPassword123!"),
        )
        val result = service.getUserByEmail(Email("email@other.com"))
        when (result) {
            is Success -> {
                assertEquals(result.value, userMem.users.find { it.id == result.value.id })
            }
            else -> fail()
        }
    }

    @Test
    fun testUpdateUser() {
        val createdUser = userMem.createUser(user.name, user.nickName, user.email, user.password, user.imageUrl)
        val newName = "New Name"
        val newNickName = "newnick"
        val newPassword = "NewPassword123!"
        when (val result = service.updateUser(createdUser.id, newName, newNickName, newPassword, null)) {
            is Success -> {
                val updatedUser = result.value
                assertEquals(updatedUser.name.value, newName)
                assertEquals(updatedUser.nickName.value, newNickName)
            }
            else -> fail()
        }
    }

    @Test
    fun testTokenOperations() {
        val user = service.getUserByEmail(user.email)
        val createdUser =
            when (user) {
                is Success -> user.value
                else -> fail("User creation failed")
            }
        val token = service.createToken(createdUser.email.value, createdUser.password.value)
        val createdToken =
            when (token) {
                is Success -> token.value
                else -> fail("Token creation failed")
            }
        assertEquals(createdUser, service.getUserByToken(createdToken.tokenValue))
        assertTrue(service.revokeToken(createdToken.tokenValue))
    }
}
