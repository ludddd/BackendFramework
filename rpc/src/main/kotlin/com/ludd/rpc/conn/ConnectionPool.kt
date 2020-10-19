package com.ludd.rpc.conn

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable

class ConnectionPool(private val host: String,
                     private val post: Int,
                     private val capacity: Int) {

    private val pool = mutableListOf<PooledSocket>()
    private val lock = Mutex()
    private val freeSocketChannel = Channel<PooledSocket>()

    suspend fun connect(): PooledSocket {
        while (true) {
            lock.withLock {
                var socket = pool.find { !it.isUsed }
                if (socket != null) return alloc(socket)
                if (pool.size < capacity) {
                    socket = PooledSocket(this)
                    pool.add(socket)
                    return alloc(socket)
                }
            }
            freeSocketChannel.receive()
        }
    }

    suspend fun free(socket: PooledSocket) {
        require(pool.contains(socket))
        assert(socket.isUsed)
        socket.isUsed = false
        freeSocketChannel.offer(socket)
    }

    private fun alloc(socket: PooledSocket): PooledSocket {
        assert(!socket.isUsed)
        socket.isUsed = true
        return socket
    }

    val size: Int
        get() = pool.size

    val usedCount: Int
        get() = runBlocking {
            lock.withLock {
                return@runBlocking pool.count { it.isUsed }
            }
        }
}

class PooledSocket(private val pool: ConnectionPool): Closeable {

    @Volatile
    var isUsed: Boolean = false

    override fun close() = runBlocking{
        pool.free(this@PooledSocket)
    }
}