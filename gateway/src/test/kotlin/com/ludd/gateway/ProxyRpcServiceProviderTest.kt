package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.nio.charset.Charset

@KtorExperimentalAPI
@SpringBootTest(properties=[
    "gateway.tcp_server.port=9000",
    "gateway.tcp_server.type=gateway",
    "gateway.service_provider=proxy"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class ProxyRpcServiceProviderTest {
    @Autowired
    private lateinit var echoServer: TcpEchoServer

    @Autowired
    private lateinit var tcpServer: GatewayServer

    @Test
    fun echo() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket =  aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())

        val response = GatewayServerTest.sendEchoMessage(socket, "aaa")
        kotlin.test.assertNotNull(response)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }
}