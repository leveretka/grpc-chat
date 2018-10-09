package ua.nedz.demo

import io.grpc.ManagedChannelBuilder
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import io.grpc.stub.StreamObserver
import io.grpc.util.RoundRobinLoadBalancerFactory
import ua.nedz.grpc.*
import java.util.concurrent.ConcurrentLinkedQueue

class ChatClient {

    private var chatTarget: String? = System.getenv("CHAT_SERVICE_TARGET")
    private val dnsNameResolverProvider = DnsNameResolverProvider()
    private val loadBalancer = RoundRobinLoadBalancerFactory.getInstance()

    init {
        if (chatTarget.isNullOrEmpty())
            chatTarget = "localhost:50051"

    }

    private val chatChannel = ManagedChannelBuilder
            .forTarget(chatTarget)
            .nameResolverFactory(dnsNameResolverProvider)
            .loadBalancerFactory(loadBalancer)
            .useTransportSecurity()
            .usePlaintext(true)
            .build()

    private val chatStub = ChatServiceGrpc.newStub(chatChannel)

    private val messages = ConcurrentLinkedQueue<ChatProto.ChatMessage>()


    fun addMessage(name: String, content: String) =
            messages.add(ChatProto.ChatMessage.newBuilder()
                    .setFrom(name)
                    .setContent(content)
                    .build())

    fun chat(onNext: (ChatProto.ChatMessage) -> Unit) =
            chatStub.chat(clientResponseObserver(onNext))

    private fun clientResponseObserver(action: (ChatProto.ChatMessage) -> Unit) =
            object : ClientResponseObserver<ChatProto.ChatMessage, ChatProto.ChatMessage> {
        override fun onError(t: Throwable?) {}

        override fun onCompleted() {}

        lateinit var requestStream: ClientCallStreamObserver<ChatProto.ChatMessage>

        override fun beforeStart(stream: ClientCallStreamObserver<ChatProto.ChatMessage>) {
            requestStream = stream
            //1.
            requestStream.disableAutoInboundFlowControl()
            //2.
            requestStream.setOnReadyHandler {
                while (requestStream.isReady) {
                    if (messages.isNotEmpty()) {
                        // Send more messages if there are more messages to send.
                        requestStream.onNext(messages.poll())
                        //requestStream.request(1)
                    } else
                        requestStream.onCompleted()
                }
            }
        }

        override fun onNext(value: ChatProto.ChatMessage) {
            action(value)
            //3.
            requestStream.request(1)
        }
    }

}