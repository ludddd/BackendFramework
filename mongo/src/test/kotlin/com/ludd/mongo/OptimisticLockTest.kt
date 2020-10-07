package com.ludd.mongo

import com.ludd.test_utils.KGenericContainer
import kotlinx.coroutines.runBlocking
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.types.ObjectId
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.litote.kmongo.coroutine.CoroutineCollection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    //TODO:  testcontainers changes port on each start so we had to reinit mongo connection
@Timeout(1, unit = TimeUnit.MINUTES)
class OptimisticLockTest {

    private val mongo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))

    @Autowired
    private lateinit var db: MongoDatabase
    private lateinit var collection: CoroutineCollection<BsonDocument>

    @BeforeEach
    fun setUp() {
        mongo.start()
        System.setProperty("mongodb.url", "mongodb://${mongo.host}:${mongo.getMappedPort(27017)}")
        collection = db.database.getCollection("test")
    }

    @AfterEach
    fun tearDown() {
        mongo.stop()
    }

    @Test
    fun lock() = runBlocking{
        val id = collection.insertOne(BsonDocument()).insertedId!!.asObjectId().value

        collection.lock(id) {
            it.append("fieldA", BsonString("A"))
        }

        val updatedDoc = collection.findOneById(id)!!
        assertEquals(0, updatedDoc.getInt32("version").value)
        assertEquals("A", updatedDoc.getString("fieldA").value)

        collection.lock(id) {
            it.append("fieldA", BsonString("B"))
        }

        val updatedDoc1 = collection.findOneById(id)!!
        assertEquals(1, updatedDoc1.getInt32("version").value)
        assertEquals("B", updatedDoc1.getString("fieldA").value)
    }

    @Test
    fun lock_fails() = runBlocking {
        val id = collection.insertOne(BsonDocument()).insertedId!!.asObjectId().value
        Assertions.assertThrows(OptimisticLockException::class.java) {
            runBlocking {
                collection.lock(id) {
                    conflictingLock(id)
                    it.append("fieldA", BsonString("A"))
                }
            }
        }
        val updatedDoc = collection.findOneById(id)!!
        assertEquals(100, updatedDoc.getInt32("version").value)
        assertEquals("B", updatedDoc.getString("fieldA").value)
        Unit
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = [2, 3])
    fun lock_with_n_tries(tryCount: Int) = runBlocking {
        val id = collection.insertOne(BsonDocument()).insertedId!!.asObjectId().value
        var nTry = 0
        collection.lock(id) {
            if (nTry < tryCount) {
                conflictingLock(id)
                nTry++
            }
            it.append("fieldA", BsonString("A"))
        }
        val updatedDoc = collection.findOneById(id)!!
        assertEquals(tryCount, updatedDoc.getInt32("version").value)
        assertEquals("A", updatedDoc.getString("fieldA").value)
        Unit
    }

    private fun conflictingLock(id: ObjectId) {
        runBlocking {
            collection.lock(id) { doc ->
                doc.append("fieldA", BsonString("B"))
            }
        }
    }
}

