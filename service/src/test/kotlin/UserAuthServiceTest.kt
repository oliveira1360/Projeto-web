import org.example.Either
import org.example.Failure
import org.example.Success
import org.example.TokenCreationError
import org.example.TransactionManagerMem
import org.example.UserAuthService
import org.example.UserError
import org.example.config.UsersDomainConfig
import org.example.entity.core.Balance
import org.example.entity.core.Email
import org.example.entity.core.Name
import org.example.entity.core.Password
import org.example.entity.core.URL
import org.example.entity.core.toMoney
import org.example.entity.player.User
import org.example.game.RepositoryGameMem
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.token.Sha256TokenEncoder
import org.example.user.RepositoryUserMem
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import java.time.Duration
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UserAuthServiceTest {
    private val domainConfig =
        UsersDomainConfig(
            tokenSizeInBytes = 100,
            tokenTtl = Duration.ofMinutes(30),
            tokenRollingTtl = Duration.ofMinutes(60),
            maxTokensPerUser = 3,
        )

    private val userMem = RepositoryUserMem()
    private val lobbyMem = RepositoryLobbyMem()
    private val tokenEncoder = Sha256TokenEncoder()
    private val clock = Clock.systemUTC()
    private val passwordEncoder = BCryptPasswordEncoder()
    private val gameMem = RepositoryGameMem()
    private val generalMem = RepositoryInviteMem()
    private val trx = TransactionManagerMem(userMem, lobbyMem, gameMem, generalMem)
    private val service =
        UserAuthService(
            passwordEncoder = passwordEncoder,
            tokenEncoder = tokenEncoder,
            config = domainConfig,
            trxManager = trx,
            clock = clock,
        )

    private var counter = 0

    private fun createTestUserData(
        name: String = "Test User",
        nickName: String = "testuser",
        email: String = "test@example.com",
        password: String = "SuperSecret123!",
    ): Pair<User, String> {
        val uniqueEmail = "${counter++}$email"
        val result =
            service.createUser(
                Name(name),
                Name(nickName),
                Email(uniqueEmail),
                Password(password),
                URL("https://example.com/avatar.png"),
            )
        return when (result) {
            is Success -> Pair(result.value, password)
            else -> throw IllegalStateException("Failed to create test user")
        }
    }

    @BeforeTest
    fun setup() {
        userMem.clear()
        lobbyMem.clear()
        counter = 0
    }

    // ============================================
    // BASIC TESTS - Create User
    // ============================================

    @Test
    fun `test create user with valid data`() {
        val result =
            service.createUser(
                Name("John Doe"),
                Name("johndoe"),
                Email("john@example.com"),
                Password("SecurePass123!"),
                URL("https://example.com/avatar.png"),
            )

        assertTrue(result is Success)
        val user = (result as Success).value
        assertEquals("John Doe", user.name.value)
        assertEquals("johndoe", user.nickName.value)
        assertEquals("john@example.com", user.email.value)
        assertNotNull(user.imageUrl)
        assertEquals(0, user.balance.money.value)
    }

    @Test
    fun `test create user without image URL`() {
        val result =
            service.createUser(
                Name("Jane Doe"),
                Name("janedoe"),
                Email("jane@example.com"),
                Password("SecurePass123!"),
                null,
            )

        assertTrue(result is Success)
        val user = (result as Success).value
        assertNull(user.imageUrl)
    }

    @Test
    fun `test create multiple users with unique emails`() {
        val user1 = createTestUserData(email = "user1@example.com")
        val user2 = createTestUserData(email = "user2@example.com")
        val user3 = createTestUserData(email = "user3@example.com")

        assertEquals(3, userMem.users.size)
        assertTrue(userMem.users.any { it.email.value.contains("user1@example.com") })
        assertTrue(userMem.users.any { it.email.value.contains("user2@example.com") })
        assertTrue(userMem.users.any { it.email.value.contains("user3@example.com") })
    }

    // ============================================
    // BASIC TESTS - Get User
    // ============================================

    @Test
    fun `test get user by email successfully`() {
        val (createdUser, _) = createTestUserData(email = "find@example.com")

        val result = service.getUserByEmail(createdUser.email)
        assertTrue(result is Success)
        val foundUser = (result as Success).value
        assertEquals(createdUser.id, foundUser.id)
        assertEquals(createdUser.email, foundUser.email)
    }

    @Test
    fun `test get user by non-existent email fails`() {
        val result = service.getUserByEmail(Email("nonexistent@example.com"))
        assertTrue(result is Failure)
        assertEquals(UserError.InvalidCredentials, (result as Failure).value)
    }

    @Test
    fun `test get user by email returns correct user among multiple`() {
        createTestUserData(name = "User One", email = "user1@example.com")
        val (targetUser, _) = createTestUserData(name = "User Two", email = "user2@example.com")
        createTestUserData(name = "User Three", email = "user3@example.com")

        val result = service.getUserByEmail(targetUser.email)
        assertTrue(result is Success)
        val foundUser = (result as Success).value
        assertEquals("User Two", foundUser.name.value)
        assertEquals(targetUser.id, foundUser.id)
    }

    // ============================================
    // INTERMEDIATE TESTS - Create Token (Login)
    // ============================================

    @Test
    fun `test create token with valid credentials`() {
        val (user, password) = createTestUserData(email = "login@example.com")

        val result = service.createToken(user.email.value, password)
        assertTrue(result is Either.Right)
        val tokenInfo = (result as Either.Right).value
        assertNotNull(tokenInfo.tokenValue)
        assertNotNull(tokenInfo.tokenExpiration)
    }

    @Test
    fun `test create token with invalid email fails`() {
        val result = service.createToken("invalid@example.com", "SomePassword123!")
        assertTrue(result is Either.Left)
        assertEquals(TokenCreationError.UserOrPasswordAreInvalid, (result as Either.Left).value)
    }


    @Test
    fun `test create token with blank email fails`() {
        val result = service.createToken("", "SomePassword123!")
        assertTrue(result is Either.Left)
        assertEquals(TokenCreationError.UserOrPasswordAreInvalid, (result as Either.Left).value)
    }

    @Test
    fun `test create token with blank password fails`() {
        val (user, _) = createTestUserData()

        val result = service.createToken(user.email.value, "")
        assertTrue(result is Either.Left)
        assertEquals(TokenCreationError.UserOrPasswordAreInvalid, (result as Either.Left).value)
    }

    @Test
    fun `test create token with whitespace credentials fails`() {
        val result1 = service.createToken("   ", "password")
        val result2 = service.createToken("email@test.com", "   ")

        assertTrue(result1 is Either.Left)
        assertTrue(result2 is Either.Left)
    }

    @Test
    fun `test create multiple tokens for same user`() {
        val (user, password) = createTestUserData()

        val token1 = service.createToken(user.email.value, password)
        val token2 = service.createToken(user.email.value, password)
        val token3 = service.createToken(user.email.value, password)

        assertTrue(token1 is Either.Right)
        assertTrue(token2 is Either.Right)
        assertTrue(token3 is Either.Right)
        assertEquals(3, RepositoryUserMem.tokens.size)
    }

    @Test
    fun `test create token respects max tokens per user limit`() {
        val (user, password) = createTestUserData()

        // Criar tokens até o limite máximo (3)
        val token1 = service.createToken(user.email.value, password)
        val token2 = service.createToken(user.email.value, password)
        val token3 = service.createToken(user.email.value, password)

        assertEquals(3, RepositoryUserMem.tokens.filter { it.userId == user.id }.size)

        // Criar mais um token deve remover o mais antigo
        val token4 = service.createToken(user.email.value, password)

        assertTrue(token4 is Either.Right)
        assertEquals(3, RepositoryUserMem.tokens.filter { it.userId == user.id }.size)
    }

    // ============================================
    // INTERMEDIATE TESTS - Get User By Token
    // ============================================

    @Test
    fun `test get user by valid token`() {
        val (user, password) = createTestUserData()
        val tokenResult = service.createToken(user.email.value, password)
        val token = (tokenResult as Either.Right).value.tokenValue

        val foundUser = service.getUserByToken(token)
        assertNotNull(foundUser)
        assertEquals(user.id, foundUser.id)
        assertEquals(user.email, foundUser.email)
    }

    @Test
    fun `test get user by invalid token returns null`() {
        val foundUser = service.getUserByToken("invalid-token-string")
        assertNull(foundUser)
    }

    @Test
    fun `test get user by token updates last used time`() {
        val (user, password) = createTestUserData()
        val tokenResult = service.createToken(user.email.value, password)
        val tokenValue = (tokenResult as Either.Right).value.tokenValue

        val tokenBefore = RepositoryUserMem.tokens.find {
            it.tokenValidationInfo == tokenEncoder.createValidationInformation(tokenValue)
        }
        assertNotNull(tokenBefore)
        val lastUsedBefore = tokenBefore.lastUsedAt

        Thread.sleep(100) // Pequena pausa para garantir diferença de tempo

        service.getUserByToken(tokenValue)

        val tokenAfter = RepositoryUserMem.tokens.find {
            it.tokenValidationInfo == tokenEncoder.createValidationInformation(tokenValue)
        }
        assertNotNull(tokenAfter)
        assertTrue(tokenAfter.lastUsedAt > lastUsedBefore)
    }

    // ============================================
    // INTERMEDIATE TESTS - Revoke Token (Logout)
    // ============================================

    @Test
    fun `test revoke token successfully`() {
        val (user, password) = createTestUserData()
        val tokenResult = service.createToken(user.email.value, password)
        val token = (tokenResult as Either.Right).value.tokenValue

        val result = service.revokeToken(token)
        assertTrue(result)

        // Verificar que o token foi removido
        val foundUser = service.getUserByToken(token)
        assertNull(foundUser)
    }

    @Test
    fun `test revoke invalid token returns true`() {
        val result = service.revokeToken("invalid-token")
        assertTrue(result)
    }

    @Test
    fun `test revoke token twice`() {
        val (user, password) = createTestUserData()
        val tokenResult = service.createToken(user.email.value, password)
        val token = (tokenResult as Either.Right).value.tokenValue

        assertTrue(service.revokeToken(token))
        assertTrue(service.revokeToken(token)) // Segunda revogação também deve retornar true
    }

    @Test
    fun `test revoke one token doesnt affect others`() {
        val (user, password) = createTestUserData()
        val token1Result = service.createToken(user.email.value, password)
        val token2Result = service.createToken(user.email.value, password)
        val token1 = (token1Result as Either.Right).value.tokenValue
        val token2 = (token2Result as Either.Right).value.tokenValue

        service.revokeToken(token1)

        assertNull(service.getUserByToken(token1))
        assertNotNull(service.getUserByToken(token2))
    }

    // ============================================
    // INTERMEDIATE TESTS - Update User
    // ============================================

    @Test
    fun `test update user name successfully`() {
        val (user, _) = createTestUserData()
        val newName = "Updated Name"

        val result = service.updateUser(user.id, newName, null, null, null)
        assertTrue(result is Success)
        val updatedUser = (result as Success).value
        assertEquals(newName, updatedUser.name.value)
        assertEquals(user.nickName.value, updatedUser.nickName.value) // Outros campos inalterados
    }

    @Test
    fun `test update user nickname successfully`() {
        val (user, _) = createTestUserData()
        val newNickName = "newnick"

        val result = service.updateUser(user.id, null, newNickName, null, null)
        assertTrue(result is Success)
        val updatedUser = (result as Success).value
        assertEquals(newNickName, updatedUser.nickName.value)
    }


    @Test
    fun `test update user image URL successfully`() {
        val (user, _) = createTestUserData()
        val newImageUrl = "https://example.com/new-avatar.png"

        val result = service.updateUser(user.id, null, null, null, newImageUrl)
        assertTrue(result is Success)
        val updatedUser = (result as Success).value
        assertEquals(newImageUrl, updatedUser.imageUrl?.value)
    }

    @Test
    fun `test update user with null values keeps existing data`() {
        val (user, _) = createTestUserData(name = "Original Name", nickName = "originalnick")

        val result = service.updateUser(user.id, null, null, null, null)
        assertTrue(result is Success)
        val updatedUser = (result as Success).value
        assertEquals("Original Name", updatedUser.name.value)
        assertEquals("originalnick", updatedUser.nickName.value)
    }

    // ============================================
    // ADVANCED TESTS - User States
    // ============================================

    @Test
    fun `test get user states by valid user id`() {
        val (user, _) = createTestUserData()

        val result = service.userStates(user.id)
        assertTrue(result is Success)
        val foundUser = (result as Success).value
        assertEquals(user.id, foundUser.id)
    }

    @Test
    fun `test get user states by invalid user id fails`() {
        val result = service.userStates(999)
        assertTrue(result is Failure)
        assertEquals(UserError.InvalidCredentials, (result as Failure).value)
    }

    // ============================================
    // ADVANCED TESTS - Complex Scenarios
    // ============================================

    @Test
    fun `test full user lifecycle - create, login, update, logout`() {
        // Criar usuário
        val originalPassword = "OriginalPass123!"
        val createResult =
            service.createUser(
                Name("Lifecycle User"),
                Name("lifecycleuser"),
                Email("lifecycle@example.com"),
                Password(originalPassword),
                null,
            )
        assertTrue(createResult is Success)
        val user = (createResult as Success).value

        // Login (criar token)
        val tokenResult = service.createToken(user.email.value, originalPassword)
        assertTrue(tokenResult is Either.Right)
        val token = (tokenResult as Either.Right).value.tokenValue

        // Atualizar usuário
        val newName = "Updated Lifecycle User"
        val updateResult = service.updateUser(user.id, newName, null, null, null)
        assertTrue(updateResult is Success)
        val updatedUser = (updateResult as Success).value
        assertEquals(newName, updatedUser.name.value)

        // Logout (revogar token)
        assertTrue(service.revokeToken(token))
        assertNull(service.getUserByToken(token))
    }

    @Test
    fun `test user can login again after token revocation`() {
        val (user, password) = createTestUserData()

        // Primeiro login
        val token1Result = service.createToken(user.email.value, password)
        val token1 = (token1Result as Either.Right).value.tokenValue

        // Logout
        service.revokeToken(token1)

        // Segundo login
        val token2Result = service.createToken(user.email.value, password)
        assertTrue(token2Result is Either.Right)
        val token2 = (token2Result as Either.Right).value.tokenValue

        assertNotNull(service.getUserByToken(token2))
    }

    @Test
    fun `test update password and login with new password`() {
        val (user, oldPassword) = createTestUserData()
        val newPassword = "NewSecurePass456!"

        // Atualizar senha
        val updateResult = service.updateUser(user.id, null, null, newPassword, null)
        assertTrue(updateResult is Success)

        // Login com senha antiga deve falhar
        val oldLoginResult = service.createToken(user.email.value, oldPassword)
        assertTrue(oldLoginResult is Either.Left)

        // Login com senha nova deve funcionar
        val newLoginResult = service.createToken(user.email.value, newPassword)
        assertTrue(newLoginResult is Either.Right)
    }

    @Test
    fun `test multiple users with different tokens dont interfere`() {
        val (user1, password1) = createTestUserData(email = "user1@example.com")
        val (user2, password2) = createTestUserData(email = "user2@example.com")

        val token1Result = service.createToken(user1.email.value, password1)
        val token2Result = service.createToken(user2.email.value, password2)

        val token1 = (token1Result as Either.Right).value.tokenValue
        val token2 = (token2Result as Either.Right).value.tokenValue

        // Revogar token do user1
        service.revokeToken(token1)

        // Token do user2 deve ainda funcionar
        assertNull(service.getUserByToken(token1))
        assertNotNull(service.getUserByToken(token2))
    }

    @Test
    fun `test concurrent token creation for different users`() {
        val users =
            (1..5).map { i ->
                createTestUserData(
                    name = "User $i",
                    email = "user$i@example.com",
                    password = "Password$i!",
                )
            }

        val tokens =
            users.map { (user, password) ->
                service.createToken(user.email.value, password)
            }

        // Todos os tokens devem ter sido criados com sucesso
        assertTrue(tokens.all { it is Either.Right })
        assertEquals(5, RepositoryUserMem.tokens.size)

        // Cada token deve retornar o usuário correto
        tokens.forEachIndexed { index, tokenResult ->
            val token = (tokenResult as Either.Right).value.tokenValue
            val foundUser = service.getUserByToken(token)
            assertNotNull(foundUser)
            assertEquals(users[index].first.id, foundUser.id)
        }
    }
}