package com.ludd.mongo

import com.ludd.test_utils.KGenericContainer
import com.mongodb.client.model.Filters
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.litote.kmongo.coroutine.CoroutineCollection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration
import java.util.concurrent.TimeUnit

class OptimisticLockException: Exception() {

}

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)    //TODO:  testcontainers changes port on each start so we had to reinit mongo connection
@Timeout(1, unit = TimeUnit.MINUTES)
class OptimisticLockTest {

    private val mongo = KGenericContainer("mongo:4.4.0-bionic")
        .withExposedPorts(27017)
        .withStartupTimeout(Duration.ofMinutes(5))

    @Autowired
    private lateinit var db: MongoDatabase
    private lateinit var collection: CoroutineCollection<Document>

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
        val id = collection.insertOne(Document()).insertedId!!.asObjectId().value

        collection.lock(id) {
            it.append("fieldA", "A")
        }

        val updatedDoc = collection.findOneById(id)!!
        assertEquals(0, updatedDoc.getInteger("version"))
        assertEquals("A", updatedDoc.getString("fieldA"))

        collection.lock(id) {
            it.append("fieldA", "B")
        }

        val updatedDoc1 = collection.findOneById(id)!!
        assertEquals(1, updatedDoc1.getInteger("version"))
        assertEquals("B", updatedDoc1.getString("fieldA"))
    }

    @Test
    fun lock_fails() = runBlocking {
        val id = collection.insertOne(Document()).insertedId!!.asObjectId().value
        Assertions.assertThrows(OptimisticLockException::class.java) {
            runBlocking {
                collection.lock(id) {
                    runBlocking {
                        collection.lock(id) {doc ->
                            doc.append("fieldA", "B")
                        }
                    }
                    it.append("fieldA", "A")
                }
            }
        }
        val updatedDoc = collection.findOneById(id)!!
        assertEquals(100, updatedDoc.getInteger("version"))
        assertEquals("B", updatedDoc.getString("fieldA"))
        Unit
    }

    @Test
    fun lock_with_2nd_try() = runBlocking {
        val id = collection.insertOne(Document()).insertedId!!.asObjectId().value
        var nTry = 0
        collection.lock(id) {
            if (nTry < 1) {
                runBlocking {
                    collection.lock(id) { doc ->
                        doc.append("fieldA", "B")
                    }
                }
                nTry++
            }
            it.append("fieldA", "A")
        }
        val updatedDoc = collection.findOneById(id)!!
        assertEquals(1, updatedDoc.getInteger("version"))
        assertEquals("A", updatedDoc.getString("fieldA"))
        Unit
    }

    @Test
    fun lock_with_3rd_try() = runBlocking {
        val id = collection.insertOne(Document()).insertedId!!.asObjectId().value
        var nTry = 0
        collection.lock(id) {
            if (nTry < 2) {
                runBlocking {
                    collection.lock(id) { doc ->
                        doc.append("fieldA", "B")
                    }
                }
                nTry++
            }
            it.append("fieldA", "A")
        }
        val updatedDoc = collection.findOneById(id)!!
        assertEquals(2, updatedDoc.getInteger("version"))
        assertEquals("A", updatedDoc.getString("fieldA"))
        Unit
    }

    suspend fun CoroutineCollection<Document>.lock(id: ObjectId, block: (doc: Document) -> Unit) {
        for (i in 0..100) {
            val doc: Document = findOneById(id)!!
            block(doc)
            var filter = Filters.eq("_id", id)
            if (doc.containsKey("version")) {
                filter = Filters.and(filter, Filters.eq("version", doc["version"]))
                doc["version"] = doc.getInteger("version") + 1
            } else {
                doc.append("version", 0)
                filter = Filters.and(filter, Filters.not(Filters.exists("version")))
            }
            if (replaceOne(filter, doc).matchedCount == 1L) return
        }
        throw OptimisticLockException()
    }
}

