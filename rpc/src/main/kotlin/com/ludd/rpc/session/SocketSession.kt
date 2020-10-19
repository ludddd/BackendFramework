package com.ludd.rpc.session

import com.google.protobuf.ByteString
import com.ludd.rpc.CallResult
import com.ludd.rpc.SessionContext
import com.ludd.rpc.conn.SocketWrapper
import com.ludd.rpc.to.Message
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoResponseFromServiceException(serviceName: String): Exception("No response from service $serviceName")
class ConnectionLost: Exception("Connection is lost")

open class SocketSession(private val serviceName: String,
                         private val socket: SocketWrapper,
                         private val enableAck: Boolean): Session {

    private val rpcOptions = Message.RequestOption.newBuilder().setAckEnabled(ackEnabled).build()

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        logger.info("Rerouting call to service $serviceName")
        val message = Message.InnerRpcRequest.newBuilder()
            .setService(serviceName)
            .setMethod(method)
            .setArg(ByteString.copyFrom(arg))
            .setContext(sessionContext.toRequestContext())
            .setOption(rpcOptions)
            .build()
        write(message)
        val rez = socket.read(Message.RpcResponse::parseDelimitedFrom)
            ?: throw NoResponseFromServiceException(serviceName)
        logger.debug("Response from service $serviceName is received")
        if (rez.hasError) logger.debug("with error: ${rez.error}")
        return rez.toCallResult()
    }

    open suspend fun write(msg: Message.InnerRpcRequest) {
        socket.write(msg)
        if (ackEnabled) {
            val ack = socket.read(Message.RpcReceiveAck::parseDelimitedFrom)
            if (ack == null) {
                socket.close()
                throw ConnectionLost()
            }
        }
    }

    private fun Message.RpcResponse.toCallResult() =
        if (hasError)
            CallResult(null, error)
        else
            CallResult(result.toByteArray(), null)

    val ackEnabled: Boolean
        get() = enableAck

    override val isClosed: Boolean
        get() = socket.isClosed
}

fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}