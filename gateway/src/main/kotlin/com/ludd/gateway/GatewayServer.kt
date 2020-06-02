package com.ludd.gateway

import com.ludd.gateway.to.Message.RpcMessage
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.type"], havingValue = "gateway")
class GatewayServer: AbstractTcpServer() {

    override suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel) {
        val message = withContext(Dispatchers.IO) {
            RpcMessage.parseFrom(read.toInputStream(job))
        }
        logger.info("message for service ${message.service} received")
    }
}