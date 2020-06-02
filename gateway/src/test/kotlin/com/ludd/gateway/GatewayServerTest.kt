package com.ludd.gateway

import com.ludd.gateway.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

@KtorExperimentalAPI
@SpringBootTest(properties=["gateway.tcp_server.port=9000", "gateway.tcp_server.type=gateway"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class GatewayServerTest {
    @Autowired
    private lateinit var tcpServer: GatewayServer

    @Test
    fun connect() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
        val write = socket.openWriteChannel(autoFlush = true)

        val message = Message.RpcMessage.newBuilder().setService("serviceA").build()
        withContext(Dispatchers.IO) {
            message.writeTo(write.toOutputStream())
        }
    }
}