package com.ludd.rpc.session

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.rpc.RpcServer
import com.ludd.rpc.SessionContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset
import kotlin.test.assertTrue

@SpringBootTest(properties = ["rpc.ackEnabled=true"])
class RemoteRpcServiceAckTest {

    private val port = 9000
    @Autowired
    private lateinit var factoryConstructor: RemoteRpcServiceConstructor

    @Test
    fun call() = runBlocking {

        @Suppress("DEPRECATION")
        val server = createServer { CallResult("bbb".toByteArray(Charset.defaultCharset()), null) }
        server.start()
        val session = factoryConstructor.create("test", "localhost", 9000)
        assertTrue(session.ackEnabled)

        val rez = session.call("test", "aaa".toByteArray(Charset.defaultCharset()), SessionContext(InetSocketAddress(0)))
        Assertions.assertNull(rez.error)
        Assertions.assertEquals("bbb", rez.result?.toString(Charset.defaultCharset()))
        server.stop()
    }

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
        return RpcServer(autoDiscovery, Integer(port), 1)
    }

    @Test
    fun connectionLost() = runBlocking {
        var stopFunc: (() -> Unit)? = null
        val server = createServer { stopFunc!!(); CallResult(null, null) }
        server.start()
        val session = factoryConstructor.create("test", "localhost", 9000)
        stopFunc = { server.stop() }
        assertThrows<NoResponseFromServiceException> {
            runBlocking {
                session.call("test", "aaa".toByteArray(Charset.defaultCharset()), SessionContext(InetSocketAddress(0)))
            }
        }
        Unit
    }
}