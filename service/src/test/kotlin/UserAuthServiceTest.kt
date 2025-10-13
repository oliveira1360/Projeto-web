import org.example.Sha256TokenEncoder
import org.example.Success
import org.example.TransactionManagerMem
import org.example.UserAuthService
import org.example.UsersDomainConfig
import org.example.entity.Balance
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.example.entity.toMoney
import org.example.lobby.RepositoryLobbyMem
import org.example.user.RepositoryUserMem
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.test.Test

class UserAuthServiceTest {
    val dominConfig = UsersDomainConfig(
        tokenSizeInBytes = 100,
        tokenTtl = java.time.Duration.ofMinutes(1),
        tokenRollingTtl = java.time.Duration.ofMinutes(5),
        maxTokensPerUser = 100,
    )

    val userMem = RepositoryUserMem()
    val lobbyMem = RepositoryLobbyMem()
    val tokenEncoder = Sha256TokenEncoder()
    val clock = Clock.systemUTC()
    val passwordEncoder = BCryptPasswordEncoder()
    val trx = TransactionManagerMem(userMem, lobbyMem)

    val service = UserAuthService(
        passwordEncoder = passwordEncoder,
        tokenEncoder = tokenEncoder,
        config = dominConfig,
        trxManager = trx,
        clock = clock,

        )

    val user = User(
        id = 1,
        name = Name("Diogo Oliveira"),
        nickName = Name("diogo"),
        imageUrl = URL("https://example.com/avatar.png"),
        email = Email("diogo.oliveira@example.com"),
        password = Password("SuperSecret123!"),
        balance = Balance(500.toMoney())
    )

    @Test
    fun testCreateUser() {
        val result = service.createUser(
            user.name,
            user.nickName,
            user.email,
            user.password,
            user.imageUrl,
        )
        when (result) {
            is Success -> {
                assertEquals(result.value, userMem.users.first())

            }
            else -> fail()
        }
    }
}