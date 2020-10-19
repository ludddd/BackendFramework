package com.ludd.rpc.conn

import com.google.protobuf.AbstractMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class SocketWrapper(private val socket: Socket) {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()

    suspend fun write(msg: AbstractMessage) {
        withContext(Dispatchers.IO) {
            msg.writeDelimitedTo(write.toOutputStream())
        }
    }

    suspend fun <T> read(msgBuilder: (input: InputStream) -> T): T {
        return withContext(Dispatchers.IO) {
            msgBuilder(read.toInputStream())
        }
    }

    fun close() {
        socket.close()
    }

    val isClosed: Boolean
        get() = socket.isClosed || write.isClosedForWrite || read.isClosedForRead
}

@Suppress("EXPERIMENTAL_API_USAGE")
suspend fun ActorSelectorManager.tcpConnect(host: String, port: Int): SocketWrapper {
    val socket = aSocket(this).tcp().connect(host, port)
    return SocketWrapper(socket)
}