package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.port"], havingValue = "")
class TcpEchoServer: CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${gateway.tcp_server.port}")
    private lateinit var port: Integer

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private lateinit var serverJob: Job

    fun getPort() = port.toInt()

    @PostConstruct
    fun start() {
        serverJob = launch {
            val serverSocket = aSocket(selectorManager).tcp().bind(port = getPort())
            logger.info("Tcp Echo Server listening at ${serverSocket.localAddress}")
            while (isActive) {
                val socket = serverSocket.accept()
                logger.info("Accepted $socket")
                launch {
                    val read = socket.openReadChannel()
                    val write = socket.openWriteChannel(autoFlush = true)
                    try {
                        while (true) {
                            val line = read.readUTF8Line()
                            logger.debug("received: $line")
                            write.writeStringUtf8("$line\n")
                            logger.debug("send: $line")
                        }
                    } catch (e: CancellationException) {
                        logger.info("server job is cancelled")
                    } catch (e: Throwable) {
                        logger.error(e) { "error while processing incoming messages" }
                        withContext(Dispatchers.IO) {
                            socket.close()
                        }
                    }
                }
            }
        }
    }

    @PreDestroy
    fun stop() = runBlocking{
        logger.info("Stopping server...")
        job.cancelAndJoin()
    }
}