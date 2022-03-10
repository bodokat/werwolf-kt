package lobby

import kotlinx.serialization.Serializable

sealed interface IncomingMessage {
    /**
     * Response to a [OutgoingMessage.Question]
     */
    @Serializable
    class Response(val id: Int, val choice: Int) : IncomingMessage

    @Serializable
    object GetOptions : IncomingMessage

    sealed interface AdminMessage : IncomingMessage {
        @Serializable
        object Start : AdminMessage

        @Serializable
        class KickPlayer(val index: Int) : AdminMessage

        @Serializable
        class SetOptions(val newOptions: Lobby.Options) : AdminMessage
    }
}

@Suppress("unused")
sealed interface OutgoingMessage {
    @Serializable
    class Text(val text: String): OutgoingMessage

    @Serializable
    class Question(val id: Int, val text: String, val options: List<String>): OutgoingMessage

    @Serializable
    class Options(val options: Lobby.Options): OutgoingMessage
}

