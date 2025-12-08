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

data class GameEvent(
    val type: GameEventType,
    val gameId: Int,
    val message: String,
    val data: Map<String, Any> = emptyMap(),
)

enum class GameEventType {
    ROUND_STARTED,
    PLAYER_ROLLED,
    PLAYER_FINISHED_TURN,
    ROUND_ENDED,
    GAME_ENDED,
    PLAYER_LEAVE,
}

private const val KEEP_ALIVE_INTERVAL = 5L

@Service
open class GameNotificationService {
    // Mapa de userId -> (gameId, emitter) - only one connection per user per game
    private val userGameEmitters = ConcurrentHashMap<String, Pair<Int, SseEmitter>>()

    // Mapa de gameId -> lista de user IDs nesse jogo
    private val gameUsers = ConcurrentHashMap<Int, MutableSet<Int>>()

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
     * Registra um jogador para receber notificações de um jogo
     * Garante apenas UMA conexão por usuário por jogo
     */
    fun subscribe(
        userId: Int,
        gameId: Int,
        emitter: SseEmitter,
    ) {
        lock.withLock {
            val key = "$userId-$gameId"

            // Remove conexão anterior do mesmo utilizador se existir
            userGameEmitters[key]?.let { (_, oldEmitter) ->
                try {
                    oldEmitter.complete()
                    logger.info("Closed previous connection for user $userId in game $gameId")
                } catch (ex: Exception) {
                    logger.debug("Error closing previous emitter: ${ex.message}")
                }
            }

            logger.info("User $userId subscribed to game $gameId")
            userGameEmitters[key] = Pair(gameId, emitter)
            gameUsers.computeIfAbsent(gameId) { mutableSetOf() }.add(userId)

            emitter.onCompletion {
                logger.info("User $userId disconnected from game $gameId (completion)")
                unsubscribe(userId, gameId, emitter)
            }

            emitter.onTimeout {
                logger.info("User $userId connection timed out in game $gameId")
                unsubscribe(userId, gameId, emitter)
            }

            emitter.onError { error ->
                logger.error("Error in SSE connection for user $userId in game $gameId: ${error.message}")
                unsubscribe(userId, gameId, emitter)
            }
        }
    }

    /**
     * Remove um jogador das notificações de um jogo
     */
    private fun unsubscribe(
        userId: Int,
        gameId: Int,
        emitter: SseEmitter,
    ) {
        lock.withLock {
            val key = "$userId-$gameId"
            val currentPair = userGameEmitters[key]

            if (currentPair != null && currentPair.second === emitter) {
                userGameEmitters.remove(key)

                gameUsers[gameId]?.remove(userId)
                if (gameUsers[gameId]?.isEmpty() == true) {
                    gameUsers.remove(gameId)
                }
                logger.info("Unsubscribed user $userId from game $gameId")
            } else {
                logger.debug("Ignored unsubscribe for user $userId (emitter mismatch or already removed)")
            }
        }
    }

    /**
     * Notifica todos os jogadores de um jogo
     */
    fun notifyGame(
        gameId: Int,
        event: GameEvent,
    ) {
        val userIds = gameUsers[gameId]?.toList() ?: return
        logger.info("Sending event ${event.type} to ${userIds.size} listeners in game $gameId")

        val failedUsers = mutableListOf<Triple<Int, Int, SseEmitter>>()
        userIds.forEach { userId ->
            val key = "$userId-$gameId"
            val (_, emitter) = userGameEmitters[key] ?: return@forEach

            try {
                emitter.send(
                    SseEmitter
                        .event()
                        .name(event.type.name)
                        .data(
                            mapOf(
                                "type" to event.type.name,
                                "gameId" to event.gameId,
                                "message" to event.message,
                                "data" to event.data,
                                "timestamp" to System.currentTimeMillis(),
                            ),
                        ),
                )
            } catch (ex: Exception) {
                logger.error("Error sending event to user $userId in game $gameId: ${ex.message}")
                failedUsers.add(Triple(userId, gameId, emitter))
            }
        }

        // Remove users com falha na conexão
        if (failedUsers.isNotEmpty()) {
            lock.withLock {
                failedUsers.forEach { (uid, gid, em) ->
                    unsubscribe(uid, gid, em)
                }
            }
        }
    }

    /**
     * Fecha todas as conexões de um jogo
     */
    fun closeGameConnections(gameId: Int) {
        lock.withLock {
            val userIds = gameUsers[gameId]?.toList() ?: return
            logger.info("Closing ${userIds.size} connections for game $gameId")

            userIds.forEach { userId ->
                val key = "$userId-$gameId"
                userGameEmitters[key]?.let { (_, emitter) ->
                    try {
                        emitter.complete()
                    } catch (ex: Exception) {
                        logger.error("Error completing emitter: ${ex.message}")
                    }
                    userGameEmitters.remove(key)
                }
            }

            gameUsers.remove(gameId)
        }
    }

    private fun sendKeepAlive() {
        val gamesToCheck = gameUsers.keys.toList()

        gamesToCheck.forEach { gameId ->
            val userIds = gameUsers[gameId]?.toList() ?: return@forEach
            val failedUsers = mutableListOf<Triple<Int, Int, SseEmitter>>()

            userIds.forEach { userId ->
                val key = "$userId-$gameId"
                val (_, emitter) = userGameEmitters[key] ?: return@forEach

                try {
                    emitter.send(
                        SseEmitter
                            .event()
                            .name("keep-alive")
                            .data("ping"),
                    )
                } catch (ex: Exception) {
                    logger.debug("Keep-alive failed for user $userId in game $gameId: ${ex.message}")
                    failedUsers.add(Triple(userId, gameId, emitter))
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
        logger.info("Shutting down GameNotificationService")
        executor.shutdown()
        lock.withLock {
            userGameEmitters.values.forEach { (_, emitter) ->
                try {
                    emitter.complete()
                } catch (ex: Exception) {
                    logger.error("Error completing emitter during shutdown: ${ex.message}")
                }
            }
            userGameEmitters.clear()
            gameUsers.clear()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GameNotificationService::class.java)
    }
}
