package com.ludd.rpc.session

import com.google.protobuf.ByteString
import com.ludd.rpc.CallResult
import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class NoResponseFromServiceException(serviceName: String): Exception("No response from service $serviceName")

class SocketSession(private val serviceName: String,
                    socket: Socket): Session {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        logger.info("Rerouting call to service $serviceName")
        val message = Message.InnerRpcRequest.newBuilder()
            .setService(serviceName)
            .setMethod(method)
            .setArg(ByteString.copyFrom(arg))
            .setContext(sessionContext.toRequestContext())
            .build()
        write(message)
        val rez = read() ?: throw NoResponseFromServiceException(serviceName)
        logger.debug("Response from service $serviceName is received")
        if (rez.hasError) logger.debug("with error: ${rez.error}")
        return rez.toCallResult()
    }

    suspend fun write(msg: Message.InnerRpcRequest) {
        withContext(Dispatchers.IO) {
            msg.writeDelimitedTo(write.toOutputStream())
        }
    }

    suspend fun read(): Message.RpcResponse? {
        return withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
    }

    private fun Message.RpcResponse.toCallResult() =
        if (hasError)
            CallResult(null, error)
        else
            CallResult(result.toByteArray(), null)
}

class SocketSessionFactory: SessionFactory {

    @OptIn(KtorExperimentalAPI::class)
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)

    override suspend fun connect(serviceName: String, host: String, port: Int): Session {
        logger.info("Connecting to $host:$port")
        val socket = try {
            aSocket(selectorManager).tcp().connect(host, port)
        } catch (e: Exception) {
            logger.error(e) {"error while connecting to $host:$port"}
            throw e
        }
        return SocketSession(serviceName, socket)
    }

}

fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}