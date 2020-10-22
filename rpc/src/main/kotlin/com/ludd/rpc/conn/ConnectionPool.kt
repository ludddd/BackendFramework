package com.ludd.rpc.conn

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.io.Closeable

private val logger = KotlinLogging.logger {}

class ConnectionPool(private val host: String,
                     private val port: Int,
                     private val capacity: Int,
                     private val socketFactory: RpcSocketFactory): Closeable {

    private val pool = mutableListOf<PooledSocket>()
    private val lock = Mutex()
    private val freeSocketChannel = Channel<PooledSocket>(capacity)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun connect(): PooledSocket {
        val socket = freeSocketChannel.poll()
        if (socket != null) {
            return socket
        }
        lock.withLock {
            if (pool.size < capacity) {
                return allocate()
            }
        }
        return freeSocketChannel.receive()
    }

    private suspend fun allocate(): PooledSocket {
        val socket = PooledSocket(socketFactory.connect(host, port), this)
        pool.add(socket)
        return socket
    }

    fun free(socket: PooledSocket) {
        require(pool.contains(socket))
        freeSocketChannel.offer(socket)
        logger.debug("socket is freed")
    }

    val size: Int
        get() = pool.size

    override fun close() {
        //TODO: wait for all sockets to free and delete them
    }
}

class PooledSocket(
    private val socket: RpcSocket,
    private val pool: ConnectionPool): RpcSocket by socket {

    override fun close() = runBlocking{
        pool.free(this@PooledSocket)
    }
}