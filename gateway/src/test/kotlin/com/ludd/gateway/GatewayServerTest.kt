package com.ludd.gateway

import com.ludd.gateway.util.sendEchoMessage
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@KtorExperimentalAPI
@SpringBootTest(properties=["gateway.tcp_server.port=9000",
    "gateway.service_provider=local",
    "gateway.echo_server.port=9001"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GatewayServerTest {
    @Autowired
    private lateinit var tcpServer: GatewayServer
    @Autowired
    private lateinit var echoServer: TcpEchoServer

    @Test
    fun echo() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
        val response = sendEchoMessage(socket, "aaa")
        assertNotNull(response)
        assertEquals("aaa", response.result.toString(Charset.defaultCharset()))

    }
}