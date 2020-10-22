package com.ludd.rpc.conn

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

class ConnectionPoolClosedException(cause: Throwable? = null): Exception(cause)

class ConnectionPool(private val host: String,
                     private val port: Int,
                     private val capacity: Int,
                     private val socketFactory: RpcSocketFactory): Closeable {

    private val pool = mutableListOf<PooledSocket>()
    private val lock = Mutex()
    private val freeSocketChannel = Channel<PooledSocket>(capacity)
    private val isClosed = AtomicBoolean(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun connect(): PooledSocket {
        if (isClosed.get()) throw ConnectionPoolClosedException()
        val socket = freeSocketChannel.poll()
        if (socket != null) {
            return socket
        }
        lock.withLock {
            if (pool.size < capacity) {
                return allocate()
            }
        }
        try {
            return freeSocketChannel.receive()
        } catch (e: kotlinx.coroutines.channels.ClosedReceiveChannelException) {
            throw ConnectionPoolClosedException(e)
        }
    }

    private suspend fun allocate(): PooledSocket {
        val socket = PooledSocket(socketFactory.connect(host, port), this)
        pool.add(socket)
        return socket
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun free(socket: PooledSocket) {
        require(pool.contains(socket))
        if (!freeSocketChannel.isClosedForSend) {
            freeSocketChannel.offer(socket)
        }
        logger.debug("socket is freed")
    }

    val size: Int
        get() = pool.size

    override fun close() = runBlocking{
        require(!isClosed.get())
        lock.withLock {
            isClosed.set(true)
            freeSocketChannel.close()
            pool.forEach { it.free() }
        }
        Unit
    }
}

class PooledSocket(
    private val socket: RpcSocket,
    private val pool: ConnectionPool): RpcSocket by socket {

    override fun close() = runBlocking{
        pool.free(this@PooledSocket)
    }

    fun free() {
        socket.close()
    }
}