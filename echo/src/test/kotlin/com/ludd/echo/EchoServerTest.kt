package com.ludd.echo

import com.google.protobuf.ByteString
import com.ludd.rpc.EchoServer
import com.ludd.rpc.conn.SocketChannelProvider
import com.ludd.rpc.to.Message
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
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
        val socketFactory = SocketChannelProvider()
        val socket = socketFactory.acquire("127.0.0.1", server.getPort())

        val message = Message.RpcRequest
            .newBuilder()
            .setService("echo")
            .setArg(ByteString.copyFrom("aaa", Charset.defaultCharset()))
            .build()
        socket.write(message)

        val response = socket.read(Message.RpcResponse::parseDelimitedFrom)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }
}