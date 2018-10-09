package ua.nedz.demo

import io.grpc.*

import java.io.PrintWriter
import java.io.StringWriter
import java.util.HashSet

class UnknownStatusInterceptor(throwables: Collection<Class<out Throwable>>) : ServerInterceptor {
    private val throwables = HashSet<Class<out Throwable>>()

    init {
        this.throwables.addAll(throwables)
    }

    override fun <ReqT, RespT> interceptCall(call: ServerCall<ReqT, RespT>,
                                             headers: Metadata, next: ServerCallHandler<ReqT, RespT>)
            : ServerCall.Listener<ReqT> {
        val wrappedCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

            override fun close(status: Status, trailers: Metadata) {
                var status = status
                if (status.code == Status.Code.UNKNOWN
                        && status.description == null
                        && status.cause != null
                        && throwables.contains(status.cause!!.javaClass)) {
                    val t = status.cause
                    status = Status.INTERNAL
                            .withDescription(t!!.message)
                            .augmentDescription(stacktraceToString(t))
                }
                super.close(status, trailers)
            }
        }
        return next.startCall(wrappedCall, headers)
    }

    private fun stacktraceToString(e: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}