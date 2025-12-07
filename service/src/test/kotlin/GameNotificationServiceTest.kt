package org.example

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class TestSseEmitter : SseEmitter() {
    val sentEvents = mutableListOf<Pair<String, Any?>>()
    var isCompleted = false
    var isTimedOut = false

    var completionCallback: (() -> Unit)? = null
    var timeoutCallback: (() -> Unit)? = null
    var errorCallback: ((Throwable) -> Unit)? = null

    override fun send(builder: SseEventBuilder) {
        if (isCompleted || isTimedOut) {
            throw IllegalStateException("Emitter is closed")
        }

        sentEvents.add("event" to builder)
    }

    fun send(data: String) {
        if (isCompleted || isTimedOut) {
            throw IllegalStateException("Emitter is closed")
        }
        sentEvents.add("data" to data)
    }

    override fun complete() {
        isCompleted = true
        completionCallback?.invoke()
    }

    override fun onCompletion(callback: Runnable) {
        completionCallback = { callback.run() }
    }

    override fun onTimeout(callback: Runnable) {
        timeoutCallback = { callback.run() }
    }

    override fun onError(callback: Consumer<Throwable>) {
        errorCallback = { error -> callback.accept(error) }
    }
}

class GameNotificationServiceTest {
    private lateinit var gameNotificationService: GameNotificationService
    private lateinit var emitter1: TestSseEmitter
    private lateinit var emitter2: TestSseEmitter

    @BeforeEach
    fun setUp() {
        gameNotificationService = GameNotificationService()
        emitter1 = TestSseEmitter()
        emitter2 = TestSseEmitter()
    }

    @Test
    fun `subscribe should register user to game`() {
        val userId = 1
        val gameId = 100

        gameNotificationService.subscribe(userId, gameId, emitter1)

        assertTrue(!emitter1.isCompleted, "Emitter should not be completed after subscribe")
    }

    @Test
    fun `subscribe should close previous connection`() {
        val userId = 1
        val gameId = 100
        val oldEmitter = TestSseEmitter()

        gameNotificationService.subscribe(userId, gameId, oldEmitter)
        gameNotificationService.subscribe(userId, gameId, emitter1)

        assertTrue(oldEmitter.isCompleted, "Old emitter should be completed")
        assertTrue(!emitter1.isCompleted, "New emitter should not be completed")
    }

    @Test
    fun `notifyGame should send event to all subscribed users`() {
        val gameId = 100
        val userId1 = 1
        val userId2 = 2
        val event =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId,
                message = "Round started",
                data = mapOf("round" to 1),
            )

        gameNotificationService.subscribe(userId1, gameId, emitter1)
        gameNotificationService.subscribe(userId2, gameId, emitter2)
        gameNotificationService.notifyGame(gameId, event)

