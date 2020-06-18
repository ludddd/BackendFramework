package com.ludd.rpc

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
abstract class AbstractTcpServer: CoroutineScope {
    protected val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${gateway.tcp_server.port}")
    private lateinit var port: Integer
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private lateinit var serverJob: Job
    fun getPort() = port.toInt()
    private val sessionCount = AtomicInteger(0)

    @PostConstruct
    fun start() {
        serverJob = launch {
            val serverSocket = aSocket(selectorManager).tcp().bind(port = getPort())
            logger.info("${javaClass.name} listening at ${serverSocket.localAddress}")
            serverSocket.use {
                while (isActive) {
                    val socket = serverSocket.accept()
                    logger.info("Accepted $socket")
                    startSession(socket)
                }
            }
            logger.info("${javaClass.name} stop listening at ${serverSocket.localAddress}")
        }
    }

    private fun startSession(socket: Socket) {
        sessionCount.incrementAndGet()
        launch {
            val remoteAddress = socket.remoteAddress
            logger.info("start session with $remoteAddress")
            socket.use {
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)
                while (isActive) {
                    processMessages(read, write)
                }
            }
            logger.info("End session with $remoteAddress")
            sessionCount.decrementAndGet()
        }
    }

    abstract suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel)

    @PreDestroy
    fun stop() = runBlocking{
        logger.info("Stopping server...")
        job.cancelChildren()
        job.cancelAndJoin()
        logger.info("Server is stopped")
    }

    fun getSessionCount() = sessionCount.get()
}