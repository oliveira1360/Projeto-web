import mock.TransactionManagerMem
import org.example.Failure
import org.example.Sha256TokenEncoder
import org.example.Success
import org.example.UserAuthService
import org.example.UserError
import org.example.UsersDomainConfig
import org.example.entity.Balance
import org.example.entity.Email
import org.example.entity.Name
import org.example.entity.Password
import org.example.entity.URL
import org.example.entity.User
import org.example.entity.toMoney
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class Test {

    val dominConfig = UsersDomainConfig(
        tokenSizeInBytes = 100,
        tokenTtl = java.time.Duration.ofMinutes(1),
        tokenRollingTtl = java.time.Duration.ofMinutes(5),
        maxTokensPerUser = 100,
    )

    val userMem = userRepoInMem()
    val lobbyMem = lobbyRepoInMem()
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
        when(result){
            is Success -> {
                assertEquals(result.value, userMem.users.first() )

            }
            else -> fail()
        }
    }

    @Test
    fun testCreateUserDuplicateEmail() {
        val result1 = service.createUser(
            user.name,
            user.nickName,
            user.email,
            user.password,
            user.imageUrl,
        )
        when(result1){
            is Success -> {
                assertEquals(result1.value, userMem.users.first() )

            }
            else -> fail()
        }

        val result2 = service.createUser(
            Name("Another User"),
            Name("another"),
            user.email, // same email
            Password("AnotherPass123!"),
            URL("https://example.com/another_avatar.png"),
        )
        assert(result2 is Failure)
    }
}