package com.ludd.player

import com.ludd.mongo.MongoDatabase
import com.ludd.test_utils.KGenericContainer
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    //TODO:  testcontainers changes port on each start so we had to reinit mongo connection
@Timeout(1, unit = TimeUnit.MINUTES)
internal class PlayerInfoRepositoryTest {

    private val mongo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))

    @Autowired
    private lateinit var repository: PlayerInfoRepository
    @Autowired
    private lateinit var db: MongoDatabase

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
    fun setName() = runBlocking {
        db.database.getCollection<Document>("player").insertOne(Document().append("id", "playerA"))
        repository.setName("playerA", "A")
        assertEquals(PlayerInfo("A"), repository.getInfo("playerA"))
    }
}