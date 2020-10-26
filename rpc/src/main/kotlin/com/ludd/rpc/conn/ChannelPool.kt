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

class ChannelPool(private val host: String,
                  private val port: Int,
                  private val capacity: Int,
                  private val socketFactory: ChannelProvider): Closeable {

    private val pool = mutableListOf<PooledChannel>()
    private val lock = Mutex()
    private val freeSocketChannel = Channel<PooledChannel>(capacity)
    private val isClosed = AtomicBoolean(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun openChannel(): PooledChannel {
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

    private suspend fun allocate(): PooledChannel {
        val socket = PooledChannel(socketFactory.acquire(host, port), this)
        pool.add(socket)
        return socket
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun closeChannel(channel: PooledChannel) {
        require(pool.contains(channel))
        if (!freeSocketChannel.isClosedForSend) {
            freeSocketChannel.offer(channel)
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

class PooledChannel(
    private val channel: com.ludd.rpc.conn.Channel,
    private val pool: ChannelPool): com.ludd.rpc.conn.Channel by channel {

    override fun close() = runBlocking{
        pool.closeChannel(this@PooledChannel)
    }

    fun free() {
        channel.close()
    }
}