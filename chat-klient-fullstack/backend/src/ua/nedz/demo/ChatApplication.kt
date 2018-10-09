package ua.nedz.demo

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.experimental.channels.*
import java.time.*

//fun main(args: Array<String>) {
//    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
//}

fun Application.main() {
    ChatApplication().apply { main() }
}

class ChatApplication {

    private val server = ChatServer()

    fun Application.main() {

        install(DefaultHeaders)
        install(CallLogging)
        install(WebSockets) {
            pingPeriod = Duration.ofMinutes(1)
        }
        install(Sessions) {
            cookie<ChatSession>("SESSION")
        }
        intercept(ApplicationCallPipeline.Features) {
            if (call.sessions.get<ChatSession>() == null) {
                call.sessions.set(ChatSession(nextNonce()))
            }
        }

        routing {
            webSocket("/chat") { // this: WebSocketSession ->
                val session = call.sessions.get<ChatSession>()
                if (session == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
                }
                server.memberJoin(session.id, this)

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receivedMessage(session.id, frame.readText())
                        }
                    }
                } finally {
                    server.memberLeft(session.id, this)
                }
            }

            // This defines a block of static resources for the '/' path (since no path is specified and we start at '/')
            static {
                // This marks index.html from the 'web' folder in resources as the default file to serve.
                defaultResource("index.html", "web")
                // This serves files from the 'web' folder in the application resources.
                resources("web")
            }

        }
    }

    data class ChatSession(val id: String)

    private suspend fun receivedMessage(id: String, command: String) =
            server.message(id, command)

}