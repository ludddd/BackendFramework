package com.ludd.auth

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import mu.KotlinLogging
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.elemMatch
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

data class PlayerId(val type: String, val name: String)

data class Player(val id: Id<Player>, val playerIds: List<PlayerId>)

private val logger = KotlinLogging.logger {}

class DuplicatePlayerIdException(type: String, name: String): Exception("Player with id of type $type and value $name is registered already")

@Component
class MongoDatabase {

    val database: CoroutineDatabase by lazy {
        val mongoUrl = System.getProperty("mongodb.url")
        logger.info("Connecting to mongo: $mongoUrl")
        val client = KMongo.createClient(mongoUrl).coroutine
        client.getDatabase("db")
    }

}

@Repository
class AuthRepository: IAuthRepository {
    @Autowired
    private lateinit var db: MongoDatabase

    override suspend fun findPlayer(type: String, id: String): String? {
        val filter = Player::playerIds elemMatch Filters.and(
            PlayerId::type eq type,
            PlayerId::name eq id)
        val player = collection.findOne(filter)
        return player?.id?.toString()
    }

    override suspend fun addPlayer(type: String, id: String): String {
        val playerId = newId<Player>()
        val rez = try {
            collection.insertOne(Player(playerId, listOf(PlayerId(type, id))))
        } catch (e: MongoWriteException) {
            throw convertException(e, type, id)
        }
        if (!rez.wasAcknowledged()) throw Exception("Failed to add player ($type, $id)")
        return playerId.toString()
    }

    private val collection get() = db.database.getCollection<Player>()

    private fun convertException(e: MongoWriteException, type: String, id: String): Exception {
        return if (e.error.category == ErrorCategory.DUPLICATE_KEY) {
            DuplicatePlayerIdException(type, id)
        } else {
            e
        }
    }

    //TODO: where to call this?
    suspend fun ensureIndex() {
        collection.ensureIndex(
            "{'${Player::playerIds.name}.${PlayerId::type.name}':1, '${Player::playerIds.name}.${PlayerId::name.name}':1}",
            indexOptions = IndexOptions()
                .background(true)
                .unique(true)
        )
    }

}