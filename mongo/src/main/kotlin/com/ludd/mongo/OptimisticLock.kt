package com.ludd.mongo

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineCollection

const val MAX_LOCK_TRY = 100

class OptimisticLockException(msg: String): Exception(msg)

class OptimisticLock(private val name: String, private val maxTry: Int = MAX_LOCK_TRY) {
    suspend fun lock(collection: CoroutineCollection<Document>, id: ObjectId, block: (doc: Document) -> Unit) {
        for (i in 0..maxTry) {
            val doc: Document = collection.findOneById(id)!!
            block(doc)
            var filter = Filters.eq("_id", id)
            if (doc.containsKey("version")) {
                filter = Filters.and(filter, Filters.eq("version", doc["version"]))
                doc["version"] = doc.getInteger("version") + 1
            } else {
                doc.append("version", 0)
                filter = Filters.and(filter, Filters.not(Filters.exists("version")))
            }
            if (collection.replaceOne(filter, doc).matchedCount == 1L) return
        }
        throw OptimisticLockException("Failed to acquire lock $name")
    }
}

suspend fun CoroutineCollection<Document>.lock(id: ObjectId, block: (doc: Document) -> Unit) {
    OptimisticLock("for document $id in collection $namespace").lock(this, id, block)
}