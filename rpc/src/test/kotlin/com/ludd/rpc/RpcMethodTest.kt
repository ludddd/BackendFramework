package com.ludd.rpc

import com.google.protobuf.ByteString
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Component
@RpcService(name = "serviceA")
class ServiceA {

    @Suppress("RedundantSuspendModifier")
    @RpcMethod(name = "methodA")
    suspend fun methodA(arg: ByteString): ByteString {
        return arg
    }
}

@ExperimentalStdlibApi
@SpringBootTest
class RpcMethodTest {

    @Autowired
    private lateinit var rpcService: ServiceA
    @Autowired
    private lateinit var rpcAutoDiscovery: RpcAutoDiscovery

    @Test
    fun discoverMethod() {
        assertTrue { rpcAutoDiscovery.hasMethod("serviceA", "methodA") }
    }

    @Test
    fun callMethod() = runBlocking {
        val arg = ByteString.copyFrom("aaa", Charset.defaultCharset())
        val rez = rpcAutoDiscovery.call("serviceA","methodA", arg)
        assertEquals(arg, rez)
    }
}