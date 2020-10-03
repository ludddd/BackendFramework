package com.ludd.player

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ludd.mongo.MongoDatabase
import com.ludd.mongo.lock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

data class PlayerInfo(val name: String)

@Repository
class PlayerInfoRepository {
    @Autowired
    private lateinit var db: MongoDatabase

    suspend fun setName(playerId: ObjectId, name: String) {
        collection().lock(playerId) {
            if (it["profile"] == null) {
                it["profile"] = Document()
            }
            (it["profile"] as Document)["name"] = name
        }
    }

    private suspend fun getPlayer(playerId: ObjectId): Document {
        return (collection().findOneById(playerId)
            ?: throw PlayerNotFoundException(playerId))
    }

    private fun collection() = db.database.getCollection<Document>("player")

    suspend fun getInfo(playerId: ObjectId): PlayerInfo {
        val doc = getPlayer(playerId).get("profile", Document::class.java)
        return withContext(Dispatchers.IO) {
            jacksonObjectMapper().readValue(doc.toJson(), PlayerInfo::class.java)
        }
    }
}

class PlayerNotFoundException(playerId: ObjectId) : Exception("Player $playerId is not found")
