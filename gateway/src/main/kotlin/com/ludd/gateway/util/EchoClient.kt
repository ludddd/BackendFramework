package com.ludd.gateway.util

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

suspend fun sendEchoMessage(socket: Socket, text: String): Message.RpcResponse? {
    val write = socket.openWriteChannel(autoFlush = true)
    val read = socket.openReadChannel()

    return sendEchoMessage(text, write, read)
}

suspend fun sendEchoMessage(
    text: String,
    write: ByteWriteChannel,
    read: ByteReadChannel
): Message.RpcResponse? {
    val message = Message.RpcRequest
        .newBuilder()
        .setService("echo")
        .setArg(ByteString.copyFrom(text, Charset.defaultCharset()))
        .build()
    withContext(Dispatchers.IO) {
        message.writeDelimitedTo(write.toOutputStream())
    }

    return withContext(Dispatchers.IO) {
        Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
    }
}