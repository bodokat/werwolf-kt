import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lobby.OutgoingMessage
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class User(val name: String, private val session: DefaultWebSocketSession) {
    override fun toString() = name

    private val choiceID = AtomicInteger(0)

    private val outstandingChoices: MutableMap<Int, CompletableDeferred<Int>> = Collections.synchronizedMap(HashMap())

    suspend fun send(text: String) {
        session.send(text)
    }

    fun response(id: Int, choice: Int) {
        outstandingChoices.remove(id)?.complete(choice)
    }

    suspend fun <T> choice(text: String,options: List<T>): Int {
        val id = choiceID.getAndIncrement()
        val message: OutgoingMessage = OutgoingMessage.Question(id,text, options.map { it.toString() } )
        val deferred = CompletableDeferred<Int>()
        outstandingChoices[id] = deferred
        session.send(Json.encodeToString(message))

        return deferred.await()
    }
}
