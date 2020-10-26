package com.ludd.rpc.conn

import com.google.protobuf.AbstractMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class SocketChannel(private val socket: Socket) : Channel {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()

    override suspend fun write(msg: AbstractMessage) {
        withContext(Dispatchers.IO) {
            msg.writeDelimitedTo(write.toOutputStream())
        }
    }

    override suspend fun <T> read(msgBuilder: (input: InputStream) -> T): T {
        return withContext(Dispatchers.IO) {
            msgBuilder(read.toInputStream())
        }
    }

    override fun close() {
        socket.close()
    }

    override val isClosed: Boolean
        get() = socket.isClosed || write.isClosedForWrite || read.isClosedForRead
}

class SocketChannelProvider: ChannelProvider {
    @OptIn(KtorExperimentalAPI::class)
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)

    override suspend fun acquire(host: String, port: Int): Channel {
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        return SocketChannel(socket)
    }
}
