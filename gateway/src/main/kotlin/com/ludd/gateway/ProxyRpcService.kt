package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
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

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        if (!isConnected()) {
            connect()
        }

        return proxyConnection!!.call(arg, sessionContext)
    }

    private fun isConnected() = proxyConnection != null && !proxyConnection!!.isClosed

    private suspend fun connect() {
        logger.info("Connecting to $host:$port")
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        proxyConnection = ProxyConnection(serviceName, SocketRpcMessageChannel(socket))
    }

}

interface IRpcMessageChannel {
    suspend fun write(msg: Message.InnerRpcRequest)
    suspend fun read(): Message.RpcResponse
    fun isClosed(): Boolean
}

//TODO: should have pool of connection to each service
//to allow multiple calls from different users at once
//but no more than connection pool size
class ProxyConnection(private val serviceName: String, private val channel: IRpcMessageChannel) {

    val isClosed: Boolean
        get() = channel.isClosed()

    suspend fun call(arg: ByteArray, sessionContext: SessionContext): CallResult {
        logger.info("Rerouting call to service $serviceName")
        val message = Message.InnerRpcRequest
            .newBuilder()
            .setService(serviceName)
            .setArg(ByteString.copyFrom(arg))
            .setContext(sessionContext.toRequestContext())
            .build()
        channel.write(message)
        val rez = channel.read()
        return CallResult(rez.result.toByteArray(), rez.error)
    }
}

private fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}
