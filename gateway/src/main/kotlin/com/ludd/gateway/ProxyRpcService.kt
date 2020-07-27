package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.IRpcService
import com.ludd.rpc.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
class ProxyRpcService(private val serviceName:String,
                      private val host:String,
                      private val port:Int): IRpcService
{
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private var proxyConnection: ProxyConnection? = null

    override suspend fun call(method: String, arg: ByteString): ByteString {
        if (!isConnected()) {
            connect()
        }

        return proxyConnection!!.call(arg)
    }

    private fun isConnected() = proxyConnection != null && !proxyConnection!!.isClosed

    private suspend fun connect() {
        logger.info("Connecting to $host:$port")
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        proxyConnection = ProxyConnection(serviceName, socket)
    }

}

//TODO: should have pool of connection to each service
//to allow multiple calls from different users at once
//but no more than connection pool size
class ProxyConnection(private val serviceName: String, private val socket: Socket) {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()

    val isClosed: Boolean
        get() = socket.isClosed || write.isClosedForWrite || read.isClosedForRead

    suspend fun call(arg: ByteString): ByteString {
        val message = Message.RpcRequest
            .newBuilder()
            .setService(serviceName)
            .setArg(arg)
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
        return response.result
    }
}
