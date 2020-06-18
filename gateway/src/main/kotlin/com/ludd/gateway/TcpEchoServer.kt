package com.ludd.gateway

import com.ludd.rpc.AbstractTcpServer
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.type"], havingValue = "echo")
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class TcpEchoServer(@Value("\${gateway.tcp_server.port}") port: Integer): AbstractTcpServer(port.toInt()) {

    override suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel) {
        val line = read.readUTF8Line()
        logger.debug("received: $line")
        write.writeStringUtf8("$line\n")
        logger.debug("send: $line")
    }
}