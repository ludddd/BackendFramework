package com.ludd.player

import com.ludd.mongo.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

data class PlayerInfo(val name: String) {
    constructor(doc: Document) : this(doc.getString("name"))
}

@Repository
class PlayerInfoRepository {
    @Autowired
    private lateinit var db: MongoDatabase

    suspend fun setName(playerId: String, name: String) {
        collection()
            .updateOne(Filters.eq("id", playerId), Updates.set("profile.name", name))
            .awaitFirst()
    }

    private suspend fun getPlayer(playerId: String): Document {
        return (collection()
            .find(Filters.eq("id", playerId))
            .awaitFirstOrNull()
            ?: throw PlayerNotFoundException(playerId))
    }

    private fun collection() = db.database.database
        .getCollection("player")

    suspend fun getInfo(playerId: String): PlayerInfo {
        val doc = getPlayer(playerId).get("profile", Document::class.java)
        return PlayerInfo(doc)
    }
}

class PlayerNotFoundException(playerId: String) : Exception("Player $playerId is not found")
