package com.ludd.gateway

import com.ludd.gateway.to.Message
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.type"], havingValue = "gateway")
class GatewayServer: AbstractTcpServer() {

    @Autowired
    private lateinit var serviceProvider: IRpcServiceProvider

    override suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel) {
        val message = withContext(Dispatchers.IO) {
            Message.RpcRequest.parseDelimitedFrom(read.toInputStream(job))
        }
        val response = callRpc(message)
        withContext(Dispatchers.IO) {
            response.writeDelimitedTo(write.toOutputStream(job))
        }
    }

    private fun callRpc(message: Message.RpcRequest): Message.RpcResponse {
        logger.info("message for service ${message.service} received")
        val service = serviceProvider.get(message.service)
        val result = service.call(message.arg)
        return Message.RpcResponse.newBuilder().setResult(result).build()
    }
}

