package ua.nedz.demo

import io.grpc.stub.StreamObserver
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.runBlocking
import ua.nedz.grpc.ChatProto
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class ChatServer {
    val usersCounter = AtomicInteger()
    val memberNames = ConcurrentHashMap<String, String>()
    val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()
    val lastMessages = LinkedList<String>()
    val chatHelper = ChatHelper()

    lateinit var observer: StreamObserver<ChatProto.ChatMessage>

    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        val name = memberNames.computeIfAbsent(member) { "user${usersCounter.incrementAndGet()}" }
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

        chatHelper.join(name, {})
        observer = chatHelper.chat {
            runBlocking {  sendToMe(socket, it) }
        }

        if (list.size == 1) {
            broadcast("server", "Member joined: $name.")
        }

        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }


    }

    suspend fun memberLeft(member: String, socket: WebSocketSession) {
        val connections = members[member]
        connections?.remove(socket)

        if (connections != null && connections.isEmpty()) {
            val name = memberNames.remove(member) ?: member
            broadcast("server", "Member left: $name.")
        }
    }

    fun message(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        observer.onNext(ChatProto.ChatMessage.newBuilder().setFrom(name).setContent(message).build())

        val formatted = "$name: $message"

        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    private suspend fun broadcast(message: String) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

    private suspend fun broadcast(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        broadcast("[$name] $message")
    }

    private suspend fun sendToMe (socket: WebSocketSession, message: ChatProto.ChatMessage) {
        socket.send(Frame.Text("${message.from}: ${message.content}"))
    }

    suspend fun List<WebSocketSession>.send(frame: Frame) {
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
                } catch (ignore: ClosedSendChannelException) {
                    // at some point it will get closed
                }
            }
        }
    }
}