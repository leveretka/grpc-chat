package ua.nedz.demo

import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.internal.DnsNameResolverProvider
import io.grpc.stub.StreamObserver
import io.grpc.util.RoundRobinLoadBalancerFactory
import ua.nedz.grpc.StatsProto
import ua.nedz.grpc.StatsServiceGrpc
import ua.nedz.grpc.VoteProto
import ua.nedz.grpc.VoteServiceGrpc
import java.util.concurrent.ConcurrentHashMap


class VoteServiceImpl : VoteServiceGrpc.VoteServiceImplBase() {

    private var statsChannel: ManagedChannel
    private val votes = ConcurrentHashMap<String, MutableList<Long>>()
    var target: String? = System.getenv("STATS_SERVICE_TARGET")
    init {
        if (target.isNullOrEmpty()) {
            target = "localhost:50061"
        }
        statsChannel = ManagedChannelBuilder
                .forTarget(target)
                .nameResolverFactory(DnsNameResolverProvider())
                .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
                .usePlaintext(true)
                .build()

        ClientInterceptors.intercept(statsChannel, HeaderClientInterceptor())
    }

    private val statsStub = StatsServiceGrpc.newFutureStub(statsChannel)


    override fun vote(request: VoteProto.VoteRequest, responseObserver: StreamObserver<VoteProto.VoteResponse>?) {
        votes.putIfAbsent(request.voterName, mutableListOf())
        val userVotes = votes[request.voterName]!!
        val firstVoted = !userVotes.contains(request.recordId)
        if (firstVoted) {
            val addVoteRequest = StatsProto.AddVoteRequest.newBuilder()
                    .setRecordId(request.recordId)
                    .setVoterName(request.voterName)
                    .build()
            statsStub.addVote(addVoteRequest)
            userVotes.add(request.recordId)
        }
        val response = VoteProto.VoteResponse.newBuilder().setResult(firstVoted).build()
        responseObserver?.onNext(response)
    }
}