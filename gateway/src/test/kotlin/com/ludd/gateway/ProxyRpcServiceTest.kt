package com.ludd.gateway

import com.ludd.rpc.SessionContext
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.net.InetSocketAddress
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(KtorExperimentalAPI::class)
class ProxyRpcServiceTest {

    @Test
    fun retryWhenConnectionFailed() = runBlocking {
        val arg = "aaa".toByteArray(Charset.defaultCharset())
        val rpcOptions = RpcOptions(3, false)
        val sessionContext = SessionContext(InetSocketAddress(0))

        val factory = Mockito.mock(ProxyRpcService.ConnectionFactory::class.java)
        Mockito.`when`(factory.connect("test", "", 0, rpcOptions)).then {throw Exception("Connection failed")}
        val service = ProxyRpcService("test", "", 0, rpcOptions, factory)

        val rez = service.call("foo", arg, sessionContext)

        Mockito.verify(factory, Mockito.times(3)).connect("test", "", 0, rpcOptions)
        assertNull(rez.result)
        assertEquals(rez.error, "java.lang.Exception: Connection failed")
        Unit
    }

    @Test
    fun retryWhenCallFailed() = runBlocking {
        val arg = "aaa".toByteArray(Charset.defaultCharset())
        val rpcOptions = RpcOptions(3, false)
        val sessionContext = SessionContext(InetSocketAddress(0))

        val connection = Mockito.mock(ProxyRpcService.Connection::class.java)
        Mockito.`when`(connection.call("foo", arg, sessionContext)).then {throw ConnectionLost()}
        val factory = Mockito.mock(ProxyRpcService.ConnectionFactory::class.java)
        Mockito.`when`(factory.connect("test", "", 0, rpcOptions)).thenReturn(connection)
        val service = ProxyRpcService("test", "", 0, rpcOptions, factory)

        val rez = service.call("foo", arg, sessionContext)

        Mockito.verify(connection, Mockito.times(3)).call("foo", arg, sessionContext)
        assertNull(rez.result)
        assertEquals(rez.error, "com.ludd.gateway.ConnectionLost")
        Unit
    }
}