package com.ludd.rpc

import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import com.ludd.rpc.conn.SocketWrapperFactory
import com.ludd.rpc.to.Message
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream

class TestClient(private val host: String = "localhost", private val port: Int = 30000) {
    @OptIn(KtorExperimentalAPI::class)
    private val socketFactory = SocketWrapperFactory()
    private val socket = runBlocking { socketFactory.connect(host, port) }

    suspend fun send(msg: AbstractMessage) = socket.write(msg)

    suspend fun sendRpc(service: String, method: String, arg: AbstractMessage) {
        sendRpc(service, method, arg.toByteString())
    }

    suspend fun sendRpc(service: String, method: String, arg: ByteString) {
        val request = Message.RpcRequest.newBuilder()
            .setService(service)
            .setMethod(method)
            .setArg(arg)
            .build()
        send(request)
    }

    suspend fun <T: AbstractMessage> receive(parseFunc: (inputStream: InputStream) -> T): T = socket.read(parseFunc)

    suspend fun <T: AbstractMessage> receiveRpc(parseFunc: (inputStream: InputStream) -> T): T =
        withContext(Dispatchers.IO) {
            val msg = receive(Message.RpcResponse::parseDelimitedFrom)
            parseFunc(msg.result.newInput())
        }
}