
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
}

private const val KEEP_ALIVE_INTERVAL = 25L

@Service
class LobbyNotificationService {
    // Mapa de lobbyId -> lista de emitters dos jogadores nesse lobby
    private val lobbyListeners = ConcurrentHashMap<Int, MutableList<SseEmitter>>()

    // Mapa de userId -> lobbyId para saber em que lobby cada user está
    private val userLobbyMap = ConcurrentHashMap<Int, Int>()
    private val lock = ReentrantLock()

    // Keep-alive every 25 seconds

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
     */
    fun subscribe(
        userId: Int,
        lobbyId: Int,
        emitter: SseEmitter,
    ) {
        lock.withLock {
            logger.info("User $userId subscribed to lobby $lobbyId")

            lobbyListeners.computeIfAbsent(lobbyId) { mutableListOf() }.add(emitter)
            userLobbyMap[userId] = lobbyId

            emitter.onCompletion {
                logger.info("User $userId disconnected from lobby $lobbyId")
                unsubscribe(userId, lobbyId, emitter)
            }

            emitter.onTimeout {
                logger.info("User $userId connection timed out in lobby $lobbyId")
                unsubscribe(userId, lobbyId, emitter)
            }

            emitter.onError { error ->
                logger.error("Error in SSE connection for user $userId in lobby $lobbyId", error)
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
            lobbyListeners[lobbyId]?.remove(emitter)
            if (lobbyListeners[lobbyId]?.isEmpty() == true) {
                lobbyListeners.remove(lobbyId)
            }
            userLobbyMap.remove(userId)
        }
    }

    /**
     * Notifica todos os jogadores de um lobby
     */
    fun notifyLobby(
        lobbyId: Int,
        event: LobbyEvent,
    ) {
        val emitters = lobbyListeners[lobbyId] ?: return
        logger.info("Sending event ${event.type} to ${emitters.size} listeners in lobby $lobbyId")

        val deadEmitters = mutableListOf<SseEmitter>()
        emitters.forEach { emitter ->
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
                logger.error("Error sending event to emitter: ${ex.message}")
                deadEmitters.add(emitter)
            }
        }

        // Remove emitters mortos com lock
        if (deadEmitters.isNotEmpty()) {
            lock.withLock {
                deadEmitters.forEach { emitter ->
                    emitters.remove(emitter)
                }
            }
        }
    }

    /**
     * Fecha todas as conexões de um lobby (usado quando o lobby é fechado)
     */
    fun closeLobbyConnections(lobbyId: Int) {
        lock.withLock {
            val emitters = lobbyListeners[lobbyId] ?: return
            logger.info("Closing ${emitters.size} connections for lobby $lobbyId")

            emitters.forEach { emitter ->
                try {
                    emitter.complete()
                } catch (ex: Exception) {
                    logger.error("Error completing emitter: ${ex.message}")
                }
            }

            lobbyListeners.remove(lobbyId)
            userLobbyMap.entries.removeIf { it.value == lobbyId }
        }
    }

    private fun sendKeepAlive() {
        lobbyListeners.forEach { (lobbyId, emitters) ->
            val deadEmitters = mutableListOf<SseEmitter>()
            emitters.forEach { emitter ->
                try {
                    emitter.send(
                        SseEmitter
                            .event()
                            .name("keep-alive")
                            .data("ping"),
                    )
                } catch (ex: Exception) {
                    logger.debug("Keep-alive failed for emitter in lobby $lobbyId: ${ex.message}")
                    deadEmitters.add(emitter)
                }
            }
            if (deadEmitters.isNotEmpty()) {
                lock.withLock {
                    deadEmitters.forEach { emitter ->
                        emitters.remove(emitter)
                    }
                    if (emitters.isEmpty()) {
                        lobbyListeners.remove(lobbyId)
                    }
                }
            }
        }
    }

    @PreDestroy
    fun destroy() {
        logger.info("Shutting down LobbyNotificationService")
        executor.shutdown()
        lobbyListeners.values.forEach { emitters ->
            emitters.forEach { it.complete() }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LobbyNotificationService::class.java)
    }
}
