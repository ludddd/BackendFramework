package com.ludd.rpc

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import com.ludd.test_util.toInputChannel
import com.ludd.test_util.toInputStream
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset
import kotlin.test.assertEquals

open class MockAutoDiscovery: IRpcAutoDiscovery {
    override suspend fun call(
        service: String,
        method: String,
        arg: ByteArray,
        sessionContext: SessionContext
    ): ByteArray {
        return "bbb".toByteArray(Charset.defaultCharset())
    }

}

@SpringBootTest
class RpcServerTest {

    @Test
    fun processMessage() = runBlocking {
        val autoDiscovery = Mockito.spy(MockAutoDiscovery())
        val arg = ByteString.copyFrom("aaa", Charset.defaultCharset())
        val context = SessionContext(InetSocketAddress.createUnresolved("localhost", 0))
        context.authenticate("playerA")
        @Suppress("DEPRECATION")
        val server = RpcServer(autoDiscovery, Integer(0))
        val inMsg = Message.InnerRpcRequest.newBuilder()
            .setService("serviceA")
            .setMethod("methodA")
            .setArg(arg)
            .setContext(
                Message.RequestContext.newBuilder()
                    .setPlayerId("playerA")
            )
            .build()
        val output = ByteChannel()
        server.processMessages(
            inMsg.toInputChannel(),
            output,
            SessionContext(InetSocketAddress.createUnresolved("", 0))
        )
        Mockito.verify(autoDiscovery).call(
            "serviceA",
            "methodA",
            "aaa".toByteArray(Charset.defaultCharset()),
            context
        )
        val outMsg = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(output.toInputStream())
        }
        assertEquals("bbb", outMsg.result.toString(Charset.defaultCharset()))
        Unit
    }



}