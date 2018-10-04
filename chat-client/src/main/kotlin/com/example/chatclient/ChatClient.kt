package com.example.chatclient

import io.grpc.ManagedChannelBuilder
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.stub.ClientCallStreamObserver
import io.grpc.stub.ClientResponseObserver
import io.grpc.stub.StreamObserver
import io.grpc.util.RoundRobinLoadBalancerFactory
import ua.nedz.grpc.*
import java.util.concurrent.ConcurrentLinkedQueue

class ChatClient {

    private var statsTarget: String? = System.getenv("STATS_SERVICE_TARGET")
    private var chatTarget: String? = System.getenv("CHAT_SERVICE_TARGET")
    private var voteTarget: String? = System.getenv("VOTE_SERVICE_TARGET")
    private val dnsNameResolverProvider = DnsNameResolverProvider()
    private val loadBalancer = RoundRobinLoadBalancerFactory.getInstance()

    init {
        if (statsTarget.isNullOrEmpty())
            statsTarget = "localhost:50061"
        if (chatTarget.isNullOrEmpty())
            chatTarget = "localhost:50051"
        if (voteTarget.isNullOrEmpty())
            voteTarget = "localhost:50071"

    }

    private val chatChannel = ManagedChannelBuilder
            .forTarget(chatTarget)
            .nameResolverFactory(dnsNameResolverProvider)
            .loadBalancerFactory(loadBalancer)
            .useTransportSecurity()
            .usePlaintext(true)
            .build()

    private val chatStub = ChatServiceGrpc.newStub(chatChannel)

    private val statsChannel = ManagedChannelBuilder
            .forTarget(statsTarget)
            .nameResolverFactory(dnsNameResolverProvider)
            .loadBalancerFactory(loadBalancer)
            .usePlaintext(true)
            .build()

    private val statsStub = StatsServiceGrpc.newStub(statsChannel)

    private val voteChannel = ManagedChannelBuilder
            .forTarget(voteTarget)
            .nameResolverFactory(dnsNameResolverProvider)
            .loadBalancerFactory(loadBalancer)
            .usePlaintext(true)
            .build()

    private val voteStub = VoteServiceGrpc.newFutureStub(voteChannel)

    private val messages = ConcurrentLinkedQueue<ChatProto.ChatMessage>()


    fun addMessage(name: String, content: String) =
            messages.add(ChatProto.ChatMessage.newBuilder()
                    .setFrom(name)
                    .setContent(content)
                    .build())

    fun chat(onNext: (ChatProto.ChatMessage) -> Unit) =
            chatStub.chat(streamObserverWithOnNext(onNext))

    fun join(name: String, onNext: (StatsProto.Statistics) -> Unit) {
        val request = StatsProto.JoinRequest.newBuilder().setName(name).build()
        statsStub.join(request, streamObserverWithOnNext(onNext))
    }

    fun vote(id: Long, name: String) {
        val voteRequest = VoteProto.VoteRequest.newBuilder().setRecordId(id).setVoterName(name).build()
        voteStub.vote(voteRequest)
    }

    private fun <T> streamObserverWithOnNext(onNext: (T) -> Unit) =
            object : StreamObserver<T> {
                override fun onNext(t: T) =
                        onNext(t)
                override fun onError(throwable: Throwable) {}
                override fun onCompleted() {}
            }

    private fun clientResponseObserver(action: (ChatProto.ChatMessage) -> Unit) =
            object : ClientResponseObserver<ChatProto.ChatMessage, ChatProto.ChatMessage> {
        override fun onError(t: Throwable?) {}

        override fun onCompleted() {}

        lateinit var requestStream: ClientCallStreamObserver<ChatProto.ChatMessage>

        override fun beforeStart(requestStream: ClientCallStreamObserver<ChatProto.ChatMessage>) {
            this.requestStream = requestStream
            requestStream.disableAutoInboundFlowControl()

            requestStream.setOnReadyHandler {
                while (requestStream.isReady) {
                    if (messages.isNotEmpty()) {
                        // Send more messages if there are more messages to send.
                        requestStream.onNext(messages.poll())
                    } else {
                        // Signal completion if there is nothing left to send.
                        requestStream.onCompleted()
                    }
                }
            }
        }

        override fun onNext(value: ChatProto.ChatMessage) {
            action(value)
            requestStream.request(1)
        }
    }

}