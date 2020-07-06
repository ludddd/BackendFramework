package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.EchoServer
import com.ludd.rpc.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    "gateway.tcp_server.type=gateway",
    "gateway.service_provider=proxy",
    "echo_server.port=9001"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class ProxyRpcServiceProviderTest {
    @Autowired
    private lateinit var echoServer: EchoServer

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

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun reconnect() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket =  aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()
        val text = "aaa"

        val message = Message.RpcRequest
            .newBuilder()
            .setService("echo")
            .setArg(ByteString.copyFrom(text, Charset.defaultCharset()))
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }

        echoServer.stop()
        echoServer.waitTillTermination()
        echoServer.start()
        delay(1000)

        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }

        kotlin.test.assertNotNull(response)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }
}