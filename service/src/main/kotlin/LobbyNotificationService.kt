package org.example

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class LobbyEvent(
    val type: LobbyEventType,
    val lobbyId: Int,
    val message: String,
    val data: Map<String, Any> = emptyMap(),
)

enum class LobbyEventType {
    PLAYER_JOINED,
    PLAYER_LEFT,
    LOBBY_CLOSED,
    LOBBY_STARTING,
    GAME_STARTED,
    LOBBY_LIST_UPDATED,
}

private const val KEEP_ALIVE_INTERVAL = 5L

@Service
class LobbyNotificationService {
    private val userLobbyEmitters = ConcurrentHashMap<String, Pair<Int, SseEmitter>>()

    // Mapa auxiliar: LobbyId -> Lista de IDs de usuários (para facilitar o broadcast)
    private val lobbyUsers = ConcurrentHashMap<Int, MutableSet<Int>>()

    private val lock = ReentrantLock()

    private val executor =
        Executors
            .newScheduledThreadPool(1)
            .also {
                it.scheduleAtFixedRate(
                    { sendKeepAlive() },
                    KEEP_ALIVE_INTERVAL,
                    KEEP_ALIVE_INTERVAL,
                    TimeUnit.SECONDS,
                )
            }

    /**
     * Registra um jogador para receber notificações de um lobby
     * Fecha conexões antigas do mesmo usuário antes de abrir novas
     */
    fun subscribe(
        userId: Int,
        lobbyId: Int,
        emitter: SseEmitter,
    ) {
        lock.withLock {
            val key = "$userId-$lobbyId"

            userLobbyEmitters[key]?.let { (_, oldEmitter) ->
                try {
                    oldEmitter.complete()
                    logger.info("Closed previous connection for user $userId in lobby $lobbyId")
                } catch (ex: Exception) {
                    logger.debug("Error closing previous emitter: ${ex.message}")
                }
            }

            logger.info("User $userId subscribed to lobby $lobbyId")

            userLobbyEmitters[key] = Pair(lobbyId, emitter)
            lobbyUsers.computeIfAbsent(lobbyId) { mutableSetOf() }.add(userId)

            emitter.onCompletion {
                logger.info("User $userId disconnected from lobby $lobbyId (completion)")
                unsubscribe(userId, lobbyId, emitter)
            }

            emitter.onTimeout {
                logger.info("User $userId connection timed out in lobby $lobbyId")
                unsubscribe(userId, lobbyId, emitter)
            }

            emitter.onError { error ->
                logger.error("Error in SSE connection for user $userId in lobby $lobbyId: ${error.message}")
                unsubscribe(userId, lobbyId, emitter)
            }
        }
    }

    /**
     * Remove um jogador das notificações de um lobby
     */
    private fun unsubscribe(
        userId: Int,
        lobbyId: Int,
        emitter: SseEmitter,
    ) {
        lock.withLock {
            val key = "$userId-$lobbyId"
            val currentPair = userLobbyEmitters[key]

            if (currentPair != null && currentPair.second === emitter) {
                userLobbyEmitters.remove(key)

                lobbyUsers[lobbyId]?.remove(userId)
                if (lobbyUsers[lobbyId]?.isEmpty() == true) {
                    lobbyUsers.remove(lobbyId)
                }
                logger.info("Unsubscribed user $userId from lobby $lobbyId")
            } else {
                logger.debug("Ignored unsubscribe for user $userId (emitter mismatch or already removed)")
            }
        }
    }

    /**
     * Notifica todos os jogadores de um lobby
     */
    fun notifyLobby(
        lobbyId: Int,
        event: LobbyEvent,
    ) {
        val userIds = lobbyUsers[lobbyId]?.toList() ?: return
        logger.info("Sending event ${event.type} to ${userIds.size} listeners in lobby $lobbyId")

        val failedUsers = mutableListOf<Triple<Int, Int, SseEmitter>>()

        userIds.forEach { userId ->
            val key = "$userId-$lobbyId"
            val (_, emitter) = userLobbyEmitters[key] ?: return@forEach

            try {
                emitter.send(
                    SseEmitter
                        .event()
                        .name(event.type.name)
                        .data(
                            mapOf(
                                "type" to event.type.name,
                                "lobbyId" to event.lobbyId,
                                "message" to event.message,
                                "data" to event.data,
                                "timestamp" to System.currentTimeMillis(),
                            ),
                        ),
                )
            } catch (ex: Exception) {
                logger.error("Error sending event to user $userId in lobby $lobbyId: ${ex.message}")
                failedUsers.add(Triple(userId, lobbyId, emitter))
            }
        }

        // Limpa usuários que falharam
        if (failedUsers.isNotEmpty()) {
            lock.withLock {
                failedUsers.forEach { (uid, gid, em) ->
                    unsubscribe(uid, gid, em)
                }
            }
        }
    }

    /**
     * Fecha todas as conexões de um lobby
     */
    fun closeLobbyConnections(lobbyId: Int) {
        lock.withLock {
            val userIds = lobbyUsers[lobbyId]?.toList() ?: return
            logger.info("Closing ${userIds.size} connections for lobby $lobbyId")

            userIds.forEach { userId ->
                val key = "$userId-$lobbyId"
                userLobbyEmitters[key]?.let { (_, emitter) ->
                    try {
                        emitter.complete()
                    } catch (ex: Exception) {
                        logger.error("Error completing emitter: ${ex.message}")
                    }
                    userLobbyEmitters.remove(key)
                }
            }
            lobbyUsers.remove(lobbyId)
        }
    }

    private fun sendKeepAlive() {
        val lobbiesToCheck = lobbyUsers.keys.toList()

        lobbiesToCheck.forEach { lobbyId ->
            val userIds = lobbyUsers[lobbyId]?.toList() ?: return@forEach
            val failedUsers = mutableListOf<Triple<Int, Int, SseEmitter>>()

            userIds.forEach { userId ->
                val key = "$userId-$lobbyId"
                val (_, emitter) = userLobbyEmitters[key] ?: return@forEach

                try {
                    emitter.send(
                        SseEmitter
                            .event()
                            .name("keep-alive")
                            .data("ping"),
                    )
                } catch (ex: Exception) {
                    logger.debug("Keep-alive failed for user $userId in lobby $lobbyId: ${ex.message}")
                    failedUsers.add(Triple(userId, lobbyId, emitter))
                }
            }

            if (failedUsers.isNotEmpty()) {
                lock.withLock {
                    failedUsers.forEach { (uid, gid, em) ->
                        unsubscribe(uid, gid, em)
                    }
                }
            }
        }
    }

    @PreDestroy
    fun destroy() {
        logger.info("Shutting down LobbyNotificationService")
        executor.shutdown()
        lock.withLock {
            userLobbyEmitters.values.forEach { (_, emitter) ->
                try {
                    emitter.complete()
                } catch (ex: Exception) {
                    logger.error("Error completing emitter during shutdown: ${ex.message}")
                }
            }
            userLobbyEmitters.clear()
            lobbyUsers.clear()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LobbyNotificationService::class.java)
    }
}
