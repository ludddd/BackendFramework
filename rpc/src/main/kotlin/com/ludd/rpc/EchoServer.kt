package com.ludd.rpc

import com.ludd.rpc.to.Message
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@ConditionalOnProperty("echo_server.port")
class EchoServer(@Value("\${echo_server.port}") val port: Integer) {

    private var impl: EchoServerImpl? = null

    @PostConstruct
    fun start() {
        require(impl == null) {"server is running already"}
        impl = EchoServerImpl(port.toInt())
        impl?.start()
    }

    @PreDestroy
    fun stop() {
        require(impl != null) {"server is stopped already"}
        impl?.stop()
        impl = null
    }

    fun waitTillTermination() {
        impl?.waitTillTermination()
    }

    fun getPort(): Int = port.toInt()
}

@KtorExperimentalAPI
class EchoServerImpl(port: Int): AbstractTcpServer(port) {

    override suspend fun processMessages(read: ByteReadChannel,
                                         write: ByteWriteChannel,
                                         sessionContext: SessionContext) {
        val message = withContext(Dispatchers.IO) {
            Message.RpcRequest.parseDelimitedFrom(read.toInputStream(job))
        }
        if (message == null) {
            logger.warn("Failed to read incoming message")
            return
        }
        val response = Message.RpcResponse.newBuilder().setResult(message.arg).build()
        withContext(Dispatchers.IO) {
            response.writeDelimitedTo(write.toOutputStream(job))
        }
    }
}