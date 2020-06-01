package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import kotlin.coroutines.CoroutineContext

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.port"], havingValue = "")
class TcpServer: CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${gateway.tcp_server.port}")
    private lateinit var port: Integer

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)

    fun getPort() = port.toInt()

    @PostConstruct
    fun start() = launch {
        val serverSocket = aSocket(selectorManager).tcp().bind(port = getPort())
        println("Echo Server listening at ${serverSocket.localAddress}")
        while (true) {
            val socket = serverSocket.accept()
            println("Accepted $socket")
            launch {
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)
                try {
                    while (true) {
                        val line = read.readUTF8Line()
                        print("received: $line")
                        write.writeStringUtf8("$line\n")
                        print("send: $line")
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.IO) {
                        socket.close()
                    }
                }
            }
        }
    }
}