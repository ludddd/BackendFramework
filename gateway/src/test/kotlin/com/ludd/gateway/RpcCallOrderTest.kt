package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.rpc.RpcServer
import com.ludd.rpc.SessionContext
import com.ludd.rpc.conn.SocketWrapperFactory
import com.ludd.rpc.session.Connection
import io.ktor.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.boot.test.context.SpringBootTest
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

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

    @Suppress("EXPERIMENTAL_API_USAGE")
    @OptIn(KtorExperimentalAPI::class)
    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun multipleCallAtOnce() = runBlocking {
        val threadPool = newFixedThreadPoolContext(10, "test")

        val autoDiscovery = MockAutoDiscovery {
            CallResult(it, null)
        }
        val port = 9001

        @Suppress("DEPRECATION")
        val server = RpcServer(autoDiscovery, Integer(port))
        server.start()

        val socketFactory = SocketWrapperFactory()
        val connection = Connection("test", "localhost", port, false, socketFactory)
        val service = ProxyRpcService(connection)

        (1..5).forEach { _ ->
            val data = (1..10).map { it.toString() }
            val barrier = Job()

            val jobs = data.map {
                async(threadPool) {
                    barrier.join()
                    val rez = service.call(
                        "test",
                        it.toByteArray(Charset.defaultCharset()),
                        SessionContext(InetSocketAddress(0))
                    )
                    val resp = rez.result?.toString(Charset.defaultCharset())
                    resp
                }
            }
            barrier.complete()
            val rez = jobs.map { it.await() }
            assertThat(rez, Matchers.allOf(data.map { Matchers.hasItem(it) }))
        }

        server.stop()
    }

}