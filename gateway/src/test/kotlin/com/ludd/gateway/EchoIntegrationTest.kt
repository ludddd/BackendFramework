package com.ludd.gateway

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
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset
import java.time.Duration

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

@KtorExperimentalAPI
@Tag("integration")
@Testcontainers
class EchoIntegrationTest {

    private val echo = KGenericContainer("echo")
        .withExposedPorts(9000)
        .withStartupTimeout(Duration.ofMinutes(5))

    @Before
    fun setUp() = runBlocking {
        echo.start()
    }

    @After
    fun tearDown()  = runBlocking {
        echo.stop()
    }

    @Test
    fun directConnect() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = 9000)
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