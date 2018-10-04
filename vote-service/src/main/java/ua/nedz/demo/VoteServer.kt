package ua.nedz.demo

import io.grpc.Server
import io.grpc.ServerBuilder

fun main(args: Array<String>) {
    val server = VoteServer()
    server.start()
    server.blockUntilShutdown()
}

class VoteServer {
    private lateinit var server: Server

    fun start() {
        val port = 50071
        server = ServerBuilder.forPort(port)
                .addService(VoteServiceImpl())
                .build()
                .start()
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    @Throws(InterruptedException::class)
    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}