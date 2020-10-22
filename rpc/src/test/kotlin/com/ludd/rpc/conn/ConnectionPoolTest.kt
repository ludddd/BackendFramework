package com.ludd.rpc.conn

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

@Timeout(5, unit = TimeUnit.SECONDS)
internal class ConnectionPoolTest {

    private val factory = object: RpcSocketFactory {
        override suspend fun connect(host: String, port: Int): RpcSocket {
            return Mockito.mock(RpcSocket::class.java)
        }
    }

    @Test
    fun connect() = runBlocking{
        val pool = ConnectionPool("", 0, 10, factory)
        val socket = pool.connect()
        assertNotNull(socket)
        assertEquals(1, pool.size)
    }

    @Test
    fun free() = runBlocking {
        val pool = ConnectionPool("", 0, 10, factory)
        val socket = pool.connect()
        socket.close()
        assertEquals(1, pool.size)
    }

    @Test
    fun socketReused() = runBlocking {
        val pool = ConnectionPool("", 0, 10, factory)
        val socketA = pool.connect()
        socketA.close()
        val socketB = pool.connect()
        assertEquals(1, pool.size)
        assertSame(socketA, socketB)
    }

    @Test
    fun capacityReached() = runBlocking {
        val pool = ConnectionPool("", 0, 1, factory)
        val socketA = pool.connect()
        val socketB = async { pool.connect() }
        delay(5)
        assertFalse(socketB.isCompleted)
        socketA.close()
    }

    @Test
    fun socketAllocatedWhenFreed() = runBlocking {
        val pool = ConnectionPool("", 0, 1, factory)
        val socketA = pool.connect()
        val socketB = async { pool.connect() }
        delay(5)
        socketA.close()
        delay(5)
        assertTrue(socketB.isCompleted)
        assertSame(socketA, socketB.await())
    }
}
