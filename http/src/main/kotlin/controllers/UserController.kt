package org.example.controllers

import org.example.Either
import org.example.Failure
import org.example.Success
import org.example.TokenCreationError
import org.example.UserAuthService
import org.example.UserError
import org.example.dto.inputDto.AuthenticatedUserDto
import org.example.dto.inputDto.CreateUserDTO
import org.example.dto.inputDto.LoginUserDTO
import org.example.dto.inputDto.UpdateUserDTO
import org.example.dto.inputDto.ValidInviteDTO
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController(
    private val userServices: UserAuthService,
) {
    @PostMapping(
        "/create",
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun createUser(
        invite: ValidInviteDTO,
        @RequestBody body: CreateUserDTO,
    ): ResponseEntity<*> =
        handleUserResult(
            "/user/create",
            userServices.createUser(body.name, body.nickName, body.email, body.password, body.imageUrl),
            HttpStatus.CREATED,
        ) {
            mapOf(
                "userId" to it.id,
                "name" to it.name.value,
                "nickName" to it.nickName.value,
                "email" to it.email.value,
                "balance" to it.balance.money.value,
                "_links" to UserLinks.createUser(it.id),
            )
        }

    @PostMapping(
        "/login",
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun loginUser(
        @RequestBody body: LoginUserDTO,
    ): ResponseEntity<*> {
        val result = userServices.createToken(body.email, body.password)
        return when (result) {
            is Failure -> handleTokenError(result.value, "/user/login")
            is Success ->
                ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    mapOf(
                        "token" to result.value.tokenValue,
                        "expiresAt" to result.value.tokenExpiration.toString(),
                        "message" to "Login successful",
                        "_links" to UserLinks.login(),
                    ),
                )
        }
    }

    @GetMapping(
        "/info",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun getUserInfo(user: AuthenticatedUserDto): ResponseEntity<*> =
        ResponseEntity.ok(
            mapOf(
                "userId" to user.user.id,
                "name" to user.user.name.value,
                "nickName" to user.user.nickName.value,
                "email" to user.user.email.value,
                "balance" to user.user.balance.money.value,
                "imageUrl" to user.user.imageUrl?.value,
                "_links" to UserLinks.userInfo(user.user.id),
            ),
        )

    @PostMapping(
        "/update",
        consumes = [ApiMediaTypes.APPLICATION_JSON],
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun updateUser(
        user: AuthenticatedUserDto,
        @RequestBody body: UpdateUserDTO,
    ): ResponseEntity<*> =
        handleUserResult(
            "/user/update",
            userServices.updateUser(user.user.id, body.name, body.nickName, body.password, body.imageUrl),
            HttpStatus.ACCEPTED,
        ) {
            mapOf(
                "userId" to it.id,
                "name" to it.name.value,
                "nickName" to it.nickName.value,
                "email" to it.email.value,
                "message" to "User updated successfully",
                "_links" to UserLinks.updateUser(it.id),
            )
        }

    @PostMapping(
        "/logout",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun logoutUser(user: AuthenticatedUserDto): ResponseEntity<*> {
        userServices.revokeToken(user.token)
        return ResponseEntity.ok(
            mapOf(
                "message" to "Logout successful",
                "_links" to UserLinks.logout(),
            ),
        )
    }

    @GetMapping(
        "/stats",
        produces = [ApiMediaTypes.APPLICATION_JSON, ApiMediaTypes.APPLICATION_PROBLEM_JSON],
    )
    fun userStats(user: AuthenticatedUserDto): ResponseEntity<*> =
        handleUserResult("/user/stats", userServices.getUserInfo(user.user.id), HttpStatus.OK) {
            mapOf(
                "userId" to it.userId,
                "totalGamesPlayed" to it.totalGamesPlayed,
                "totalWins" to it.totalWins,
                "totalLosses" to it.totalLosses,
                "totalPoints" to it.totalPoints,
                "longestStreak" to it.longestStreak,
                "currentStreak" to it.currentStreak,
                "_links" to UserLinks.userStats(it.userId),
            )
        }

    private fun handleTokenError(
        error: TokenCreationError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is TokenCreationError.UserOrPasswordAreInvalid ->
                ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INVALID_CREDENTIALS,
                            title = "Invalid Credentials",
                            status = HttpStatus.UNAUTHORIZED,
                            detail = "The provided email or password is incorrect.",
                            instance = instance,
                        ),
                    )
        }

    private fun handleUserError(
        error: UserError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is UserError.InsecurePassword ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INSECURE_PASSWORD,
                            title = "Insecure Password",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "Password does not meet security requirements.",
                            instance = instance,
                        ),
                    )

            is UserError.AlreadyUsedEmailAddress ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.EMAIL_ALREADY_USED,
                            title = "Email Already Used",
                            status = HttpStatus.CONFLICT,
                            detail = "An account with this email already exists.",
                            instance = instance,
                        ),
                    )

            is UserError.InvalidCredentials ->
                ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INVALID_CREDENTIALS,
                            title = "Invalid Credentials",
                            status = HttpStatus.UNAUTHORIZED,
                            detail = "The provided credentials are invalid.",
                            instance = instance,
                        ),
                    )
        }

    private inline fun <T> handleUserResult(
        path: String,
        result: Either<UserError, T>,
        status: HttpStatus = HttpStatus.OK,
        successBodyBuilder: (T) -> Any,
    ): ResponseEntity<*> =
        when (result) {
            is Failure -> handleUserError(result.value, path)
            is Success -> ResponseEntity.status(status).body(successBodyBuilder(result.value))
        }
}
