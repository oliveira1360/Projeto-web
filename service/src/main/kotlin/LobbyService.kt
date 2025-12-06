package org.example

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Named
import org.example.config.LobbiesDomainConfig
import org.example.entity.core.toName
import org.example.entity.lobby.Lobby
import org.example.game.GameService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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
    private val gameService: GameService,
) {
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

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
            notificationService.notifyLobby(
                0,
                LobbyEvent(
                    type = LobbyEventType.LOBBY_LIST_UPDATED,
                    lobbyId = 0,
                    message = "New lobby created",
                ),
            )
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

    @PostConstruct
    fun init() {
        scheduler.scheduleAtFixedRate(
            {
                try {
                    checkAndStartReadyLobbies()
                } catch (e: Exception) {
                    logger.error("Error checking lobbies", e)
                }
            },
            5, // initial delay
            5, // period
            TimeUnit.SECONDS,
        )
        logger.info("LobbyService scheduler started - checking lobbies every 5 seconds")
    }

    @PreDestroy
    fun destroy() {
        logger.info("Shutting down LobbyService scheduler")
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
        }
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
                    data =
                        mapOf(
                            "userId" to user.id,
                            "userName" to user.nickName.value,
                            "currentPlayers" to updatedLobby.currentPlayers.size,
                            "maxPlayers" to updatedLobby.maxPlayers,
                        ),
                ),
            )

            // Verificar se o lobby está pronto para iniciar após o join
            if (updatedLobby.currentPlayers.size >= updatedLobby.maxPlayers) {
                startLobby(updatedLobby)
            }

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
                    data =
                        mapOf(
                            "userId" to user.id,
                            "userName" to user.nickName.value,
                            "currentPlayers" to updatedLobby.currentPlayers.size,
                            "maxPlayers" to updatedLobby.maxPlayers,
                        ),
                ),
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
                    timeElapsed >= config.lobbyTimeoutSeconds -> {
                        if (currentPlayerCount >= config.minPlayersToStart) {
                            notificationService.notifyLobby(
                                lobby.id,
                                LobbyEvent(
                                    type = LobbyEventType.LOBBY_STARTING,
                                    lobbyId = lobby.id,
                                    message = "Timeout reached! Starting game with $currentPlayerCount players...",
                                    data =
                                        mapOf(
                                            "reason" to "TIMEOUT",
                                            "currentPlayers" to currentPlayerCount,
                                        ),
                                ),
                            )
                            startLobby(lobby)
                        } else {
                            closeLobbyAndNotify(
                                lobby.id,
                                "Lobby closed: timeout with insufficient players ($currentPlayerCount/${config.minPlayersToStart} required)",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startLobby(lobby: Lobby) {
        trxManager.run {
            logger.info("Starting lobby ${lobby.id} with ${lobby.currentPlayers.size} players")

            notificationService.notifyLobby(
                lobby.id,
                LobbyEvent(
                    type = LobbyEventType.LOBBY_STARTING,
                    lobbyId = lobby.id,
                    message = "Game is starting!",
                    data =
                        mapOf(
                            "currentPlayers" to lobby.currentPlayers.size,
                            "players" to
                                lobby.currentPlayers.map {
                                    mapOf("id" to it.id, "nickName" to it.nickName.value)
                                },
                        ),
                ),
            )

            val gameId = gameService.createGame(lobby.hostId, lobby.id)

            notificationService.notifyLobby(
                lobby.id,
                LobbyEvent(
                    type = LobbyEventType.GAME_STARTED,
                    lobbyId = lobby.id,
                    message = "Game has started!",
                    data = mapOf("gameId" to gameId),
                ),
            )

            repositoryLobby.closeLobby(lobby.id)
            notificationService.closeLobbyConnections(lobby.id)
        }
    }

    private fun closeLobbyAndNotify(
        lobbyId: Int,
        reason: String,
    ) {
        trxManager.run {
            val lobby = repositoryLobby.findById(lobbyId) ?: return@run

            logger.info("Closing lobby $lobbyId: $reason")

            notificationService.notifyLobby(
                lobbyId,
                LobbyEvent(
                    type = LobbyEventType.LOBBY_CLOSED,
                    lobbyId = lobbyId,
                    message = reason,
                    data =
                        mapOf(
                            "currentPlayers" to lobby.currentPlayers.size,
                            "minPlayersRequired" to config.minPlayersToStart,
                        ),
                ),
            )

            repositoryLobby.closeLobby(lobbyId)
            notificationService.closeLobbyConnections(lobbyId)
            notificationService.notifyLobby(
                0,
                LobbyEvent(
                    type = LobbyEventType.LOBBY_LIST_UPDATED,
                    lobbyId = 0,
                    message = "Lobby closed",
                ),
            )
        }
    }

    private fun checkIfLobbyReady(lobby: Lobby) {
        val currentPlayerCount = lobby.currentPlayers.size

        // Se o lobby está cheio, pode iniciar imediatamente
        if (currentPlayerCount >= lobby.maxPlayers) {
            startLobby(lobby)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LobbyService::class.java)
    }
}
