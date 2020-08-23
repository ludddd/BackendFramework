package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.IRpcService
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

@KtorExperimentalAPI
class ProxyRpcService(
    private val serviceName: String,
    private val host: String,
    private val port: Int
): IRpcService
{
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private var proxyConnection: ProxyConnection? = null

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): ByteArray {
        if (!isConnected()) {
            connect()
        }

        return proxyConnection!!.call(arg, sessionContext)
    }

    private fun isConnected() = proxyConnection != null && !proxyConnection!!.isClosed

    private suspend fun connect() {
        logger.info("Connecting to $host:$port")
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        proxyConnection = ProxyConnection(serviceName, SocketProxyConnectionChannel(socket))
    }

}

interface IProxyConnectionChannel {
    suspend fun write(msg: Message.InnerRpcRequest)
    suspend fun read(): Message.RpcResponse
    fun isClosed(): Boolean
}

class SocketProxyConnectionChannel(private val socket: Socket): IProxyConnectionChannel {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()
    override suspend fun write(msg: Message.InnerRpcRequest) {
        withContext(Dispatchers.IO) {
            msg.writeDelimitedTo(write.toOutputStream())
        }
    }

    override suspend fun read(): Message.RpcResponse {
        return withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
    }

    override fun isClosed(): Boolean = socket.isClosed || write.isClosedForWrite || read.isClosedForRead

}

//TODO: should have pool of connection to each service
//to allow multiple calls from different users at once
//but no more than connection pool size
class ProxyConnection(private val serviceName: String, private val channel: IProxyConnectionChannel) {

    val isClosed: Boolean
        get() = channel.isClosed()

    suspend fun call(arg: ByteArray, sessionContext: SessionContext): ByteArray {
        val message = Message.InnerRpcRequest
            .newBuilder()
            .setService(serviceName)
            .setArg(ByteString.copyFrom(arg))
            .setContext(sessionContext.toRequestContext())
            .build()
        channel.write(message)
        val response = channel.read()
        return response.result.toByteArray()
    }
}

private fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}
