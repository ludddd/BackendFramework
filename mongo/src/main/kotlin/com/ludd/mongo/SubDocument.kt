package com.ludd.mongo

import org.bson.BsonDocument
import org.bson.BsonDocumentReader
import org.bson.BsonDocumentWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class SubDocument<T>(private val parent: BsonDocument, private val field: String, private val type: Class<T>) {

    private val codec: Codec<T> by lazy { MongoCodecRegistry.get()[type] }

    val value: T = if (parent[field] != null) {
        codec.decode(
            BsonDocumentReader(
                (parent.getDocument(field))
            ), DecoderContext.builder().build()
        )
    } else {
        type.getDeclaredConstructor().newInstance()
    }

    fun save() {
        val doc = BsonDocument()
        val writer = BsonDocumentWriter(doc)
        codec.encode(writer, value, EncoderContext.builder().build())
        parent[field] = doc
    }
}