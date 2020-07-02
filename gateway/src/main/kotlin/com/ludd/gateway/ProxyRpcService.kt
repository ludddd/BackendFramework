package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@KtorExperimentalAPI
class ProxyRpcService(private val serviceName:String,
                      private val host:String,
                      private val port:Int): IRpcService
{
    //TODO: add reconnected
    //TODO: user connection pooling
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private var proxyConnection: ProxyConnection? = null

    override suspend fun call(arg: ByteString): ByteString {
        if (!isConnected()) {
            connect()
        }

        return proxyConnection!!.call(arg)
    }

    private fun isConnected() = proxyConnection != null

    private suspend fun connect() {
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        proxyConnection = ProxyConnection(serviceName, socket)
    }

}

class ProxyConnection(private val serviceName: String, private val socket: Socket) {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()

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
