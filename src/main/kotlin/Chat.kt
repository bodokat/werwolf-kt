import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

fun Route.mainChatRoute() {
    val connections: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())
    val lastUserID = AtomicInteger(0)
    webSocket("/chat") {
        val name = call.parameters["name"] ?: "user${lastUserID.getAndIncrement()}"
        val thisConnection = Connection(this, name)
        connections.add(thisConnection)
        try {
            connections.map { async { it.session.send("Welcome, ${thisConnection.name}! There are ${connections.size} users currently connected") } }.map { it.await() }
            incoming.consumeAsFlow()
                .mapNotNull { it as? Frame.Text }
                .map { it.readText() }
                .map { "[${thisConnection.name}]: $it" }
                .map { message -> connections.map { connection -> async { connection.session.send(message) } }.map { it.await() } }
        } catch (e: Exception) {
            println(e.message)
        } finally {
            println("removing $thisConnection")
            connections.map { async { it.session.send("Goodbye, ${thisConnection.name}") } }.map { it.await() }
            connections.remove(thisConnection)
        }
    }
}

fun Route.chatRoute() {
    val chatrooms: MutableMap<String?, Chatroom> = Collections.synchronizedMap(HashMap())
    webSocket("/chat/{name}") {
        val thisConnection = Connection(this, "nobody")
        val name = call.parameters["name"]
        val chatroom = chatrooms.computeIfAbsent(name) { Chatroom() }
        try {
            chatroom.add(thisConnection)
            chatroom.send("Welcome, ${thisConnection.name}! There are ${chatroom.connections.size} users currently connected.")
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val received = frame.readText()
                val textWithName = "[${thisConnection.name}]: $received"
                chatroom.send(textWithName)
            }
        } catch (e: Exception) {
            println(e.message)
        } finally {
            println("removing $thisConnection!")
            chatroom.send("Goodbye, ${thisConnection.name}!")
            chatroom.remove(thisConnection)
            if (chatroom.connections.isEmpty()) {
                chatrooms.remove(name)
            }
        }
    }
}

class Chatroom {
    val connections: MutableSet<Connection> = Collections.synchronizedSet(LinkedHashSet())

    fun add(connection: Connection) {
        connections.add(connection)
    }

    fun remove(connection: Connection) {
        connections.remove(connection)
    }

    suspend fun send(message: String) = coroutineScope {
        connections.map { async {it.session.send(message)} }.map { it.await() }
    }
}

data class Connection(val session: DefaultWebSocketSession, val name: String)