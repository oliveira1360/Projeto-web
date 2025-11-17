package org.example.game

sealed class GameError {
    data object EmptyHand : GameError()

    data object NoRoundInProgress : GameError()

    data object GameNotFinished : GameError()

    data object GameNotFound : GameError()

    data object LobbyNotFound : GameError()

    data object InvalidGameId : GameError()

    data object InvalidUserId : GameError()

    data object GameAlreadyFinished : GameError()

    data object NoPlayersInGame : GameError()

    data object InvalidDiceIndices : GameError()

    data object TooManyRolls : GameError()

    data object RoundNotStarted : GameError()

    data object AllPlayersNotFinished : GameError()

    data object UserNotInGame : GameError()

    data object UnauthorizedAction : GameError()

    data object NotPlayerTurn : GameError()
}