        assertTrue(emitter1.sentEvents.isNotEmpty(), "Emitter 1 should have received events")
        assertTrue(emitter2.sentEvents.isNotEmpty(), "Emitter 2 should have received events")
    }

    @Test
    fun `notifyGame should not send to other games`() {
        val gameId1 = 100
        val gameId2 = 200
        val userId = 1
        val event =
            GameEvent(
                type = GameEventType.PLAYER_ROLLED,
                gameId = gameId1,
                message = "Player rolled",
            )

        gameNotificationService.subscribe(userId, gameId1, emitter1)
        gameNotificationService.subscribe(userId, gameId2, emitter2)
        gameNotificationService.notifyGame(gameId1, event)

        assertTrue(emitter1.sentEvents.isNotEmpty(), "Emitter 1 should have received events for game 1")
        assertTrue(emitter2.sentEvents.isEmpty(), "Emitter 2 should not receive events for game 1")
    }

    @Test
    fun `closeGameConnections should close all connections for a game`() {
        val gameId = 100
        val userId1 = 1
        val userId2 = 2

        gameNotificationService.subscribe(userId1, gameId, emitter1)
        gameNotificationService.subscribe(userId2, gameId, emitter2)
        gameNotificationService.closeGameConnections(gameId)

        assertTrue(emitter1.isCompleted, "Emitter 1 should be completed")
        assertTrue(emitter2.isCompleted, "Emitter 2 should be completed")
    }

    @Test
    fun `notifyGame should remove failed users`() {
        val gameId = 100
        val userId1 = 1
        val userId2 = 2

        val failingEmitter =
            object : TestSseEmitter() {
                override fun send(builder: SseEventBuilder): Unit = throw IOException("Connection failed")
            }

        val successEmitter = TestSseEmitter()

        val event =
            GameEvent(
                type = GameEventType.ROUND_ENDED,
                gameId = gameId,
                message = "Round ended",
            )

        gameNotificationService.subscribe(userId1, gameId, failingEmitter)
        gameNotificationService.subscribe(userId2, gameId, successEmitter)
        gameNotificationService.notifyGame(gameId, event)

        val failingEmitterEventCount = failingEmitter.sentEvents.size
        val successEmitterEventCount1 = successEmitter.sentEvents.size

        gameNotificationService.notifyGame(gameId, event)

        assertEquals(
            failingEmitterEventCount,
            failingEmitter.sentEvents.size,
            "Failing emitter should not receive more events",
        )
        assertTrue(
            successEmitter.sentEvents.size > successEmitterEventCount1,
            "Success emitter should receive more events",
        )
    }

    @Test
    fun `unsubscribe via completion callback should remove user`() {
        val gameId = 100
        val userId = 1

        gameNotificationService.subscribe(userId, gameId, emitter1)

        val event =
            GameEvent(
                type = GameEventType.GAME_ENDED,
                gameId = gameId,
                message = "Game ended",
            )

        gameNotificationService.notifyGame(gameId, event)
        val firstEventCount = emitter1.sentEvents.size

        emitter1.complete()

        gameNotificationService.notifyGame(gameId, event)

        assertEquals(
            firstEventCount,
            emitter1.sentEvents.size,
            "User should not receive events after completion",
        )
    }

    @Test
    fun `unsubscribe via timeout callback should remove user`() {
        val gameId = 100
        val userId = 1
        var timeoutCallback: (() -> Unit)? = null

        val customEmitter =
            object : TestSseEmitter() {
                override fun onTimeout(callback: Runnable) {
                    timeoutCallback = { callback.run() }
                }
            }

        gameNotificationService.subscribe(userId, gameId, customEmitter)

        val event =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId,
                message = "Round started",
            )

        gameNotificationService.notifyGame(gameId, event)
        val firstEventCount = customEmitter.sentEvents.size

        // Simular timeout
        timeoutCallback?.invoke()

        gameNotificationService.notifyGame(gameId, event)

        assertEquals(
            firstEventCount,
            customEmitter.sentEvents.size,
            "User should not receive events after timeout",
        )
    }

    @Test
    fun `unsubscribe via error callback should remove user`() {
        val gameId = 100
        val userId = 1
        var errorCallback: ((Throwable) -> Unit)? = null

        val customEmitter =
            object : TestSseEmitter() {
                override fun onError(callback: Consumer<Throwable>) {
                    errorCallback = { error -> callback.accept(error) }
                }
            }

        gameNotificationService.subscribe(userId, gameId, customEmitter)

        val event =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId,
                message = "Round started",
            )

        gameNotificationService.notifyGame(gameId, event)
        val firstEventCount = customEmitter.sentEvents.size

        // Simular erro
        errorCallback?.invoke(Exception("Connection error"))

        gameNotificationService.notifyGame(gameId, event)

        assertEquals(
            firstEventCount,
            customEmitter.sentEvents.size,
            "User should not receive events after error",
        )
    }

    @Test
    fun `multiple games should be independent`() {
        val gameId1 = 100
        val gameId2 = 200
        val userId1 = 1
        val userId2 = 2

        val emitter3 = TestSseEmitter()
        val emitter4 = TestSseEmitter()

        gameNotificationService.subscribe(userId1, gameId1, emitter3)
        gameNotificationService.subscribe(userId2, gameId2, emitter4)

        val event1 =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId1,
                message = "Game 1 started",
            )
        val event2 =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId2,
                message = "Game 2 started",
            )

        gameNotificationService.notifyGame(gameId1, event1)
        gameNotificationService.notifyGame(gameId2, event2)

        assertTrue(emitter3.sentEvents.isNotEmpty(), "Emitter 3 should receive events for game 1")
        assertTrue(emitter4.sentEvents.isNotEmpty(), "Emitter 4 should receive events for game 2")
        assertEquals(1, emitter3.sentEvents.size, "Emitter 3 should only receive 1 event")
        assertEquals(1, emitter4.sentEvents.size, "Emitter 4 should only receive 1 event")
    }

    @Test
    fun `same user in multiple games should not interfere`() {
        val gameId1 = 100
        val gameId2 = 200
        val userId = 1

        val emitterGame1 = TestSseEmitter()
        val emitterGame2 = TestSseEmitter()

        gameNotificationService.subscribe(userId, gameId1, emitterGame1)
        gameNotificationService.subscribe(userId, gameId2, emitterGame2)

        val event1 =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId1,
                message = "Game 1 round",
            )
        val event2 =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId2,
                message = "Game 2 round",
            )

        gameNotificationService.notifyGame(gameId1, event1)
        gameNotificationService.notifyGame(gameId2, event2)

        assertEquals(1, emitterGame1.sentEvents.size, "User should receive only 1 event for game 1")
        assertEquals(1, emitterGame2.sentEvents.size, "User should receive only 1 event for game 2")
    }

    @Test
    fun `multiple users in same game should receive events`() {
        val gameId = 100
        val userId1 = 1
        val userId2 = 2
        val userId3 = 3

        val emitter3 = TestSseEmitter()

        val event =
            GameEvent(
                type = GameEventType.PLAYER_FINISHED_TURN,
                gameId = gameId,
                message = "Player finished turn",
            )

        gameNotificationService.subscribe(userId1, gameId, emitter1)
        gameNotificationService.subscribe(userId2, gameId, emitter2)
        gameNotificationService.subscribe(userId3, gameId, emitter3)
        gameNotificationService.notifyGame(gameId, event)

        assertTrue(emitter1.sentEvents.isNotEmpty())
        assertTrue(emitter2.sentEvents.isNotEmpty())
        assertTrue(emitter3.sentEvents.isNotEmpty())
    }

    @Test
    fun `empty game notification should handle gracefully`() {
        val gameId = 100
        val event =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId,
                message = "Round started",
            )

        gameNotificationService.notifyGame(gameId, event)
    }

    @Test
    fun `closeGameConnections should not affect other games`() {
        val gameId1 = 100
        val gameId2 = 200
        val userId1 = 1
        val userId2 = 2

        val emitter3 = TestSseEmitter()
        val emitter4 = TestSseEmitter()

        gameNotificationService.subscribe(userId1, gameId1, emitter3)
        gameNotificationService.subscribe(userId2, gameId2, emitter4)

        gameNotificationService.closeGameConnections(gameId1)

        assertTrue(emitter3.isCompleted, "Emitter for game 1 should be completed")
        assertTrue(!emitter4.isCompleted, "Emitter for game 2 should NOT be completed")
    }

    @Test
    @Timeout(15, unit = TimeUnit.SECONDS)
    fun `concurrent subscriptions should be thread safe`() {
        val gameId = 100
        val numUsers = 10
        val threads = mutableListOf<Thread>()
        val emitters = mutableListOf<TestSseEmitter>()

        for (i in 1..numUsers) {
            val emitter = TestSseEmitter()
            emitters.add(emitter)

            val thread =
                Thread {
                    gameNotificationService.subscribe(i, gameId, emitter)
                }
            threads.add(thread)
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val event =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId,
                message = "Round started",
            )

        gameNotificationService.notifyGame(gameId, event)

        assertEquals(
            numUsers,
            emitters.count { it.sentEvents.isNotEmpty() },
            "All emitters should receive the event",
        )
    }

    @Test
    fun `event contains correct data`() {
        val gameId = 100
        val userId = 1
        val testData = mapOf("playerId" to 5, "score" to 150)

        val event =
            GameEvent(
                type = GameEventType.PLAYER_ROLLED,
                gameId = gameId,
                message = "Dice roll",
                data = testData,
            )

        gameNotificationService.subscribe(userId, gameId, emitter1)
        gameNotificationService.notifyGame(gameId, event)

        assertTrue(emitter1.sentEvents.isNotEmpty(), "Should have events")
        assertEquals(1, emitter1.sentEvents.size, "Should have exactly one event")
    }

    @Test
    fun `subscribe and immediately unsubscribe`() {
        val gameId = 100
        val userId = 1

        gameNotificationService.subscribe(userId, gameId, emitter1)
        emitter1.complete()

        val event =
            GameEvent(
                type = GameEventType.ROUND_STARTED,
                gameId = gameId,
                message = "Round started",
            )

        gameNotificationService.notifyGame(gameId, event)

        assertTrue(emitter1.sentEvents.isEmpty(), "Should not receive events after unsubscribe")
    }
}
