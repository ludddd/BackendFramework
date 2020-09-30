package com.ludd.mongo

import mu.KotlinLogging
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class MongoDatabase {

    val database: CoroutineDatabase by lazy {
        val mongoUrl = System.getProperty("mongodb.url") ?: System.getenv("mongodb.url")
        logger.info("Connecting to mongo: $mongoUrl")
        val client = KMongo.createClient(mongoUrl).coroutine
        client.getDatabase("db")
    }

}