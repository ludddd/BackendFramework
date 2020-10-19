package com.ludd.rpc.conn

import com.google.protobuf.AbstractMessage
import java.io.Closeable
import java.io.InputStream

interface RpcSocket: Closeable {
    val isClosed: Boolean
    suspend fun write(msg: AbstractMessage)
    suspend fun <T> read(msgBuilder: (input: InputStream) -> T): T
}

interface RpcSocketFactory {
    suspend fun connect(host: String, port: Int): RpcSocket
}