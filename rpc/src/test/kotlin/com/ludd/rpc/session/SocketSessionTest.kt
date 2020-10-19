package com.ludd.rpc.session

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.rpc.RpcServer
import com.ludd.rpc.SessionContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

@SpringBootTest(properties = ["rpc.ackEnabled=false"])
internal class SocketSessionTest {

    private val port = 9000
    @Autowired
    private lateinit var factoryConstructor: ConnectionProvider

    private fun createServer(function: () -> CallResult): RpcServer {
        val autoDiscovery = object: IRpcAutoDiscovery {
            override suspend fun call(
                service: String,
                method: String,
                arg: ByteArray,
                sessionContext: SessionContext
            ): CallResult {
                return function()
            }
        }

        @Suppress("DEPRECATION")
        return RpcServer(autoDiscovery, Integer(port))
    }

    @Test
    fun call() = runBlocking{
        val server = createServer { CallResult("bbb".toByteArray(Charset.defaultCharset()), null) }
        server.start()
        val session = createSession()
        val rez = session.call("test", "aaa".toByteArray(Charset.defaultCharset()), SessionContext(InetSocketAddress(0)))
        assertNull(rez.error)
        assertEquals("bbb", rez.result?.toString(Charset.defaultCharset()))
        server.stop()
    }

    @Test
    fun error() = runBlocking {
        val server = createServer { CallResult(null, "error") }
        server.start()
        val session = createSession()
        val rez = session.call("test", "aaa".toByteArray(Charset.defaultCharset()), SessionContext(InetSocketAddress(0)))
        assertEquals("error", rez.error)
        assertNull(rez.result)
        server.stop()
    }

    @Test
    fun exception() = runBlocking {
        val exception = Exception("test")
        val server = createServer { throw exception }
        server.start()
        val session = createSession()
        val rez = session.call("test", "aaa".toByteArray(Charset.defaultCharset()), SessionContext(InetSocketAddress(0)))
        assertEquals(exception.toString(), rez.error)
        assertNull(rez.result)
        server.stop()
    }

    @Test
    fun connectionRefused() = runBlocking {
        assertThrows<java.net.ConnectException> { runBlocking { createSession() } }
        Unit
    }

    @Test
    fun connectionLost() = runBlocking {
        val server = createServer { CallResult(null, null) }
        server.start()
        val session = createSession()
        server.stop()
        assertThrows<NoResponseFromServiceException> {
            runBlocking {
                session.call("test", "aaa".toByteArray(Charset.defaultCharset()), SessionContext(InetSocketAddress(0)))
            }
        }
        Unit
    }

    private suspend fun createSession(): Session {
        return factoryConstructor.create("test", "localhost", 9000).openSession()
    }

    @Test
    fun sessionContext() = runBlocking{

        val autoDiscovery = object: IRpcAutoDiscovery {
            override suspend fun call(
                service: String,
                method: String,
                arg: ByteArray,
                sessionContext: SessionContext
            ): CallResult {
                assertEquals("playerA", sessionContext.playerId)
                return CallResult(null, null)
            }

        }
        @Suppress("DEPRECATION") val server = RpcServer(autoDiscovery, Integer(port))
        server.start()
        val session = createSession()
        val sessionContext = SessionContext(InetSocketAddress(0))
        sessionContext.authenticate("playerA")
        session.call("test", "aaa".toByteArray(Charset.defaultCharset()), sessionContext)
        server.stop()
    }
}