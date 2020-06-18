package com.ludd.echo

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
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

@KtorExperimentalAPI
@SpringBootTest(properties=["echo_server.port=9000"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
internal class EchoServerTest {

    @Autowired
    private lateinit var server: EchoServer

    @Test
    fun echo() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = server.getPort())
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()

        val message = Message.RpcRequest
            .newBuilder()
            .setService("echo")
            .setArg(ByteString.copyFrom("aaa", Charset.defaultCharset()))
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }
}