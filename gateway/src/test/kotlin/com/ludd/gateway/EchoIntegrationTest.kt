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
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.TimeUnit

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Tag("integration")
@Testcontainers
class EchoIntegrationTest {

    private val echo = KGenericContainer("ludd.echo:0.1")
        .withExposedPorts(9000)
        .withStartupTimeout(Duration.ofMinutes(5))

    @BeforeEach
    fun setUp() = runBlocking {
        echo.start()
        logger.info("Container Id: ${echo.containerId}")
        logger.info { echo.containerInfo }
        logger.info("docker initialized")
    }

    @AfterEach
    fun tearDown()  = runBlocking {
        echo.stop()
    }

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun directConnect() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val port = echo.getMappedPort(9000)
        logger.info("Connecting to ${echo.host}:$port")
        val socket = aSocket(selectorManager).tcp().connect(echo.host, port = port)
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