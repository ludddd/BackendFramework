package com.ludd.auth

import com.ludd.test_utils.KGenericContainer
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

private val logger = KotlinLogging.logger {}

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    //TODO:  testcontainers changes port on each start so we had to reinit mongo connection
@Timeout(1, unit = TimeUnit.MINUTES)
internal class AuthRepositoryTest {

    private val mongo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))


    @Autowired
    private lateinit var repository: AuthRepository

    @BeforeEach
    fun setUp() {
        mongo.start()
        System.setProperty("mongodb.url", "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}")
        runBlocking {
            repository.ensureIndex()
        }
    }

    @AfterEach
    fun tearDown() {
        mongo.stop()
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
    fun overwriteId() = runBlocking {
        val ids = PlayerIds()
        ids.add(PlayerId("deviceId", "A"))
        ids.add(PlayerId("deviceId", "B"))
        assertThat(ids.ids, Matchers.hasSize(1))
        assertEquals("B", ids.ids[0].name)
        Unit
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