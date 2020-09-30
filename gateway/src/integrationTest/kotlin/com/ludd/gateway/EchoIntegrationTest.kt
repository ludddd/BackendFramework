package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import com.ludd.test_utils.KGenericContainer
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Testcontainers
class EchoIntegrationTest {

    private val echo = KGenericContainer("ludd.echo:0.1")
        .withExposedPorts(9001)
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
        val port = echo.firstMappedPort
        logger.info("Connecting to ${echo.host}:$port")
        val socket = aSocket(selectorManager).tcp().connect(echo.host, port)
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