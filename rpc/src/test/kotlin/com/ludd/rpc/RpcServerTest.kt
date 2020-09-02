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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.stream.Stream
import kotlin.test.assertEquals

open class MockAutoDiscovery(private val function: () -> ByteArray) : IRpcAutoDiscovery {
    override suspend fun call(
        service: String,
        method: String,
        arg: ByteArray,
        sessionContext: SessionContext
    ): ByteArray {
        return function()
    }
}

@SpringBootTest
class RpcServerTest {

    @Test
    fun processMessage() = runBlocking {
        val autoDiscovery = mockService {
            "bbb".toByteArray(Charset.defaultCharset())
        }
        @Suppress("DEPRECATION")
        val server = RpcServer(autoDiscovery, Integer(0))
        val output = ByteChannel()
        server.processMessages(
            rpcRequest().toInputChannel(),
            output,
            SessionContext(InetSocketAddress.createUnresolved("", 0))
        )
        Mockito.verify(autoDiscovery).call(
            "serviceA",
            "methodA",
            "aaa".toByteArray(Charset.defaultCharset()),
            sessionContext()
        )
        val outMsg = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(output.toInputStream())
        }
        assertEquals("bbb", outMsg.result.toString(Charset.defaultCharset()))
        Unit
    }

    private fun mockService(function: () -> ByteArray) = Mockito.spy(MockAutoDiscovery(function))

    companion object {
        @JvmStatic
        private fun exception(): Stream<Exception> = Stream.of(
            NoServiceException("serviceA"),
            NoMethodException("serviceA", "methodA")
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun exception(e: Exception) = runBlocking {
        val autoDiscovery = mockService {
            throw e
        }
        @Suppress("DEPRECATION")
        val server = RpcServer(autoDiscovery, Integer(0))
        val output = ByteChannel()
        server.processMessages(
            rpcRequest().toInputChannel(),
            output,
            SessionContext(InetSocketAddress.createUnresolved("", 0))
        )
        val outMsg = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(output.toInputStream())
        }
        assertEquals(e.toString(), outMsg.error)
    }

    private fun rpcRequest(): Message.InnerRpcRequest {
        return Message.InnerRpcRequest.newBuilder()
            .setService("serviceA")
            .setMethod("methodA")
            .setArg(ByteString.copyFrom("aaa", Charset.defaultCharset()))
            .setContext(
                Message.RequestContext.newBuilder()
                    .setPlayerId("playerA")
            )
            .build()
    }

    private fun sessionContext(): SessionContext {
        val context = SessionContext(InetSocketAddress.createUnresolved("localhost", 0))
        context.authenticate("playerA")
        return context
    }
}