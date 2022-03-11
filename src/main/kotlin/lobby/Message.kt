package lobby

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class IncomingMessage {
    /**
     * Response to a [OutgoingMessage.Question]
     */
    @Serializable
    @SerialName("response")
    class Response(val id: Int, val choice: Int) : IncomingMessage()

    @Serializable
    @SerialName("getOptions")
    object GetOptions : IncomingMessage()

    sealed class AdminMessage : IncomingMessage() {
        @Serializable
        @SerialName("start")
        object Start : AdminMessage()

        @Serializable
        @SerialName("kick")
        class KickPlayer(val index: Int) : AdminMessage()

        @Serializable
        @SerialName("setOptions")
        class SetOptions(val newOptions: Lobby.Options) : AdminMessage()
    }
}

@Suppress("unused")
@Serializable
sealed class OutgoingMessage {
    @Serializable
    @SerialName("text")
    class Text(val text: String): OutgoingMessage()

    @Serializable
    @SerialName("question")
    class Question(val id: Int, val text: String, val options: List<String>): OutgoingMessage()

    @Serializable
    @SerialName("options")
    class Options(val options: Lobby.Options): OutgoingMessage()
}

