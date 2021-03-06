package com.ludd.rpc

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
abstract class AbstractTcpServer(private val port:Int, private val shutdownTimeoutMs: Long = 30_000): CoroutineScope {

    constructor(port:Int): this(port, 30_000)   //why spring need this?

    protected val job = Job()
    @Suppress("EXPERIMENTAL_API_USAGE")
    private val threadPool = newFixedThreadPoolContext(5, "tcpServer") //TODO: move to parameter
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private lateinit var serverJob: Job
    fun getPort() = port
    private val sessionCount = AtomicInteger(0)

    fun start() {
        logger.info("Starting tcp server at port: $port")
        val serverSocket = aSocket(selectorManager).tcp().bind(port = getPort())
        logger.info("${this@AbstractTcpServer.javaClass.name} listening at ${serverSocket.localAddress}")
        serverJob = launch(threadPool) {
            serverSocket.use {
                while (isActive) {
                    val socket = serverSocket.accept()
                    logger.info("Accepted $socket")
                    supervisorScope {
                        startSession(socket)
                    }
                }
            }
            logger.info("${this@AbstractTcpServer.javaClass.name} stop listening at ${serverSocket.localAddress}")
        }
    }

    private fun startSession(socket: Socket) {
        sessionCount.incrementAndGet()
        launch(threadPool) {
            val remoteAddress = socket.remoteAddress
            logger.info("start session with $remoteAddress")
            socket.use {
                val sessionContext = SessionContext(remoteAddress)
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)
                while (isActive) {
                    try {
                        processMessages(read, write, sessionContext)
                    } catch (e: Exception) {
                        if (!read.isClosedForRead && !write.isClosedForWrite) {
                            logger.error(e) { "Error while processing messages" }
                        }
                        break
                    }
                }
            }
            logger.info("End session with $remoteAddress")
            sessionCount.decrementAndGet()
        }
    }

    abstract suspend fun processMessages(read: ByteReadChannel, write: ByteWriteChannel, sessionContext: SessionContext)

    fun stop() = runBlocking{
        logger.info("Stopping server...")
        try {
            withTimeout(shutdownTimeoutMs) {
                job.cancelChildren()
                job.cancelAndJoin()
            }
        } catch (e: TimeoutCancellationException) {
            logger.error("server jobs failed to stop in required time")
        }
        logger.info("Server is stopped")
    }

    fun getSessionCount() = sessionCount.get()

    fun waitTillTermination() = runBlocking {
        job.join()
    }
}