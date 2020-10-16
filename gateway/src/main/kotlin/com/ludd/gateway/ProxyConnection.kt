package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.CallResult
import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoResponseFromServiceException(serviceName: String): Exception("No response from service $serviceName")

//TODO: should have pool of connection to each service
//to allow multiple calls from different users at once
//but no more than connection pool size
@OptIn(KtorExperimentalAPI::class)
open class ProxyConnection(private val serviceName: String,
                           private val channel: IRpcMessageChannel,
                           ackEnabled: Boolean): ProxyRpcService.Connection {

    override val isClosed: Boolean
        get() = channel.isClosed()

    private val requestOptions = Message.RequestOption.newBuilder().setAckEnabled(ackEnabled)

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        logger.info("Rerouting call to service $serviceName")
        val message = Message.InnerRpcRequest.newBuilder()
            .setService(serviceName)
            .setMethod(method)
            .setArg(ByteString.copyFrom(arg))
            .setContext(sessionContext.toRequestContext())
            .setOption(requestOptions)
            .build()
        channel.write(message)
        val rez = channel.read() ?: throw NoResponseFromServiceException(serviceName)
        logger.debug("Response from service $serviceName is received")
        if (rez.hasError) logger.debug("with error: ${rez.error}")
        return rez.toCallResult()
    }

    private fun Message.RpcResponse.toCallResult() =
        if (hasError)
            CallResult(null, error)
        else
            CallResult(result.toByteArray(), null)
}