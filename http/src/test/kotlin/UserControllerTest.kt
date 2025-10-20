@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example.controllers

import org.example.TransactionManager
import org.example.TransactionManagerMem
import org.example.UserAuthService
import org.example.config.UsersDomainConfig
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateUserDTO
import org.example.dto.inputDto.LoginUserDTO
import org.example.dto.inputDto.UpdateUserDTO
import org.example.dto.inputDto.ValidInviteDTO
import org.example.entity.core.*
import org.example.entity.player.User
import org.example.game.RepositoryGameMem
import org.example.general.RepositoryInviteMem
import org.example.lobby.RepositoryLobbyMem
import org.example.token.Sha256TokenEncoder
import org.example.user.RepositoryUserMem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserControllerTest {
    private val usersDomainConfig =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )

    private val userMem = RepositoryUserMem()
    private val lobbyMem = RepositoryLobbyMem()
    private val gameMem = RepositoryGameMem()
    private val generalMem = RepositoryInviteMem()
    private val trxManager: TransactionManager = TransactionManagerMem(userMem, lobbyMem, gameMem, generalMem)

    private val passwordEncoder = BCryptPasswordEncoder()
    private val tokenEncoder = Sha256TokenEncoder()
    private val clock = Clock.systemUTC()

    private val userAuthService = UserAuthService(passwordEncoder, tokenEncoder, usersDomainConfig, trxManager, clock)
    private val userController = UserController(userAuthService)

    val validInvite =
        ValidInviteDTO(
            id = 1,
            token = "ABC123XYZ",
            createdAt = Instant.parse("2025-10-18T12:00:00Z"),
            expiresAt = Instant.parse("2025-11-18T12:00:00Z"),
            used = false,
        )
    private var userCounter = 0

    private fun createTestUser(
        name: String = "Test User",
        nickName: String = "testuser",
        email: String = "test@example.com",
        password: String = "SecurePass123!",
    ): User {
        val uniqueEmail = "${userCounter++}$email"
        return trxManager.run {
            repositoryUser.createUser(
                name = Name(name),
                nickName = Name("$nickName$userCounter"),
                email = Email(uniqueEmail),
                password = Password(password),
                imageUrl = URL("https://example.com/avatar.png"),
            )
        }
    }

    @BeforeEach
    fun cleanup() {
        trxManager.run {
            repositoryUser.clear()
        }
        userCounter = 0
    }

    // ============================================
    // EASY TESTS - User Creation
    // ============================================

    @Test
    fun `createUser should return CREATED with valid data`() {
        // given: valid user input
        val input =
            CreateUserDTO(
                name = "John Doe",
                nickName = "johndoe",
                email = "john@example.com",
                password = "SecurePass123!",
                imageUrl = "https://example.com/john.png",
            )

        // when: creating user
        val resp = userController.createUser(validInvite, input)

        // then: response is 201 with user details
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["userId"])
        assertEquals("John Doe", body["name"])
        assertEquals("johndoe", body["nickName"])
        assertEquals("john@example.com", body["email"])
        assertEquals(0, body["balance"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `createUser should succeed with minimum required fields`() {
        // given: user with no image URL
        val input =
            CreateUserDTO(
                name = "Jane",
                nickName = "jane",
                email = "jane@example.com",
                password = "Pass1234!",
                imageUrl = null,
            )

        // when: creating user
        val resp = userController.createUser(validInvite, input)

        // then: response is 201
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("Jane", body["name"])
    }

    @Test
    fun `createUser should create users with different emails`() {
        // given: multiple users with different emails
        val user1 = CreateUserDTO("User1", "nick1", "user1@example.com", "Pass123!", null)
        val user2 = CreateUserDTO("User2", "nick2", "user2@example.com", "Pass123!", null)

        // when: creating both users
        val resp1 = userController.createUser(validInvite, user1)
        val resp2 = userController.createUser(validInvite, user2)

        // then: both are created successfully
        assertEquals(HttpStatus.CREATED, resp1.statusCode)
        assertEquals(HttpStatus.CREATED, resp2.statusCode)
    }

    @Test
    fun `createUser should initialize balance to zero`() {
        // given: a new user
        val input = CreateUserDTO("User", "user", "user@test.com", "Pass123!", null)

        // when: creating user
        val resp = userController.createUser(validInvite, input)

        // then: balance is 0
        val body = resp.body as Map<*, *>
        assertEquals(0, body["balance"])
    }

    // ============================================
    // EASY TESTS - Login
    // ============================================

    @Test
    fun `loginUser should return token with valid credentials`() {
        // given: an existing user
        val password = "SecurePass123!"
        createTestUser(email = "login@example.com", password = password)

        val loginInput =
            LoginUserDTO(
                email = "0login@example.com",
                password = password,
            )

        // when: logging in
        val resp = userController.loginUser(loginInput)

        // then: response is 202 with token
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertNotNull(body["token"])
        assertNotNull(body["expiresAt"])
        assertEquals("Login successful", body["message"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `loginUser should return 401 with invalid email`() {
        // given: no user exists
        val loginInput =
            LoginUserDTO(
                email = "nonexistent@example.com",
                password = "SomePass123!",
            )

        // when: attempting to login
        val resp = userController.loginUser(loginInput)

        // then: response is 401
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        val body = resp.body as ProblemDetail
        assertEquals(ProblemTypes.INVALID_CREDENTIALS, body.type)
    }

    @Test
    fun `loginUser should return 401 with blank email`() {
        // given: blank email
        val loginInput = LoginUserDTO(email = "", password = "Pass123!")

        // when: attempting to login
        val resp = userController.loginUser(loginInput)

        // then: response is 401
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `loginUser should return 401 with blank password`() {
        // given: blank password
        val loginInput = LoginUserDTO(email = "user@example.com", password = "")

        // when: attempting to login
        val resp = userController.loginUser(loginInput)

        // then: response is 401
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Get User Info
    // ============================================

    @Test
    fun `getUserInfo should return user details`() {
        // given: a user
        val user =
            createTestUser(
                name = "Info User",
                nickName = "infouser",
                email = "info@example.com",
            )

        // when: getting user info
        val resp = userController.getUserInfo(AuthenticatedUserDto(user, "token123"))

        // then: response is 200 with user info
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(user.id, body["userId"])
        assertEquals("Info User", body["name"])
        assertEquals(user.nickName.value, body["nickName"])
        assertEquals(user.email.value, body["email"])
        assertEquals(0, body["balance"])
        assertNotNull(body["imageUrl"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `getUserInfo should return correct balance`() {
        // given: a user
        val user = createTestUser()

        // when: getting user info
        val resp = userController.getUserInfo(AuthenticatedUserDto(user, "token"))

        // then: balance is included
        val body = resp.body as Map<*, *>
        assertNotNull(body["balance"])
    }

    // ============================================
    // INTERMEDIATE TESTS - Update User
    // ============================================

    @Test
    fun `updateUser should update name`() {
        // given: a user
        val user = createTestUser(name = "Old Name")
        val updateInput =
            UpdateUserDTO(
                name = "New Name",
                nickName = null,
                password = null,
                imageUrl = null,
            )

        // when: updating user
        val resp = userController.updateUser(AuthenticatedUserDto(user, "token"), updateInput)

        // then: response is 202 with updated data
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("New Name", body["name"])
        assertEquals("User updated successfully", body["message"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `updateUser should update nickName`() {
        // given: a user
        val user = createTestUser(nickName = "oldnick")
        val updateInput =
            UpdateUserDTO(
                name = null,
                nickName = "newnick",
                password = null,
                imageUrl = null,
            )

        // when: updating user
        val resp = userController.updateUser(AuthenticatedUserDto(user, "token"), updateInput)

        // then: nickname is updated
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("newnick", body["nickName"])
    }

    @Test
    fun `updateUser should update password`() {
        // given: a user
        val user = createTestUser()
        val updateInput =
            UpdateUserDTO(
                name = null,
                nickName = null,
                password = "NewSecurePass123!",
                imageUrl = null,
            )

        // when: updating user
        val resp = userController.updateUser(AuthenticatedUserDto(user, "token"), updateInput)

        // then: response is successful
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
    }

    @Test
    fun `updateUser should update multiple fields`() {
        // given: a user
        val user = createTestUser(name = "Old", nickName = "old")
        val updateInput =
            UpdateUserDTO(
                name = "New Name",
                nickName = "newnick",
                password = "NewPass123!",
                imageUrl = "https://example.com/new.png",
            )

        // when: updating multiple fields
        val resp = userController.updateUser(AuthenticatedUserDto(user, "token"), updateInput)

        // then: all fields are updated
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("New Name", body["name"])
        assertEquals("newnick", body["nickName"])
    }

    @Test
    fun `updateUser with no changes should succeed`() {
        // given: a user
        val user = createTestUser()
        val updateInput =
            UpdateUserDTO(
                name = null,
                nickName = null,
                password = null,
                imageUrl = null,
            )

        // when: updating with no changes
        val resp = userController.updateUser(AuthenticatedUserDto(user, "token"), updateInput)

        // then: response is successful
        assertEquals(HttpStatus.ACCEPTED, resp.statusCode)
    }

    // ============================================
    // INTERMEDIATE TESTS - Logout
    // ============================================

    @Test
    fun `logoutUser should revoke token`() {
        // given: a logged in user
        val user = createTestUser(email = "logout@example.com")
        val loginResp =
            userController.loginUser(
                LoginUserDTO("0logout@example.com", "SecurePass123!"),
            )
        val loginBody = loginResp.body as Map<*, *>
        val token = loginBody["token"] as String

        // when: logging out
        val resp = userController.logoutUser(AuthenticatedUserDto(user, token))

        // then: response is 200 with success message
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals("Logout successful", body["message"])
        assertNotNull(body["_links"])
    }

    @Test
    fun `logoutUser should allow multiple logouts`() {
        // given: a user
        val user = createTestUser()

        // when: logging out multiple times
        val resp1 = userController.logoutUser(AuthenticatedUserDto(user, "token1"))
        val resp2 = userController.logoutUser(AuthenticatedUserDto(user, "token2"))

        // then: both succeed
        assertEquals(HttpStatus.OK, resp1.statusCode)
        assertEquals(HttpStatus.OK, resp2.statusCode)
    }

    // ============================================
    // ADVANCED TESTS - Complex Scenarios
    // ============================================

    @Test
    fun `user can login and logout multiple times`() {
        // given: a user
        val password = "Pass123!"
        val user = createTestUser(email = "multi@example.com", password = password)

        // when: login -> logout -> login again
        val login1 = userController.loginUser(LoginUserDTO("0multi@example.com", password))
        assertEquals(HttpStatus.ACCEPTED, login1.statusCode)

        userController.logoutUser(AuthenticatedUserDto(user, "token1"))

        val login2 = userController.loginUser(LoginUserDTO("0multi@example.com", password))
        assertEquals(HttpStatus.ACCEPTED, login2.statusCode)

        // then: both logins succeed
        assertTrue(true)
    }

    @Test
    fun `user can update profile after creation`() {
        // given: a newly created user
        val createInput = CreateUserDTO("Original", "original", "update@example.com", "Pass123!", null)
        val createResp = userController.createUser(validInvite, createInput)
        val createBody = createResp.body as Map<*, *>
        val userId = createBody["userId"] as Int

        val user = trxManager.run { repositoryUser.findById(userId)!! }

        // when: updating the profile
        val updateInput = UpdateUserDTO("Updated Name", "updated", null, null)
        val updateResp = userController.updateUser(AuthenticatedUserDto(user, "token"), updateInput)

        // then: update succeeds
        assertEquals(HttpStatus.ACCEPTED, updateResp.statusCode)
        val updateBody = updateResp.body as Map<*, *>
        assertEquals("Updated Name", updateBody["name"])
    }

    @Test
    fun `getUserInfo returns updated information after update`() {
        // given: a user that was updated
        val user = createTestUser(name = "Before")
        userController.updateUser(
            AuthenticatedUserDto(user, "token"),
            UpdateUserDTO("After", null, null, null),
        )

        // when: getting user info
        val updatedUser = trxManager.run { repositoryUser.findById(user.id)!! }
        val resp = userController.getUserInfo(AuthenticatedUserDto(updatedUser, "token"))

        // then: info reflects the update
        val body = resp.body as Map<*, *>
        assertEquals("After", body["name"])
    }

    @Test
    fun `multiple users can be created and managed independently`() {
        // given: multiple users
        val user1 = createTestUser(name = "User1", email = "user1@example.com")
        val user2 = createTestUser(name = "User2", email = "user2@example.com")

        // when: getting info for each
        val resp1 = userController.getUserInfo(AuthenticatedUserDto(user1, "token1"))
        val resp2 = userController.getUserInfo(AuthenticatedUserDto(user2, "token2"))

        // then: each returns correct data
        val body1 = resp1.body as Map<*, *>
        val body2 = resp2.body as Map<*, *>

        assertEquals("User1", body1["name"])
        assertEquals("User2", body2["name"])
        assertTrue(body1["userId"] != body2["userId"])
    }

    @Test
    fun `user creation validates unique emails`() {
        // given: a user with an email
        val email = "duplicate@example.com"
        val input1 = CreateUserDTO("User1", "nick1", email, "Pass123!", null)
        userController.createUser(validInvite, input1)

        // when: trying to create another user with same email
        val input2 = CreateUserDTO("User2", "nick2", email, "Pass123!", null)

        // then: creation fails with exception
        val exception =
            assertThrows<IllegalArgumentException> {
                userController.createUser(validInvite, input2)
            }
        assertEquals(
            "User with email Email(value=$email) already exists",
            exception.message,
        )
    }

    @Test
    fun `complete user workflow - create, login, update, logout`() {
        // given: complete user workflow
        val password = "Workflow123!"

        // when: creating user
        val createResp =
            userController.createUser(
                validInvite,
                CreateUserDTO("Workflow User", "workflow", "workflow@example.com", password, null),
            )
        assertEquals(HttpStatus.CREATED, createResp.statusCode)

        // when: logging in
        val loginResp = userController.loginUser(LoginUserDTO("workflow@example.com", password))
        assertEquals(HttpStatus.ACCEPTED, loginResp.statusCode)
        val loginBody = loginResp.body as Map<*, *>
        val token = loginBody["token"] as String

        // when: getting user to update
        val userId = (createResp.body as Map<*, *>)["userId"] as Int
        val user = trxManager.run { repositoryUser.findById(userId)!! }

        // when: updating user
        val updateResp =
            userController.updateUser(
                AuthenticatedUserDto(user, token),
                UpdateUserDTO("Updated Workflow", null, null, null),
            )
        assertEquals(HttpStatus.ACCEPTED, updateResp.statusCode)

        // when: logging out
        val logoutResp = userController.logoutUser(AuthenticatedUserDto(user, token))
        assertEquals(HttpStatus.OK, logoutResp.statusCode)

        // then: entire workflow succeeds
        assertTrue(true)
    }

    @Test
    fun `HATEOAS links are present in all successful user responses`() {
        // given: a user
        val user = createTestUser(email = "links@example.com")

        // when: create user
        val createResp =
            userController.createUser(
                validInvite,
                CreateUserDTO("Links User", "links", "links2@example.com", "Pass123!", null),
            )
        val createBody = createResp.body as Map<*, *>
        assertNotNull(createBody["_links"], "Create user should have _links")

        // when: login
        val loginResp = userController.loginUser(LoginUserDTO("0links@example.com", "SecurePass123!"))
        val loginBody = loginResp.body as Map<*, *>
        assertNotNull(loginBody["_links"], "Login should have _links")

        // when: get user info
        val infoResp = userController.getUserInfo(AuthenticatedUserDto(user, "token"))
        val infoBody = infoResp.body as Map<*, *>
        assertNotNull(infoBody["_links"], "User info should have _links")

        // when: update user
        val updateResp =
            userController.updateUser(
                AuthenticatedUserDto(user, "token"),
                UpdateUserDTO("New", null, null, null),
            )
        val updateBody = updateResp.body as Map<*, *>
        assertNotNull(updateBody["_links"], "Update user should have _links")

        // when: logout
        val logoutResp = userController.logoutUser(AuthenticatedUserDto(user, "token"))
        val logoutBody = logoutResp.body as Map<*, *>
        assertNotNull(logoutBody["_links"], "Logout should have _links")
    }

    @Test
    fun `stress test create multiple users rapidly`() {
        // given: multiple user creation requests
        val users =
            (1..10).map { i ->
                CreateUserDTO(
                    name = "Stress User $i",
                    nickName = "stress$i",
                    email = "stress$i@example.com",
                    password = "Pass123!",
                    imageUrl = null,
                )
            }

        // when: creating all users
        val responses = users.map { userController.createUser(validInvite, it) }

        // then: all creations succeed
        responses.forEach { resp ->
            assertEquals(HttpStatus.CREATED, resp.statusCode)
        }
    }

    @Test
    fun `edge case user with very long name`() {
        // given: user with long name
        val longName = "A".repeat(100)
        val input = CreateUserDTO(longName, "nick", "long@example.com", "Pass123!", null)

        // when: creating user
        val resp = userController.createUser(validInvite, input)

        // then: creation succeeds
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body as Map<*, *>
        assertEquals(longName, body["name"])
    }

    @Test
    fun `edge case user with special characters in email`() {
        // given: email with special characters
        val input =
            CreateUserDTO(
                "User",
                "nick",
                "user+test@example.co.uk",
                "Pass123!",
                null,
            )

        // when: creating user
        val resp = userController.createUser(validInvite, input)

        // then: creation succeeds
        assertEquals(HttpStatus.CREATED, resp.statusCode)
    }
}
