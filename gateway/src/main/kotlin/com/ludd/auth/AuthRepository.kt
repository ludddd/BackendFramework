package com.ludd.auth

import com.ludd.mongo.MongoDatabase
import com.ludd.mongo.SubDocument
import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import mu.KotlinLogging
import org.bson.Document
import org.litote.kmongo.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

data class PlayerId(val type: String, val name: String)
data class PlayerIds(val ids: MutableList<PlayerId>) {
    @Suppress("unused")
    constructor(): this(mutableListOf())

    fun add(id: PlayerId) {
        ids.removeIf { it.type == id.type }
        ids.add(id)
    }
}
const val PlayerIdsField = "playerIds"

private val logger = KotlinLogging.logger {}

class DuplicatePlayerIdException(type: String, name: String): Exception("Player with id of type $type and value $name is registered already")

@Repository
class AuthRepository: IAuthRepository {
    @Autowired
    private lateinit var db: MongoDatabase

    override suspend fun findPlayer(type: String, id: String): String? {
        val filter = Filters.elemMatch("$PlayerIdsField.${PlayerIds::ids.name}",
                Filters.and(
            PlayerId::type eq type,
            PlayerId::name eq id))
        val player = collection.findOne(filter)
        return player?.getObjectId("_id")?.toString()
    }

    override suspend fun addPlayer(type: String, id: String): String {
        val doc = Document()
        addPlayerId(doc, type, id)
        val rez = try {
            collection.insertOne(doc)
        } catch (e: MongoWriteException) {
            throw convertException(e, type, id)
        }
        if (!rez.wasAcknowledged()) throw Exception("Failed to add player ($type, $id)")
        return rez.insertedId!!.asObjectId().value.toString()
    }

    private fun addPlayerId(doc: Document, type: String, id: String) {
        val ids = SubDocument(doc, PlayerIdsField, PlayerIds::class.java)
        ids.value.add(PlayerId(type, id))
        ids.save()
    }

    private val collection get() = db.database.getCollection<Document>("player")

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
            "{'$PlayerIdsField.${PlayerIds::ids.name}.${PlayerId::type.name}':1, '$PlayerIdsField.${PlayerIds::ids.name}.${PlayerId::name.name}':1}",
            indexOptions = IndexOptions()
                .background(true)
                .unique(true)
        )
    }

}