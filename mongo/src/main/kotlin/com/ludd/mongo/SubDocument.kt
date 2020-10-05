package com.ludd.mongo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.bson.Document

class SubDocument<T>(private val parent: Document, private val field: String, type: Class<T>) {

    val value: T = if (parent[field] != null) {
        jacksonObjectMapper().readValue((parent[field] as Document).toJson(), type)
    } else {
        type.getDeclaredConstructor().newInstance()
    }

    fun save() {
        parent[field] = Document.parse(jacksonObjectMapper().writeValueAsString(value))
    }
}