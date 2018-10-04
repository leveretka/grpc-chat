package ua.nedz.demo

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptors

fun main(args: Array<String>) {
    val server = StatsServer()
    server.start()
    server.blockUntilShutdown()
}

class StatsServer {
    private lateinit var server: Server

    fun start() {

        // First add interceptor
        val port = 50061
        server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(StatsServiceImpl(), HeaderServerInterceptor()))
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