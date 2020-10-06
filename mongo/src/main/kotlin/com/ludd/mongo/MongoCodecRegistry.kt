package com.ludd.mongo

import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

class MongoCodecRegistry {

    companion object {
        //TODO: thread safety!!!
        private var codecRegistry = com.mongodb.MongoClientSettings.getDefaultCodecRegistry()

        fun <T> register(type: Class<T>) {
            codecRegistry = CodecRegistries.fromRegistries(
                codecRegistry,
                CodecRegistries.fromProviders(PojoCodecProvider.builder().register(type).build())
            )
        }

        fun get() = codecRegistry
    }
}