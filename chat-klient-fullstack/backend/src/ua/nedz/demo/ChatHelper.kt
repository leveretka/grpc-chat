package ua.nedz.demo

import io.grpc.ManagedChannelBuilder
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.stub.StreamObserver
import io.grpc.util.RoundRobinLoadBalancerFactory
import ua.nedz.grpc.*

class ChatHelper {

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
                override fun onError(throwable: Throwable) {
                    throwable.printStackTrace()
                }
                override fun onCompleted() {}
            }
}