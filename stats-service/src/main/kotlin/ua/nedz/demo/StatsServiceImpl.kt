package ua.nedz.demo

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import ua.nedz.grpc.StatsProto
import ua.nedz.grpc.StatsServiceGrpc
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class StatsServiceImpl : StatsServiceGrpc.StatsServiceImplBase() {
    private val listeners = mutableSetOf<StreamObserver<StatsProto.Statistics>>()
    private val votes = ConcurrentHashMap<Long, AtomicLong>()
    private val records = ConcurrentHashMap<Long, Record>()
    private val counter = AtomicLong(1000L)

    override fun join(request: StatsProto.JoinRequest, responseObserver: StreamObserver<StatsProto.Statistics>) {
        // 1. Add to Listeners
        // 2. Notify new client

        listeners.add(responseObserver)
        notifyClients(responseObserver)
    }

    override fun addRecord(request: StatsProto.Record, responseObserver: StreamObserver<StatsProto.AddRecordResponse>) {
        // 1. Add id to a new record
        // 2. Put record in map with key id
        // 3. Put record in votes with 0 value
        // 4. Notify all clients
        // 5. Return result

        val record = Record(counter.incrementAndGet(), request.author, request.content)
        records[record.id] = record
        votes[record.id] = AtomicLong(0L)
        notifyClients(*listeners.toTypedArray())
        responseObserver.onNext(StatsProto.AddRecordResponse
                .newBuilder()
                .setResult(true)
                .build())
    }

    override fun addVote(request: StatsProto.AddVoteRequest, responseObserver: StreamObserver<StatsProto.Record>) {
        // 1. Get record by id
        // 2. Check if is not self vote
        // 3. Increment votes
        // 4. Notify all clients

        val record = records[request.recordId]
        record?.let {
            if (record.author != request.voterName)
                votes[record.id]?.incrementAndGet()

            responseObserver.onNext(StatsProto.Record.newBuilder()
                    .setId(it.id)
                    .setAuthor(it.author)
                    .setContent(it.content)
                    .build())
            notifyClients(*listeners.toTypedArray())
        }
    }

    private fun notifyClients(vararg clients: StreamObserver<StatsProto.Statistics>) {
        val builder = StatsProto.Statistics.newBuilder()
        records.forEach { id: Long, r: Record ->
            builder.addRecord(
                    StatsProto.Record.newBuilder()
                            .setId(id)
                            .setAuthor(r.author)
                            .setContent(r.content)
                            .setVotes(votes[id]?.get() ?: 0L))
        }
        clients.forEach {
            try {
                it.onNext(builder.build())
            } catch (e: StatusRuntimeException) {
            }
        }
    }

    data class Record(val id: Long, val author: String, val content: String)
}
