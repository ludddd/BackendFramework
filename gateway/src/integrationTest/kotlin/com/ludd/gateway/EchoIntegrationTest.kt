package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.TestClient
import com.ludd.rpc.to.Message
import com.ludd.test_utils.KGenericContainer
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
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
        val port = echo.firstMappedPort
        logger.info("Connecting to ${echo.host}:$port")
        val client = TestClient(echo.host, port)

        val message = Message.RpcRequest
            .newBuilder()
            .setService("echo")
            .setArg(ByteString.copyFrom("aaa", Charset.defaultCharset()))
            .build()
        client.send(message)

        val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }
}