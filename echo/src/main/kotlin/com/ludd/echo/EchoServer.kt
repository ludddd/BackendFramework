package com.ludd.echo

import com.ludd.rpc.AbstractTcpServer
import com.ludd.rpc.to.Message
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@KtorExperimentalAPI
@Component
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class EchoServer(@Value("\${echo_server.port}") port: Integer): AbstractTcpServer(port.toInt()) {
    override suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel) {
        val message = withContext(Dispatchers.IO) {
            Message.RpcRequest.parseDelimitedFrom(read.toInputStream(job))
        }
        val response = Message.RpcResponse.newBuilder().setResult(message.arg).build()
        withContext(Dispatchers.IO) {
            response.writeDelimitedTo(write.toOutputStream(job))
        }
    }
}