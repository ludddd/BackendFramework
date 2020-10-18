package com.ludd.rpc.session

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class SocketSessionFactory: SessionFactory {

    @OptIn(KtorExperimentalAPI::class)
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.ackEnabled:false}")
    private lateinit var ackEnabled: java.lang.Boolean

    override suspend fun connect(serviceName: String, host: String, port: Int): Session {
        logger.info("Connecting to $host:$port")
        val socket = try {
            aSocket(selectorManager).tcp().connect(host, port)
        } catch (e: Exception) {
            logger.error(e) {"error while connecting to $host:$port"}
            throw e
        }
        return SocketSession(serviceName, socket, ackEnabled.booleanValue())
    }

}