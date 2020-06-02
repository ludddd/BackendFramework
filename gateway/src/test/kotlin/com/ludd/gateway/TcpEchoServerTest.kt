package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import kotlin.test.assertEquals

@KtorExperimentalAPI
@SpringBootTest(properties=["gateway.tcp_server.port=9000", "gateway.tcp_server.type=echo"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class TcpEchoServerTest {

    @Autowired
    private lateinit var tcpServer: TcpEchoServer

    @Test
    fun connect() = runBlocking {
        val socket = connectLocal()
        val read = socket.openReadChannel()
        val write = socket.openWriteChannel(autoFlush = true)

        write.writeStringUtf8("aaa\n")

        assertEquals("aaa", read.readUTF8Line(100))
    }

    @Test
    fun clientDisconnected() = runBlocking {
        val socket = connectLocal()
        val write = socket.openWriteChannel(autoFlush = true)
        write.writeStringUtf8("aaa\n")
        withContext(Dispatchers.IO) {
            println("close client socket")
            socket.close()
            socket.awaitClosed()
            println("client socket is closed")
        }
    }

    private suspend fun connectLocal(): Socket {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        return aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
    }
}