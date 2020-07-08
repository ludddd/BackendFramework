package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    companion object {
        suspend fun sendEchoMessage(socket: Socket, text: String): Message.RpcResponse? {
            val write = socket.openWriteChannel(autoFlush = true)
            val read = socket.openReadChannel()

            return sendEchoMessage(text, write, read)
        }

        suspend fun sendEchoMessage(
            text: String,
            write: ByteWriteChannel,
            read: ByteReadChannel
        ): Message.RpcResponse? {
            val message = Message.RpcRequest
                .newBuilder()
                .setService("echo")
                .setArg(ByteString.copyFrom(text, Charset.defaultCharset()))
                .build()
            withContext(Dispatchers.IO) {
                message.writeDelimitedTo(write.toOutputStream())
            }

            return withContext(Dispatchers.IO) {
                Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
            }
        }
    }
}