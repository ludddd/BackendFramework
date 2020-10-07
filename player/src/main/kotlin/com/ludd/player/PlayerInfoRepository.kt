package com.ludd.player

import com.ludd.mongo.MongoCodecRegistry
import com.ludd.mongo.MongoDatabase
import com.ludd.mongo.SubDocument
import com.ludd.mongo.lock
import org.bson.BsonDocument
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

data class PlayerInfo(var name: String) {
    @Suppress("unused")
    constructor(): this("")
}

const val ProfileFieldName = "profile"

@Repository
class PlayerInfoRepository {
    @Autowired
    private lateinit var db: MongoDatabase

    init {
        MongoCodecRegistry.register(PlayerInfo::class.java)
    }

    suspend fun setName(playerId: ObjectId, name: String) {
        collection().lock(playerId) {
            val playerInfo = SubDocument(it, ProfileFieldName, PlayerInfo::class.java)
            playerInfo.value.name = name
            playerInfo.save()
        }
    }

    private suspend fun getPlayer(playerId: ObjectId): BsonDocument {
        return (collection().findOneById(playerId)
            ?: throw PlayerNotFoundException(playerId))
    }

    private fun collection() = db.database.getCollection<BsonDocument>("player")

    suspend fun getInfo(playerId: ObjectId): PlayerInfo {
        return SubDocument(getPlayer(playerId), ProfileFieldName, PlayerInfo::class.java).value
    }
}

class PlayerNotFoundException(playerId: ObjectId) : Exception("Player $playerId is not found")
