package com.ludd.auth

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.GenericContainer
import java.time.Duration
import java.util.concurrent.TimeUnit

//TODO: duplication with integrationTests
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

private val logger = KotlinLogging.logger {}

@SpringBootTest
internal class AuthRepositoryTest {

    private val echo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))


    @Autowired
    private lateinit var repository: AuthRepository

    @BeforeEach
    fun setUp() {
        echo.start()
        System.setProperty("mongodb.url", "mongodb://${echo.host}:${echo.getMappedPort(27017)}")
    }

    @AfterEach
    fun tearDown() {
        echo.stop()
    }

    @Test
    @Timeout(100, unit = TimeUnit.MINUTES)
    fun addPlayer() = runBlocking {
        val playerId = repository.addPlayer("deviceId", "userA")
        assertEquals(playerId, repository.findPlayer("deviceId", "userA"))
    }
}