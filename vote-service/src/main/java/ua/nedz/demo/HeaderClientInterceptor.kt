package ua.nedz.demo

import io.grpc.*

class HeaderClientInterceptor : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

            override fun start(responseListener: ClientCall.Listener<RespT>, headers: Metadata) {
                headers.put(HEADER_KEY, "customRequestValue")
                super.start(object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {},
                        headers)
            }
        }
    }

    companion object {
        val HEADER_KEY: Metadata.Key<String> =
                Metadata.Key.of("vote_service_header", Metadata.ASCII_STRING_MARSHALLER)
    }
}
