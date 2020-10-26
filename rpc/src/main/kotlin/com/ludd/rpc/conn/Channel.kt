package com.ludd.rpc.conn

import com.google.protobuf.AbstractMessage
import java.io.Closeable
import java.io.InputStream

interface Channel: Closeable {
    val isClosed: Boolean
    suspend fun write(msg: AbstractMessage)
    suspend fun <T> read(msgBuilder: (input: InputStream) -> T): T
}

interface ChannelProvider {
    suspend fun acquire(host: String, port: Int): Channel
}