package ua.nedz.demo

import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import io.netty.util.internal.ConcurrentSet
import ua.nedz.grpc.ChatProto
import ua.nedz.grpc.ChatServiceGrpc
import java.util.concurrent.atomic.AtomicBoolean

class ManualFlowChatServiceImpl: ChatServiceGrpc.ChatServiceImplBase() {

    private val clients = ConcurrentSet<StreamObserver<ChatProto.ChatMessage>>()

    override fun chat(client: StreamObserver<ChatProto.ChatMessage>): StreamObserver<ChatProto.ChatMessage> {
        clients.add(client)

        val serverCallStreamObserver =
                client as ServerCallStreamObserver<ChatProto.ChatMessage>
        //1.
        serverCallStreamObserver.disableAutoInboundFlowControl()

        val wasReady = AtomicBoolean(false)

        //2.
        with (serverCallStreamObserver) {
            setOnReadyHandler {
                if (isReady && wasReady.compareAndSet(false, true))
                    //3.
                    request(1)
            }
        }

        return object : StreamObserver<ChatProto.ChatMessage> {
            override fun onNext(msg: ChatProto.ChatMessage) {
                println("${msg.from}: ${msg.content}")
                clients.forEach {
                    it.onNext(msg)
                }

                with (serverCallStreamObserver) {
                    if (isReady)
                        //3.
                        request(1)
                    else
                        wasReady.set(false)
                }
            }

            override fun onCompleted() {}

            override fun onError(t: Throwable?) {}

        }
    }


}