package com.ludd.gateway

import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
        val connection = ProxyConnection("test", channel)
        val context = SessionContext(InetSocketAddress(0))
        val arg = "aaa".toByteArray(Charset.defaultCharset())
        connection.call("funcA", arg, context)
        assertEquals("aaa", channel.outMessage!!.arg.toString(Charset.defaultCharset()))
        assertEquals("test", channel.outMessage!!.service)
        assertEquals("funcA", channel.outMessage!!.method)
    }
}