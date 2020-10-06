package com.ludd.mongo

import com.mongodb.client.model.Filters
import org.bson.BsonDocument
import org.bson.BsonInt32
import org.bson.types.ObjectId
import org.litote.kmongo.coroutine.CoroutineCollection

const val MAX_LOCK_TRY = 100

class OptimisticLockException(msg: String): Exception(msg)

//TODO: provide Document interface too
class OptimisticLock(private val name: String, private val maxTry: Int = MAX_LOCK_TRY) {
    suspend fun lock(collection: CoroutineCollection<BsonDocument>, id: ObjectId, block: (doc: BsonDocument) -> Unit) {
        for (i in 0..maxTry) {
            val doc: BsonDocument = collection.findOneById(id)!!
            block(doc)
            var filter = Filters.eq("_id", id)
            if (doc.containsKey("version")) {
                filter = Filters.and(filter, Filters.eq("version", doc["version"]))
                doc["version"] = BsonInt32(doc.getInt32("version").value + 1)
            } else {
                doc.append("version", BsonInt32(0))
                filter = Filters.and(filter, Filters.not(Filters.exists("version")))
            }
            if (collection.replaceOne(filter, doc).matchedCount == 1L) return
        }
        throw OptimisticLockException("Failed to acquire lock $name")
    }
}

suspend fun CoroutineCollection<BsonDocument>.lock(id: ObjectId, block: (doc: BsonDocument) -> Unit) {
    OptimisticLock("for document $id in collection $namespace").lock(this, id, block)
}