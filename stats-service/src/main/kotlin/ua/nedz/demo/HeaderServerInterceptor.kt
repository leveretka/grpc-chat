package ua.nedz.demo

import io.grpc.*

class HeaderServerInterceptor : ServerInterceptor {

    companion object {
        val HEADER_KEY: Metadata.Key<String> =
                Metadata.Key.of("vote_service_header", Metadata.ASCII_STRING_MARSHALLER)
    }

    override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val headerValue = requestHeaders.get(HEADER_KEY)
        return next.startCall(object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun sendHeaders(responseHeaders: Metadata) {
                responseHeaders.put(HEADER_KEY, headerValue?: "")
                super.sendHeaders(responseHeaders)
            }
        }, requestHeaders)
    }
}
