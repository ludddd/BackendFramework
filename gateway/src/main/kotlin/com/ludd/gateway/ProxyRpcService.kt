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
                      private val port:Int): IRpcService {

    //TODO: move connected state to class itself/class builder
    //TODO: add reconnected
    //TODO: user connection pooling
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private var socket: Socket? = null
    private var write: ByteWriteChannel? = null
    private var read: ByteReadChannel? = null

    override suspend fun call(arg: ByteString): ByteString {
        if (!isConnected()) {
            connect()
        }

        val message = Message.RpcRequest
            .newBuilder()
            .setService(serviceName)
            .setArg(arg)
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(outputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(inputStream())
        }
        return response.result
    }

    private fun inputStream() = read?.toInputStream() ?:
        throw Exception("trying to receive while not being connected to $serviceName")

    private fun outputStream() = write?.toOutputStream() ?:
        throw Exception("trying to send while not being connected to $serviceName")

    private fun isConnected() = socket != null

    private suspend fun connect() {
        socket = aSocket(selectorManager).tcp().connect(host, port)
        val currentSocket = socket ?: throw Exception("Failed to connect to service $serviceName")

        write = currentSocket.openWriteChannel(autoFlush = true)
        read = currentSocket.openReadChannel()
    }

}
