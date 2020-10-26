package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.rpc.RpcServer
import com.ludd.rpc.SessionContext
import com.ludd.rpc.conn.ChannelProvider
import com.ludd.rpc.conn.SocketChannelProvider
import com.ludd.rpc.session.PooledSocketFactory
import com.ludd.rpc.session.RemoteRpcService
import io.ktor.util.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

private val logger = KotlinLogging.logger {}

open class MockAutoDiscovery(private val function: (arg: ByteArray) -> CallResult) : IRpcAutoDiscovery {
    override suspend fun call(
        service: String,
        method: String,
        arg: ByteArray,
        sessionContext: SessionContext
    ): CallResult {
        return function(arg)
    }
}

@SpringBootTest
class RpcCallOrderTest {

    companion object {
        @JvmStatic
        private fun multipleCallAtOnce(): Stream<ChannelProvider> = Stream.of(
            SocketChannelProvider(),
            PooledSocketFactory(5)
        )
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    @OptIn(KtorExperimentalAPI::class)
    @Timeout(1, unit = TimeUnit.MINUTES)
    @ParameterizedTest
    @MethodSource
    fun multipleCallAtOnce(socketFactory: ChannelProvider) = runBlocking {
        val threadPool = newFixedThreadPoolContext(10, "test")

        val autoDiscovery = MockAutoDiscovery {
            CallResult(it, null)
        }
        val port = 9001

        @Suppress("DEPRECATION")
        val server = RpcServer(autoDiscovery, Integer(port))
        server.start()

        val service = RemoteRpcService("test", "localhost", port, false, socketFactory)

        (1..5).forEach { _ ->
            val data = (1..10).map { it.toString() }
            val barrier = Job()

            val jobs = data.map {
                callRpcAsync(threadPool, barrier, service, it)
            }
            barrier.complete()
            val rez = jobs.map { it.await() }
            assertThat(rez, Matchers.allOf(data.map { Matchers.hasItem(it) }))
        }

        server.stop()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun CoroutineScope.callRpcAsync(
        threadPool: ExecutorCoroutineDispatcher,
        barrier: CompletableJob,
        service: RemoteRpcService,
        arg: String
    ): Deferred<String?> {
        return async(threadPool) {
            barrier.join()
            val rez = service.call(
                "test",
                arg.toByteArray(Charset.defaultCharset()),
                SessionContext(InetSocketAddress(0))
            )
            val resp = rez.result?.toString(Charset.defaultCharset())
            resp
        }
    }

}