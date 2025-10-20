package org.example

import jakarta.inject.Named
import org.example.config.LobbiesDomainConfig
import org.example.entity.core.toName
import org.example.entity.lobby.Lobby
import java.time.Duration
import java.time.Instant
import java.util.UUID

sealed class LobbyError(
    val message: String,
) {
    object LobbyNotFound : LobbyError("Lobby not found")

    object UserNotFound : LobbyError("User not found")

    object InvalidLobbyData : LobbyError("Invalid lobby data")

    object AlreadyInLobby : LobbyError("User already in a lobby")

    object NotInLobby : LobbyError("User not in this lobby")

    object LobbyFull : LobbyError("Lobby is full")
}

typealias LobbyResult = Either<LobbyError, Lobby>

@Named
class LobbyService(
    private val trxManager: TransactionManager,
    private val config: LobbiesDomainConfig,
    private val notificationService: LobbyNotificationService,
) {
    fun createLobby(
        hostId: Int,
        name: String,
        maxPlayers: Int,
        rounds: Int,
    ): LobbyResult {
        if (name.isBlank() || maxPlayers <= 0) {
            return failure(LobbyError.InvalidLobbyData)
        }
        return trxManager.run {
            val lobby = repositoryLobby.createLobby(name.toName(), hostId, maxPlayers, rounds)
            success(lobby)
        }
    }

    fun listLobbies(): List<Lobby> =
        trxManager.run {
            repositoryLobby.findAll()
        }

    fun getLobbyDetails(lobbyId: Int): LobbyResult =
        trxManager.run {
            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)
            success(lobby)
        }

    fun joinLobby(
        userId: Int,
        lobbyId: Int,
    ): LobbyResult =
        trxManager.run {
            val user =
                repositoryUser.findById(userId)
                    ?: return@run failure(LobbyError.UserNotFound)

            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)

            if (repositoryLobby.isUserInLobby(userId, lobby.id)) {
                return@run failure(LobbyError.AlreadyInLobby)
            }

            if (repositoryLobby.countPlayers(lobby.id) >= lobby.maxPlayers) {
                return@run failure(LobbyError.LobbyFull)
            }

            repositoryLobby.addPlayer(lobby.id, user.id)
            val updatedLobby = repositoryLobby.findById(lobby.id)!!

            // Notificar todos no lobby que um jogador entrou
            notificationService.notifyLobby(
                updatedLobby.id,
                LobbyEvent(
                    type = LobbyEventType.PLAYER_JOINED,
                    lobbyId = updatedLobby.id,
                    message = "${user.nickName.value} joined the lobby",
                    data = mapOf(
                        "userId" to user.id,
                        "userName" to user.nickName.value,
                        "currentPlayers" to updatedLobby.currentPlayers.size,
                        "maxPlayers" to updatedLobby.maxPlayers,
                    )
                )
            )

            // Verificar se o lobby está pronto para iniciar após o join
            checkIfLobbyReady(updatedLobby)

            success(updatedLobby)
        }

    fun leaveLobby(
        userId: Int,
        lobbyId: Int,
    ): LobbyResult =
        trxManager.run {
            val lobby =
                repositoryLobby.findById(lobbyId)
                    ?: return@run failure(LobbyError.LobbyNotFound)

            if (!repositoryLobby.isUserInLobby(userId, lobby.id)) {
                return@run failure(LobbyError.NotInLobby)
            }

            val user = repositoryUser.findById(userId)!!

            // Captura o estado do lobby antes de remover
            val isHost = lobby.hostId == userId
            val updatedLobby =
                lobby.copy(
                    currentPlayers = lobby.currentPlayers.filter { it.id != userId },
                )

            repositoryLobby.removePlayer(lobby.id, userId)

            // Notificar todos no lobby que um jogador saiu
            notificationService.notifyLobby(
                lobby.id,
                LobbyEvent(
                    type = LobbyEventType.PLAYER_LEFT,
                    lobbyId = lobby.id,
                    message = "${user.nickName.value} left the lobby",
                    data = mapOf(
                        "userId" to user.id,
                        "userName" to user.nickName.value,
                        "currentPlayers" to updatedLobby.currentPlayers.size,
                        "maxPlayers" to updatedLobby.maxPlayers,
                    )
                )
            )

            // Se o host sair, fecha o lobby
            if (isHost) {
                closeLobbyAndNotify(lobby.id, "Host left the lobby")
            }

            success(updatedLobby)
        }

    /**
     * Verifica lobbies prontos para iniciar ou fechar por timeout
     */
    fun checkAndStartReadyLobbies() {
        trxManager.run {
            val allLobbies = repositoryLobby.findAll()
            val now = Instant.now()

            allLobbies.forEach { lobby ->
                val currentPlayerCount = lobby.currentPlayers.size
                val timeElapsed = Duration.between(lobby.createdAt, now).seconds

                when {
                    // 1. Lobby está cheio - iniciar imediatamente
                    currentPlayerCount >= lobby.maxPlayers -> {
                        startLobby(lobby)
                    }

                    // 2. Timeout passou
                    timeElapsed >= config.lobbyTimeoutSeconds -> {
                        if (currentPlayerCount >= config.minPlayersToStart) {
                            // Tem jogadores suficientes - iniciar
                            startLobby(lobby)
                        } else {
                            closeLobbyAndNotify(
                                lobby.id,
                                "Lobby closed: insufficient players after timeout (${currentPlayerCount}/${config.minPlayersToStart} required)"
                            )
                        }
                    }
                    // 3. Ainda dentro do tempo - não fazer nada
                }
            }
        }
    }

    private fun startLobby(lobby: Lobby) {
        trxManager.run{
        println("Lobby ${lobby.id} starting with ${lobby.currentPlayers.size} jogadores!")

        // Notificar todos os jogadores que o jogo está começando
        notificationService.notifyLobby(
            lobby.id,
            LobbyEvent(
                type = LobbyEventType.LOBBY_STARTING,
                lobbyId = lobby.id,
                message = "Game is starting!",
                data = mapOf(
                    "currentPlayers" to lobby.currentPlayers.size,
                    "players" to lobby.currentPlayers.map {
                        mapOf(
                            "id" to it.id,
                            "nickName" to it.nickName.value
                        )
                    }
                )
            )
        )

        repositoryGame.createGame(lobby.hostId, lobby.id)

        // Após iniciar o jogo, enviar evento final
        notificationService.notifyLobby(
            lobby.id,
            LobbyEvent(
                type = LobbyEventType.GAME_STARTED,
                lobbyId = lobby.id,
                message = "Game has started!",
                data = mapOf("gameId" to 123) // ID do jogo criado
            )
        )

        // Fechar conexões SSE após iniciar o jogo

            repositoryLobby.closeLobby(lobby.id)
        notificationService.closeLobbyConnections(lobby.id)
    }
    }

    private fun closeLobbyAndNotify(
        lobbyId: Int,
        reason: String
    ) {
        trxManager.run{
        val lobby = repositoryLobby.findById(lobbyId) ?: return@run

        println("Closing lobby $lobbyId: $reason")

        // Notificar todos os jogadores antes de fechar
        notificationService.notifyLobby(
            lobbyId,
            LobbyEvent(
                type = LobbyEventType.LOBBY_CLOSED,
                lobbyId = lobbyId,
                message = reason,
                data = mapOf(
                    "currentPlayers" to lobby.currentPlayers.size,
                    "minPlayersRequired" to config.minPlayersToStart
                )
            )
        )

        // Fechar o lobby
        repositoryLobby.closeLobby(lobbyId)

        // Fechar todas as conexões SSE
        notificationService.closeLobbyConnections(lobbyId)
    }
    }

    private fun checkIfLobbyReady(lobby: Lobby) {
        val currentPlayerCount = lobby.currentPlayers.size

        // Se o lobby está cheio, pode iniciar imediatamente
        if (currentPlayerCount >= lobby.maxPlayers) {
            startLobby(lobby)
        }
    }


}
