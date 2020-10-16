package com.ludd.gateway

import com.ludd.rpc.to.Message
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ConnectionLost: Exception()

class SocketRpcMessageChannel(private val socket: Socket): IRpcMessageChannel {

    private val write: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val read: ByteReadChannel = socket.openReadChannel()
    override suspend fun write(msg: Message.InnerRpcRequest) {
        withContext(Dispatchers.IO) {
            msg.writeDelimitedTo(write.toOutputStream())
            if (msg.option.ackEnabled) {
                val ack = Message.RpcReceiveAck.parseDelimitedFrom(read.toInputStream())
                if (ack == null) {
                    socket.close()
                    throw ConnectionLost()
                }
            }
        }
    }

    override suspend fun read(): Message.RpcResponse? {
        return withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
    }

    override fun isClosed(): Boolean = socket.isClosed || write.isClosedForWrite || read.isClosedForRead
}