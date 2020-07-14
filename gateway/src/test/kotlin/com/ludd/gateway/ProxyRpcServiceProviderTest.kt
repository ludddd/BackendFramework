package com.ludd.gateway

import com.ludd.gateway.util.sendEchoMessage
import com.ludd.rpc.EchoServer
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
@SpringBootTest(properties=[
    "gateway.tcp_server.port=9000",
    "gateway.service_provider=proxy",
    "gateway.services=echo:localhost:9001",
    "echo_server.port=9001",
    "echo_server.host=localhost"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class ProxyRpcServiceProviderTest {
    @Autowired
    private lateinit var echoServer: EchoServer

    @Autowired
    private lateinit var tcpServer: GatewayServer

    @Autowired
    private lateinit var proxyRpcServiceProvider: ProxyRpcServiceProvider

    @Test
    fun echo() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket =  aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())

        val response = sendEchoMessage(socket, "aaa")
        kotlin.test.assertNotNull(response)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun reconnect() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket =  aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()
        val text = "aaa"

        sendEchoMessage(text, write, read)

        echoServer.stop()
        echoServer.waitTillTermination()
        echoServer.start()
        delay(1000)

        val response = sendEchoMessage(text, write, read)

        kotlin.test.assertNotNull(response)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }

    @Test
    fun servicesFromProperties() {
        assertThat(proxyRpcServiceProvider.servicesList(), Matchers.hasItem("echo:localhost:9001"))
    }
}