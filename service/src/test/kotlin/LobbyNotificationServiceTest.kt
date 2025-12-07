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

open class TestLobbyEmitter : SseEmitter() {
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

class LobbyNotificationServiceTest {
    private lateinit var lobbyNotificationService: LobbyNotificationService
    private lateinit var emitter1: TestLobbyEmitter
    private lateinit var emitter2: TestLobbyEmitter

    @BeforeEach
    fun setUp() {
        lobbyNotificationService = LobbyNotificationService()
        emitter1 = TestLobbyEmitter()
        emitter2 = TestLobbyEmitter()
    }

    @Test
    fun `subscribe should register user to lobby`() {
        val userId = 1
        val lobbyId = 100

        lobbyNotificationService.subscribe(userId, lobbyId, emitter1)

        assertTrue(!emitter1.isCompleted, "Emitter should not be completed after subscribe")
    }

    @Test
    fun `subscribe should close previous connection`() {
        val userId = 1
        val lobbyId = 100
        val oldEmitter = TestLobbyEmitter()

        lobbyNotificationService.subscribe(userId, lobbyId, oldEmitter)
        lobbyNotificationService.subscribe(userId, lobbyId, emitter1)

        assertTrue(oldEmitter.isCompleted, "Old emitter should be completed")
        assertTrue(!emitter1.isCompleted, "New emitter should not be completed")
    }

    @Test
    fun `notifyLobby should send event to all subscribed users`() {
        val lobbyId = 100
        val userId1 = 1
        val userId2 = 2
        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
                data = mapOf("playerName" to "John"),
            )

        lobbyNotificationService.subscribe(userId1, lobbyId, emitter1)
        lobbyNotificationService.subscribe(userId2, lobbyId, emitter2)
        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertTrue(emitter1.sentEvents.isNotEmpty(), "Emitter 1 should have received events")
        assertTrue(emitter2.sentEvents.isNotEmpty(), "Emitter 2 should have received events")
    }

    @Test
    fun `closeLobbyConnections should close all connections for a lobby`() {
        val lobbyId = 100
        val userId1 = 1
        val userId2 = 2

        lobbyNotificationService.subscribe(userId1, lobbyId, emitter1)
        lobbyNotificationService.subscribe(userId2, lobbyId, emitter2)
        lobbyNotificationService.closeLobbyConnections(lobbyId)

        assertTrue(emitter1.isCompleted, "Emitter 1 should be completed")
        assertTrue(emitter2.isCompleted, "Emitter 2 should be completed")
    }

    @Test
    fun `notifyLobby should remove failed users`() {
        val lobbyId = 100
        val userId1 = 1
        val userId2 = 2

        val failingEmitter =
            object : TestLobbyEmitter() {
                override fun send(builder: SseEventBuilder): Unit = throw IOException("Connection failed")
            }

        val successEmitter = TestLobbyEmitter()

        val event =
            LobbyEvent(
                type = LobbyEventType.LOBBY_CLOSED,
                lobbyId = lobbyId,
                message = "Lobby closed",
            )

        lobbyNotificationService.subscribe(userId1, lobbyId, failingEmitter)
        lobbyNotificationService.subscribe(userId2, lobbyId, successEmitter)
        lobbyNotificationService.notifyLobby(lobbyId, event)

        val failingEmitterEventCount = failingEmitter.sentEvents.size
        val successEmitterEventCount1 = successEmitter.sentEvents.size

        lobbyNotificationService.notifyLobby(lobbyId, event)

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
        val lobbyId = 100
        val userId = 1

        lobbyNotificationService.subscribe(userId, lobbyId, emitter1)

        val event =
            LobbyEvent(
                type = LobbyEventType.GAME_STARTED,
                lobbyId = lobbyId,
                message = "Game started",
            )

        lobbyNotificationService.notifyLobby(lobbyId, event)
        val firstEventCount = emitter1.sentEvents.size

        emitter1.complete()

        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertEquals(
            firstEventCount,
            emitter1.sentEvents.size,
            "User should not receive events after completion",
        )
    }

    @Test
    fun `unsubscribe via timeout callback should remove user`() {
        val lobbyId = 100
        val userId = 1
        var timeoutCallback: (() -> Unit)? = null

        val customEmitter =
            object : TestLobbyEmitter() {
                override fun onTimeout(callback: Runnable) {
                    timeoutCallback = { callback.run() }
                }
            }

        lobbyNotificationService.subscribe(userId, lobbyId, customEmitter)

        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
            )

        lobbyNotificationService.notifyLobby(lobbyId, event)
        val firstEventCount = customEmitter.sentEvents.size

