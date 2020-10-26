package com.ludd.rpc.conn

import com.google.protobuf.AbstractMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Timeout(10, unit = TimeUnit.SECONDS)
internal class ChannelPoolTest {

    private val factory = object: ChannelProvider {
        override suspend fun acquire(host: String, port: Int): Channel {
            return Mockito.mock(Channel::class.java)
        }
    }

    @Test
    fun connect() = runBlocking{
        val pool = ChannelPool("", 0, 10, factory)
        val socket = pool.openChannel()
        assertNotNull(socket)
        assertEquals(1, pool.size)
    }

    @Test
    fun free() = runBlocking {
        val pool = ChannelPool("", 0, 10, factory)
        val socket = pool.openChannel()
        socket.close()
        assertEquals(1, pool.size)
    }

    @Test
    fun socketReused() = runBlocking {
        val pool = ChannelPool("", 0, 10, factory)
        val socketA = pool.openChannel()
        socketA.close()
        val socketB = pool.openChannel()
        assertEquals(1, pool.size)
        assertSame(socketA, socketB)
    }

    @Test
    fun capacityReached() = runBlocking {
        val pool = ChannelPool("", 0, 1, factory)
        val socketA = pool.openChannel()
        val socketB = async { pool.openChannel() }
        delay(5)
        assertFalse(socketB.isCompleted)
        socketA.close()
    }

    @Test
    fun socketAllocatedWhenFreed() = runBlocking {
        val pool = ChannelPool("", 0, 1, factory)
        val socketA = pool.openChannel()
        val socketB = async { pool.openChannel() }
        delay(5)
        socketA.close()
        delay(5)
        assertTrue(socketB.isCompleted)
        assertSame(socketA, socketB.await())
    }

    @Test
    fun deletePool() = runBlocking {
        val factory = closableSocketFactory()
        val pool = ChannelPool("", 0, 2, factory)
        val socketA = pool.openChannel()
        pool.close()
        assertTrue(socketA.isClosed)
    }

    @Test
    fun cancelSocketWaitingOnPoolDeletion() = runBlocking {
        val factory = closableSocketFactory()
        val pool = ChannelPool("", 0, 1, factory)
        pool.openChannel()

        val socketB = async {
            assertThrows(ConnectionPoolClosedException::class.java) {
                runBlocking {
                    pool.openChannel()
                }
            }
        }
        pool.close()
        socketB.join()
        Unit
    }

    private fun closableSocketFactory(): ChannelProvider {
        return object : ChannelProvider {
            override suspend fun acquire(host: String, port: Int): Channel {
                return object : Channel {
                    override var isClosed: Boolean = false

                    override suspend fun write(msg: AbstractMessage) {
                        throw NotImplementedError()
                    }

                    override suspend fun <T> read(msgBuilder: (input: InputStream) -> T): T {
                        throw NotImplementedError()
                    }

                    override fun close() {
                        isClosed = true
                    }

                }
            }
        }
    }
}
