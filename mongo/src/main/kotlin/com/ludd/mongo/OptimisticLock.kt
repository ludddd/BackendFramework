package com.ludd.mongo

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineCollection

class OptimisticLockException: Exception()

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