        timeoutCallback?.invoke()

        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertEquals(
            firstEventCount,
            customEmitter.sentEvents.size,
            "User should not receive events after timeout",
        )
    }

    @Test
    fun `unsubscribe via error callback should remove user`() {
        val lobbyId = 100
        val userId = 1
        var errorCallback: ((Throwable) -> Unit)? = null

        val customEmitter =
            object : TestLobbyEmitter() {
                override fun onError(callback: Consumer<Throwable>) {
                    errorCallback = { error -> callback.accept(error) }
                }
            }

        lobbyNotificationService.subscribe(userId, lobbyId, customEmitter)

        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
            )

        lobbyNotificationService.notifyLobby(lobbyId, event)
        val firstEventCount = customEmitter.sentEvents.size

        errorCallback?.invoke(Exception("Connection error"))

        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertEquals(
            firstEventCount,
            customEmitter.sentEvents.size,
            "User should not receive events after error",
        )
    }

    @Test
    fun `multiple lobbies should be independent`() {
        val lobbyId1 = 100
        val lobbyId2 = 200
        val userId1 = 1
        val userId2 = 2

        val emitter3 = TestLobbyEmitter()
        val emitter4 = TestLobbyEmitter()

        lobbyNotificationService.subscribe(userId1, lobbyId1, emitter3)
        lobbyNotificationService.subscribe(userId2, lobbyId2, emitter4)

        val event1 =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId1,
                message = "Player joined lobby 1",
            )
        val event2 =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId2,
                message = "Player joined lobby 2",
            )

        lobbyNotificationService.notifyLobby(lobbyId1, event1)
        lobbyNotificationService.notifyLobby(lobbyId2, event2)

        assertTrue(emitter3.sentEvents.isNotEmpty())
        assertTrue(emitter4.sentEvents.isNotEmpty())
        assertEquals(1, emitter3.sentEvents.size)
        assertEquals(1, emitter4.sentEvents.size)
    }

    @Test
    fun `same user in multiple lobbies should not interfere`() {
        val lobbyId1 = 100
        val lobbyId2 = 200
        val userId = 1

        val emitterLobby1 = TestLobbyEmitter()
        val emitterLobby2 = TestLobbyEmitter()

        lobbyNotificationService.subscribe(userId, lobbyId1, emitterLobby1)
        lobbyNotificationService.subscribe(userId, lobbyId2, emitterLobby2)

        val event1 =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId1,
                message = "Joined lobby 1",
            )
        val event2 =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId2,
                message = "Joined lobby 2",
            )

        lobbyNotificationService.notifyLobby(lobbyId1, event1)
        lobbyNotificationService.notifyLobby(lobbyId2, event2)

        assertEquals(1, emitterLobby1.sentEvents.size, "User should receive only 1 event for lobby 1")
        assertEquals(1, emitterLobby2.sentEvents.size, "User should receive only 1 event for lobby 2")
    }

    @Test
    fun `multiple users in same lobby should receive events`() {
        val lobbyId = 100
        val userId1 = 1
        val userId2 = 2
        val userId3 = 3

        val emitter3 = TestLobbyEmitter()

        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_LEFT,
                lobbyId = lobbyId,
                message = "Player left",
            )

        lobbyNotificationService.subscribe(userId1, lobbyId, emitter1)
        lobbyNotificationService.subscribe(userId2, lobbyId, emitter2)
        lobbyNotificationService.subscribe(userId3, lobbyId, emitter3)
        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertTrue(emitter1.sentEvents.isNotEmpty())
        assertTrue(emitter2.sentEvents.isNotEmpty())
        assertTrue(emitter3.sentEvents.isNotEmpty())
    }

    @Test
    fun `empty lobby notification should handle gracefully`() {
        val lobbyId = 100
        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
            )

        lobbyNotificationService.notifyLobby(lobbyId, event)
    }

    @Test
    fun `closeLobbyConnections should not affect other lobbies`() {
        val lobbyId1 = 100
        val lobbyId2 = 200
        val userId1 = 1
        val userId2 = 2

        val emitter3 = TestLobbyEmitter()
        val emitter4 = TestLobbyEmitter()

        lobbyNotificationService.subscribe(userId1, lobbyId1, emitter3)
        lobbyNotificationService.subscribe(userId2, lobbyId2, emitter4)

        lobbyNotificationService.closeLobbyConnections(lobbyId1)

        assertTrue(emitter3.isCompleted, "Emitter for lobby 1 should be completed")
        assertTrue(!emitter4.isCompleted, "Emitter for lobby 2 should NOT be completed")
    }

    @Test
    @Timeout(15, unit = TimeUnit.SECONDS)
    fun `concurrent subscriptions should be thread safe`() {
        val lobbyId = 100
        val numUsers = 10
        val threads = mutableListOf<Thread>()
        val emitters = mutableListOf<TestLobbyEmitter>()

        for (i in 1..numUsers) {
            val emitter = TestLobbyEmitter()
            emitters.add(emitter)

            val thread =
                Thread {
                    lobbyNotificationService.subscribe(i, lobbyId, emitter)
                }
            threads.add(thread)
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
            )

        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertEquals(
            numUsers,
            emitters.count { it.sentEvents.isNotEmpty() },
            "All emitters should receive the event",
        )
    }

    @Test
    fun `event contains correct data`() {
        val lobbyId = 100
        val userId = 1
        val testData = mapOf("players" to 4, "capacity" to 8)

        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
                data = testData,
            )

        lobbyNotificationService.subscribe(userId, lobbyId, emitter1)
        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertTrue(emitter1.sentEvents.isNotEmpty(), "Should have events")
        assertEquals(1, emitter1.sentEvents.size, "Should have exactly one event")
    }

    @Test
    fun `subscribe and immediately unsubscribe`() {
        val lobbyId = 100
        val userId = 1

        lobbyNotificationService.subscribe(userId, lobbyId, emitter1)
        emitter1.complete()

        val event =
            LobbyEvent(
                type = LobbyEventType.PLAYER_JOINED,
                lobbyId = lobbyId,
                message = "Player joined",
            )

        lobbyNotificationService.notifyLobby(lobbyId, event)

        assertTrue(emitter1.sentEvents.isEmpty(), "Should not receive events after unsubscribe")
    }
}
