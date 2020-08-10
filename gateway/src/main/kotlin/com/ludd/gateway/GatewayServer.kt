package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.AbstractTcpServer
import com.ludd.rpc.IRpcServiceProvider
import com.ludd.rpc.to.Message
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.port"])
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class GatewayServer(@Value("\${gateway.tcp_server.port}") port: Integer):
    AbstractTcpServer(port.toInt()) {

    @Autowired
    private lateinit var serviceProvider: IRpcServiceProvider

    override suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel) {
        val message = withContext(Dispatchers.IO) {
            Message.RpcRequest.parseDelimitedFrom(read.toInputStream(coroutineContext[Job]))
        }
        val response = callRpc(message)
        withContext(Dispatchers.IO) {
            response.writeDelimitedTo(write.toOutputStream(coroutineContext[Job]))
        }
    }

    private suspend fun callRpc(message: Message.RpcRequest): Message.RpcResponse {
        logger.info("message for service ${message.service} received")
        val service = serviceProvider.get(message.service)
        val result = service.call(message.method, message.arg.toByteArray())
        return Message.RpcResponse.newBuilder().setResult(ByteString.copyFrom(result)).build()
    }

    @PostConstruct
    fun onPostConstruct() {
        start()
    }

    @PreDestroy
    fun onPreDestroy() {
        super.stop()
    }
}

