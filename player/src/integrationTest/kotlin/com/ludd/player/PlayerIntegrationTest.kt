package com.ludd.player

import com.ludd.player.to.Player
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
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Testcontainers
class PlayerIntegrationTest {

    private val mongo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))
    private val player = KGenericContainer("ludd.player:0.1")
        .withExposedPorts(9001)
        .withStartupTimeout(Duration.ofMinutes(5))

    @BeforeEach
    fun setUp() = runBlocking {
        mongo.start()
        val mongoHostPort = mongo.getMappedPort(27017)
        org.testcontainers.Testcontainers.exposeHostPorts(mongoHostPort)
        player.withEnv("mongodb.url", "localhost:$mongoHostPort")
        player.start()
        logger.info("Container Id: ${player.containerId}")
        logger.info { player.containerInfo }
        logger.info("docker initialized")
    }

    @AfterEach
    fun tearDown()  = runBlocking {
        player.stop()
        mongo.stop()
    }

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun directConnect() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val port = player.firstMappedPort
        logger.info("Connecting to ${player.host}:$port")
        val socket = aSocket(selectorManager).tcp().connect(player.host, port)
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()

        val arg = Player.SetNameRequest
            .newBuilder()
            .setName("aaa")
            .build()

        val message = Message.InnerRpcRequest
            .newBuilder()
            .setService("playerInfo")
            .setMethod("setName")
            .setArg(arg.toByteString())
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
        kotlin.test.assertEquals("Player should be authorized", response.error)
    }
}