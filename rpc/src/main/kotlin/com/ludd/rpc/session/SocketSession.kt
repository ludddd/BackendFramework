package com.ludd.rpc.session

import com.google.protobuf.ByteString
import com.ludd.rpc.CallResult
import com.ludd.rpc.SessionContext
import com.ludd.rpc.conn.RpcSocket
import com.ludd.rpc.conn.RpcSocketFactory
import com.ludd.rpc.to.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoResponseFromServiceException(serviceName: String): Exception("No response from service $serviceName")
class ConnectionLost: Exception("Connection is lost")

open class SocketSession(private val serviceName: String,
                         private val host: String,
                         private val port: Int,
                         val ackEnabled: Boolean,
                         private val socketFactory: RpcSocketFactory): Session {

    private val rpcOptions = Message.RequestOption.newBuilder().setAckEnabled(ackEnabled).build()

    private suspend fun connect(): RpcSocket {
        logger.info("Connecting to $host:$port")
        return try {
            socketFactory.connect(host, port)
        } catch (e: Exception) {
            logger.error(e) {"error while connecting to $host:$port"}
            throw e
        }
    }

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        logger.info("Rerouting call to service $serviceName")
        val socket = connect()
        val message = Message.InnerRpcRequest.newBuilder()
            .setService(serviceName)
            .setMethod(method)
            .setArg(ByteString.copyFrom(arg))
            .setContext(sessionContext.toRequestContext())
            .setOption(rpcOptions)
            .build()
        write(socket, message)
        val rez = socket.read(Message.RpcResponse::parseDelimitedFrom)
            ?: throw NoResponseFromServiceException(serviceName)
        logger.debug("Response from service $serviceName is received")
        if (rez.hasError) logger.debug("with error: ${rez.error}")
        return rez.toCallResult()
    }

    private suspend fun write(socket: RpcSocket, msg: Message.InnerRpcRequest) {
        socket.write(msg)
        if (ackEnabled) {
            val ack = socket.read(Message.RpcReceiveAck::parseDelimitedFrom)
            if (ack == null) {
                withContext(Dispatchers.IO) {
                    socket.close()
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
}

fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}