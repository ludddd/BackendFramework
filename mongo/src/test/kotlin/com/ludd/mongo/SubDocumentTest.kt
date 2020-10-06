package com.ludd.mongo

import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.BsonString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

data class TestStruct(var fieldA: String, var fieldB: Int) {
    @Suppress("unused")
    constructor(): this("", 0)
}

class SubDocumentTest {

    @BeforeEach
    fun setUp() {
        MongoCodecRegistry.register(TestStruct::class.java)
    }

    @Test
    fun createNew() {
        val doc = BsonDocument()
        val subdoc = SubDocument(doc, "struct", TestStruct::class.java)
        subdoc.value.fieldA = "a"
        subdoc.save()
        assertEquals("a", (doc["struct"] as BsonDocument).getString("fieldA").value)
    }

    @Test
    fun changeExisting() {
        val doc = BsonDocument().append("struct", BsonDocument().append("fieldA", BsonString("a")).append("fieldB", BsonInt32(5)))
        val subdoc = SubDocument(doc, "struct", TestStruct::class.java)
        assertEquals("a", subdoc.value.fieldA)
        assertEquals(5, subdoc.value.fieldB)
        subdoc.value.fieldA = "b"
        subdoc.save()
        assertEquals("b", (doc["struct"] as BsonDocument).getString("fieldA").value)
        assertEquals(5, (doc["struct"] as BsonDocument).getInt32("fieldB").value)
    }

}