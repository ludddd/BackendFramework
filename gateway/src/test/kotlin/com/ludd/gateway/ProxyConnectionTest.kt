package com.ludd.gateway

import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset

class MockChannel: IRpcMessageChannel {
    var outMessage: Message.InnerRpcRequest? = null
    var inMessage: Message.RpcResponse = Message.RpcResponse.newBuilder().build()
    var closed = false

    override suspend fun write(msg: Message.InnerRpcRequest) {
        outMessage = msg
    }

    override suspend fun read(): Message.RpcResponse {
        return inMessage
    }

    override fun isClosed(): Boolean {
        return closed
    }

}

@SpringBootTest
internal class ProxyConnectionTest {
    @Test
    fun call() = runBlocking {
        val channel = MockChannel()
        val connection = ProxyConnection("test", channel, false)
        val context = SessionContext(InetSocketAddress(0))
        val arg = "aaa".toByteArray(Charset.defaultCharset())
        connection.call("funcA", arg, context)
        assertEquals("aaa", channel.outMessage!!.arg.toString(Charset.defaultCharset()))
        assertEquals("test", channel.outMessage!!.service)
        assertEquals("funcA", channel.outMessage!!.method)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun acknowledgement(ackEnabled: Boolean) = runBlocking {
        val channel = MockChannel()
        val connection = ProxyConnection("test", channel, ackEnabled)
        val context = SessionContext(InetSocketAddress(0))
        val arg = "aaa".toByteArray(Charset.defaultCharset())
        connection.call("funcA", arg, context)
        assertEquals(ackEnabled, channel.outMessage!!.option.ackEnabled)
    }

    @Test
    fun noResponseFromService() = runBlocking {
        val channel = Mockito.mock(IRpcMessageChannel::class.java)
        val connection = ProxyConnection("test", channel, false)
        val context = SessionContext(InetSocketAddress(0))
        val arg = "aaa".toByteArray(Charset.defaultCharset())
        assertThrows<NoResponseFromServiceException>{
            runBlocking { connection.call("funcA", arg, context) } }
        Unit
    }
}