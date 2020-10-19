package com.ludd.rpc.conn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ConnectionPool(private val host: String,
                     private val port: Int,
                     private val capacity: Int,
                     private val socketFactory: RpcSocketFactory) {

    private val pool = mutableListOf<PooledSocket>()
    private val lock = Mutex()
    private val freeSocketChannel = Channel<PooledSocket>()

    suspend fun connect(): PooledSocket {
        while (true) {
            lock.withLock {
                var socket = pool.find { !it.isUsed }
                if (socket != null) return alloc(socket)
                if (pool.size < capacity) {
                    socket = PooledSocket(socketFactory.connect(host, port), this)
                    pool.add(socket)
                    return alloc(socket)
                }
            }
            freeSocketChannel.receive()
        }
    }

    fun free(socket: PooledSocket) {
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

class PooledSocket(
    private val socket: RpcSocket,
    private val pool: ConnectionPool): RpcSocket by socket {

    @Volatile
    var isUsed: Boolean = false

    override fun close() = runBlocking{
        withContext(Dispatchers.IO) {
            socket.close()
        }
        pool.free(this@PooledSocket)
    }
}