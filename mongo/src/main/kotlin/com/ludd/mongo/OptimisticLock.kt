package com.ludd.mongo

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineCollection

const val MAX_LOCK_TRY = 100

class OptimisticLockException(msg: String): Exception(msg)

suspend fun CoroutineCollection<Document>.lock(id: ObjectId, block: (doc: Document) -> Unit) {
    for (i in 0..MAX_LOCK_TRY) {
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
    throw OptimisticLockException("Failed to acquire lock for document $id in collection $namespace")
}