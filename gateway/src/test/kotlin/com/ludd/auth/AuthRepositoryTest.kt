package com.ludd.auth

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.containers.GenericContainer
import java.time.Duration
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

//TODO: duplication with integrationTests
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

private val logger = KotlinLogging.logger {}

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    //TODO:  testcontainers changes port on each start so we had to reinit mongo connection
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
        runBlocking {
            repository.ensureIndex()
        }
    }

    @AfterEach
    fun tearDown() {
        echo.stop()
    }

    @Test
    fun addPlayer() = runBlocking {
        val playerId = repository.addPlayer("deviceId", "userA")
        assertEquals(playerId, repository.findPlayer("deviceId", "userA"))
    }

    @Test
    fun addMultipleIds()  = runBlocking {
        val playerA = repository.addPlayer("deviceId", "userA")
        val playerB = repository.addPlayer("deviceId", "userB")
        repository.addPlayer("deviceId", "userC")
        assertEquals(playerB, repository.findPlayer("deviceId", "userB"))
        assertNotEquals(playerA, playerB)
    }

    @Test
    fun addMultipleTypes() = runBlocking {
        val playerDevice = repository.addPlayer("deviceId", "userA")
        val playerFacebook = repository.addPlayer("facebookId", "userA")
        assertEquals(playerFacebook, repository.findPlayer("facebookId", "userA"))
        assertNotEquals(playerDevice, playerFacebook)
    }

    @Test
    fun findNonExisting() = runBlocking {
        assertNull(repository.findPlayer("deviceId", "nonExisting"))
    }

    @Test
    fun duplicate() = runBlocking {
        repository.addPlayer("deviceId", "userA")
        assertThrows<DuplicatePlayerIdException> {
            runBlocking {
                repository.addPlayer("deviceId", "userA")
            }
        }
        Unit
    }
}