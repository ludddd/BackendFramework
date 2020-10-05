package com.ludd.mongo

import org.bson.Document
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

data class TestStruct(var fieldA: String, var fieldB: Int) {
    @Suppress("unused")
    constructor(): this("", 0)
}

class SubDocumentTest {

    @Test
    fun createNew() {
        val doc = Document()
        val subdoc = SubDocument(doc, "struct", TestStruct::class.java)
        subdoc.value.fieldA = "a"
        subdoc.save()
        assertEquals("a", (doc["struct"] as Document).getString("fieldA"))
    }

    @Test
    fun changeExisting() {
        val doc = Document()
        doc.append("struct", Document().append("fieldA", "a").append("fieldB", 5))
        val subdoc = SubDocument(doc, "struct", TestStruct::class.java)
        assertEquals("a", subdoc.value.fieldA)
        assertEquals(5, subdoc.value.fieldB)
        subdoc.value.fieldA = "b"
        subdoc.save()
        assertEquals("b", (doc["struct"] as Document).getString("fieldA"))
        assertEquals(5, (doc["struct"] as Document).getInteger("fieldB"))
    }

}