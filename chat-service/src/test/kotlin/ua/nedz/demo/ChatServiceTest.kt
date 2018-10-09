package ua.nedz.demo

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import ua.nedz.grpc.ChatProto
import ua.nedz.grpc.ChatProto.ChatMessage
import ua.nedz.grpc.ChatServiceGrpc

@RunWith(JUnit4::class)
class ChatServiceTest {

    lateinit var server: Server
    lateinit var inProcessChannel: ManagedChannel

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val serverName = InProcessServerBuilder.generateName()
        server = InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(ManualFlowChatServiceImpl())
                .build().start()

        inProcessChannel = InProcessChannelBuilder.forName(serverName).directExecutor().build()

    }

    @After
    fun tearDown() {
        inProcessChannel.shutdown()
        server.shutdown()
    }


    /**
     * To test the server, make calls with a real stub using the in-process channel, and verify
     * behaviors or state changes from the client side.
     */
    @Test
    fun chatTest() {
        val stub = ChatServiceGrpc.newStub(inProcessChannel)
        val responseObserver: StreamObserver<ChatProto.ChatMessage> = mock {  }
        val requestObserver = stub.chat(responseObserver)
        val chatMessage = ChatMessage.newBuilder().setFrom("Margo").setContent("Hello!").build()
        val chatMessageCaptor: ArgumentCaptor<ChatProto.ChatMessage> =
                ArgumentCaptor.forClass(ChatMessage::class.java)

        requestObserver.onNext(chatMessage)

        chatMessageCaptor.run {
            verify(responseObserver).onNext(capture())
            assertEquals("Margo", value.from)
            assertEquals("Hello!", value.content)
        }
    }
}
