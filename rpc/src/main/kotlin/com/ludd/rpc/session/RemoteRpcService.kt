package com.ludd.rpc.session

import com.google.protobuf.ByteString
import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import com.ludd.rpc.conn.Channel
import com.ludd.rpc.conn.ChannelProvider
import com.ludd.rpc.to.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoResponseFromServiceException(serviceName: String): Exception("No response from service $serviceName")
class ConnectionLost: Exception("Connection is lost")

open class RemoteRpcService(private val serviceName: String,
                            val host: String,
                            val port: Int,
                            val ackEnabled: Boolean,
                            private val channelProvider: ChannelProvider): IRpcService {

    private val rpcOptions = Message.RequestOption.newBuilder().setAckEnabled(ackEnabled).build()

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        logger.info("Rerouting call to service $serviceName")
        channelProvider.acquire(host, port).use { channel ->
            val message = Message.InnerRpcRequest.newBuilder()
                .setService(serviceName)
                .setMethod(method)
                .setArg(ByteString.copyFrom(arg))
                .setContext(sessionContext.toRequestContext())
                .setOption(rpcOptions)
                .build()
            write(channel, message)
            val rez = channel.read(Message.RpcResponse::parseDelimitedFrom)
                ?: throw NoResponseFromServiceException(serviceName)
            logger.debug("Response from service $serviceName is received")
            if (rez.hasError) logger.debug("with error: ${rez.error}")
            return rez.toCallResult()
        }
    }

    private suspend fun write(channel: Channel, msg: Message.InnerRpcRequest) {
        channel.write(msg)
        if (ackEnabled) {
            val ack = channel.read(Message.RpcReceiveAck::parseDelimitedFrom)
            if (ack == null) {
                withContext(Dispatchers.IO) {
                    channel.close()
                }
                throw ConnectionLost()
            }
        }
    }

    private fun Message.RpcResponse.toCallResult() =
        if (hasError)
            CallResult(null, error)
        else
            CallResult(result.toByteArray(), null)

    val url: String
        get() = "$host:$port"
}

fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}