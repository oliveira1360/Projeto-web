package org.example.controllers

import org.example.Either
import org.example.Failure
import org.example.Success
import org.example.game.GameError
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class HandleError {
    /**
     * Handles game errors and converts them to RFC 7807 Problem+JSON responses.
     */
    fun handleGameError(
        error: GameError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is GameError.EmptyHand ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.EMPTY_HAND,
                            title = "Empty Hand",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "Player has no hand in the current round. Shuffle first.",
                            instance = instance,
                            additionalProperties = mapOf("requiredAction" to "shuffle"),
                        ),
                    )

            is GameError.NoRoundInProgress ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.NO_ROUND_IN_PROGRESS,
                            title = "No Round In Progress",
                            status = HttpStatus.CONFLICT,
                            detail = "Cannot determine round winner when no round is in progress.",
                            instance = instance,
                        ),
                    )

            is GameError.GameNotFinished ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.GAME_NOT_FINISHED,
                            title = "Game Not Finished",
                            status = HttpStatus.CONFLICT,
                            detail = "Cannot determine game winner until all rounds are completed.",
                            instance = instance,
                            additionalProperties = mapOf("allPlayersFinished" to false),
                        ),
                    )

            is GameError.TooManyRolls ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.TOO_MANY_ROLLS,
                            title = "Too Many Rolls",
                            status = HttpStatus.FORBIDDEN,
                            detail = "Maximum number of rolls (3) exceeded for this round.",
                            instance = instance,
                            additionalProperties = mapOf("maxRolls" to 3, "currentRolls" to 3),
                        ),
                    )

            is GameError.GameNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.GAME_NOT_FOUND,
                            title = "Game Not Found",
                            status = HttpStatus.NOT_FOUND,
                            detail = "The requested game does not exist.",
                            instance = instance,
                        ),
                    )

            is GameError.LobbyNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.LOBBY_NOT_FOUND,
                            title = "Lobby Not Found",
                            status = HttpStatus.NOT_FOUND,
                            detail = "The requested lobby does not exist.",
                            instance = instance,
                        ),
                    )

            is GameError.InvalidGameId ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INVALID_GAME_ID,
                            title = "Invalid Game ID",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "The provided game ID is invalid.",
                            instance = instance,
                        ),
                    )

            is GameError.InvalidUserId ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INVALID_USER_ID,
                            title = "Invalid User ID",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "The provided user ID is invalid.",
                            instance = instance,
                        ),
                    )

            is GameError.GameAlreadyFinished ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.GAME_ALREADY_FINISHED,
                            title = "Game Already Finished",
                            status = HttpStatus.CONFLICT,
                            detail = "The game has already been finished.",
                            instance = instance,
                        ),
                    )

            is GameError.NoPlayersInGame ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.NO_PLAYERS_IN_GAME,
                            title = "No Players In Game",
                            status = HttpStatus.CONFLICT,
                            detail = "The game has no players.",
                            instance = instance,
                        ),
                    )

            is GameError.InvalidDiceIndices ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.INVALID_DICE_INDICES,
                            title = "Invalid Dice Indices",
                            status = HttpStatus.BAD_REQUEST,
                            detail = "The provided dice indices are invalid.",
                            instance = instance,
                        ),
                    )

            is GameError.RoundNotStarted ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.ROUND_NOT_STARTED,
                            title = "Round Not Started",
                            status = HttpStatus.CONFLICT,
                            detail = "No round has been started yet.",
                            instance = instance,
                        ),
                    )

            is GameError.AllPlayersNotFinished ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.ALL_PLAYERS_NOT_FINISHED,
                            title = "All Players Not Finished",
                            status = HttpStatus.CONFLICT,
                            detail = "Not all players have finished their turns.",
                            instance = instance,
                        ),
                    )

            is GameError.UserNotInGame ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.USER_NOT_IN_GAME,
                            title = "User Not In Game",
                            status = HttpStatus.CONFLICT,
                            detail = "The user is not a player in this game.",
                            instance = instance,
                        ),
                    )

            is GameError.UnauthorizedAction ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.UNAUTHORIZED_ACTION,
                            title = "Unauthorized Action",
                            status = HttpStatus.FORBIDDEN,
                            detail = "You are not authorized to perform this action.",
                            instance = instance,
                        ),
                    )

            is GameError.NotPlayerTurn ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_PROBLEM_JSON)
                    .body(
                        createProblemDetail(
                            type = ProblemTypes.UNAUTHORIZED_ACTION,
                            title = "Not Player Turn",
                            status = HttpStatus.CONFLICT,
                            detail = "It is not your turn to play.",
                            instance = instance,
                        ),
                    )
        }

    inline fun <T> handleResult(
        path: String,
        result: Either<GameError, T>,
        status: HttpStatus = HttpStatus.OK,
        successBodyBuilder: (T) -> Any,
    ): ResponseEntity<*> =
        when (result) {
            is Failure -> handleGameError(result.value, path)
            is Success -> ResponseEntity.status(status).body(successBodyBuilder(result.value))
        }
}
