package com.ludd.rpc

import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream

class TestClient(private val host: String = "localhost", private val port: Int = 30000) {
    @OptIn(KtorExperimentalAPI::class)
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val socket = runBlocking { aSocket(selectorManager).tcp().connect(host, port) }
    private val write = socket.openWriteChannel(autoFlush = true)
    private val read = socket.openReadChannel()

    private suspend fun send(msg: AbstractMessage) {
        withContext(Dispatchers.IO) {
            msg.writeDelimitedTo(write.toOutputStream())
        }
    }

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

    suspend fun <T: AbstractMessage> receive(parseFunc: (inputStream: InputStream) -> T): T =
        withContext(Dispatchers.IO) {
            parseFunc(read.toInputStream())
        }

    suspend fun <T: AbstractMessage> receiveRpc(parseFunc: (inputStream: InputStream) -> T): T =
        withContext(Dispatchers.IO) {
            val msg = receive(Message.RpcResponse::parseDelimitedFrom)
            parseFunc(msg.result.newInput())
        }
}