package com.ludd.auth

import com.mongodb.client.model.Filters
import mu.KotlinLogging
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.elemMatch
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.reactivestreams.KMongo
import org.springframework.stereotype.Repository

data class PlayerId(val type: String, val name: String)

data class Player(val id: Id<Player>, val playerIds: List<PlayerId>)

private val logger = KotlinLogging.logger {}

@Repository
class AuthRepository: IAuthRepository {

/*
    @Value("\${mongodb.url}")
    private lateinit var mongoUrl: String
*/

    private val client: CoroutineClient by lazy {
        val mongoUrl = System.getProperty("mongodb.url")
        logger.info("Connecting to mongo: $mongoUrl")
        KMongo.createClient(mongoUrl).coroutine
    }
    private val database: CoroutineDatabase by lazy { client.getDatabase("db") }

    override suspend fun findPlayer(type: String, id: String): String? {
        val filter = Player::playerIds elemMatch Filters.and(
            PlayerId::type eq type,
            PlayerId::name eq id)
        val player = database.getCollection<Player>().findOne(filter)
        return player?.id?.toString()
    }

    override suspend fun addPlayer(type: String, id: String): String {
        val playerId = newId<Player>()
        val rez = database.getCollection<Player>().insertOne(Player(playerId, listOf(PlayerId(type, id))))
        if (!rez.wasAcknowledged()) throw Exception("Failed to add player ($type, $id)")
        return playerId.toString()
    }
}