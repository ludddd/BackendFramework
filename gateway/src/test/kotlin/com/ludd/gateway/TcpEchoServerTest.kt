package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@KtorExperimentalAPI
@SpringBootTest(properties=["gateway.tcp_server.port=9000"])
internal class TcpEchoServerTest {

    @Autowired
    private lateinit var tcpServer: TcpEchoServer

    @Test
    fun connect() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
        val read = socket.openReadChannel()
        val write = socket.openWriteChannel(autoFlush = true)

        write.writeStringUtf8("aaa\n")

        assertEquals("aaa", read.readUTF8Line())
    }
}