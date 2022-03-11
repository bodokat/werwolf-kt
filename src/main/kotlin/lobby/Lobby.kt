package lobby

import User
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import roles.Dorfbewohner
import roles.Role
import roles.Werwolf
import startGame
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class Lobby {
    val users: MutableList<User> = Collections.synchronizedList(mutableListOf())
    val roles: MutableList<Role> = Collections.synchronizedList(mutableListOf(Dorfbewohner,Dorfbewohner,Dorfbewohner,Werwolf))


    var running = AtomicBoolean(false)

    fun canStart(): Boolean =
        users.size < roles.size


    suspend fun send(message: String) {
        users.forEach { it.send(message) }
    }

    var options: Options = Options()

    @Serializable
    class Options {
        
    }



    suspend fun handleMessage(message: IncomingMessage, user: User) {
        if (message is IncomingMessage.AdminMessage)  {
            if (user != users.first()) {
                return
            } else {
                when (message) {
                    is IncomingMessage.AdminMessage.SetOptions -> this.options = message.newOptions
                    is IncomingMessage.AdminMessage.KickPlayer -> this.users.removeAt(message.index)
                    is IncomingMessage.AdminMessage.Start -> if (canStart()) startGame()
                }
            }
        } else when (message) {
            is IncomingMessage.GetOptions -> user.send(OutgoingMessage.Options(options).let { Json.encodeToString(it) })
            is IncomingMessage.Response -> user.response(message.id,message.choice)
            else -> {} // TODO: remove when warning is fixed
        }
    }
}

fun Route.werwolfRoute() {
    val lobbies: MutableMap<String?, Lobby> = Collections.synchronizedMap(HashMap())
    val lastUserID = AtomicInteger(0)

    webSocket("/werwolf/{lobby}") {
        val lobbyName = call.parameters["lobby"]
        val lobby = lobbies.computeIfAbsent(lobbyName) { Lobby() }
        val userName = call.request.queryParameters["name"] ?: "user${lastUserID.getAndIncrement()}"
        val user = User(userName, this)
        try {
            lobby.users.add(user)
            lobby.send("Welcome, ${user.name}!")
            incoming.receiveAsFlow()
                .mapNotNull { it as Frame.Text }
                .map {
                    val msg = it.readText()
                    println("Got message: $msg")
                    try {
                        val decoded = Json.decodeFromString<IncomingMessage>(msg)
                        println("Decoded: $decoded")
                        return@map decoded
                    } catch (e: SerializationException) {
                        println("Error while decoding message: $e")
                        return@map null
                    }
                }
                .filterNotNull()
                .map { launch { lobby.handleMessage(it, user) } }
                .collect()
        } catch (e: Exception) {
            println("Exception: $e (${e.message})")
        } finally {
            println("removing $user!")
            lobby.send("Goodbye, ${user.name}!")
            lobby.users.remove(user)
            if (lobby.users.isEmpty()) {
                lobbies.remove(lobbyName)
            }
        }
    }
}