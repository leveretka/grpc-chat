package ua.nedz.demo

import io.grpc.ManagedChannelBuilder
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.stub.StreamObserver
import io.grpc.util.RoundRobinLoadBalancerFactory
import io.netty.util.internal.ConcurrentSet
import ua.nedz.grpc.ChatProto
import ua.nedz.grpc.ChatServiceGrpc
import ua.nedz.grpc.StatsProto
import ua.nedz.grpc.StatsServiceGrpc


class ChatServiceImpl : ChatServiceGrpc.ChatServiceImplBase() {
    var target: String? = System.getenv("STATS_SERVICE_TARGET")

    init {
        if (target.isNullOrEmpty()) {
            target = "localhost:50061"
        }
    }


    private val clients = ConcurrentSet<StreamObserver<ChatProto.ChatMessage>>()
    private val statsChannel = ManagedChannelBuilder
            .forTarget(target)
            .nameResolverFactory(DnsNameResolverProvider())
            .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
            .usePlaintext(true)
            .build()
    private val statsStub = StatsServiceGrpc.newFutureStub(statsChannel)

    override fun chat(client: StreamObserver<ChatProto.ChatMessage>): StreamObserver<ChatProto.ChatMessage> {
        // 1. Add client to listeners
        // 2. return streamObserver wit proper onNext
        // 3. When implementing second step add message to statistics


        clients.add(client)
        return object : StreamObserver<ChatProto.ChatMessage> {
            override fun onNext(msg: ChatProto.ChatMessage) {
                println("${msg.from}: ${msg.content}")
                clients.forEach { try { it.onNext(msg)} finally { }}

                if (msg.content.startsWith("gRPC")) {
                    val record: StatsProto.Record = StatsProto.Record.newBuilder()
                            .setAuthor(msg.from)
                            .setContent(msg.content)
                            .build()

                    statsStub.addRecord(record)

                }
            }

            override fun onCompleted() {}

            override fun onError(t: Throwable?) {}

        }
    }

    private fun notifyClient(client: StreamObserver<ChatProto.ChatMessage>, vararg messages: ChatProto.ChatMessage)
            = messages.forEach { client.onNext(it) }

}