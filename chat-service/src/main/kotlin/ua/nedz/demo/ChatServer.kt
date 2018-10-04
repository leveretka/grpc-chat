package ua.nedz.demo

import io.grpc.Server
import io.grpc.ServerBuilder

fun main(args: Array<String>) {
    val server = ChatServer()
    server.start()
    server.blockUntilShutdown()
}

class ChatServer {
    private lateinit var server: Server

    fun start() {
        val port = 50051
        server = ServerBuilder.forPort(port)
                .addService(ChatServiceImpl())
                .build()
                .start()
        println("Server started!")
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    @Throws(InterruptedException::class)
    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}