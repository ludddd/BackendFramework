package com.ludd.player

import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.test_utils.KGenericContainer
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.util.SocketUtils
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@SpringBootTest(properties=["server.port=9000"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    //TODO:  testcontainers changes port on each start so we had to reinit mongo connection
@Timeout(1, unit = TimeUnit.MINUTES)
class ApplicationTest {

    private val mongo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))


    @BeforeEach
    fun setUp() {
        mongo.start()
        System.setProperty("mongodb.url", "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}")
    }

    @AfterEach
    fun tearDown() {
        mongo.stop()
    }

    @Test
    fun runApplication() {
    }

    @Autowired
    private lateinit var autoDiscovery: IRpcAutoDiscovery

    @Test
    fun startServer() {
        @Suppress("DEPRECATION") val server = Server(autoDiscovery, Integer(SocketUtils.findAvailableTcpPort()))
        server.start()
        server.stop()
    }
}