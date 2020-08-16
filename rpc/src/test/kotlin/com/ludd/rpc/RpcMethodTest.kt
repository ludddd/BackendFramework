package com.ludd.rpc

import com.ludd.test_util.mockSessionContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Component
@RpcService(name = "serviceA")
class ServiceA {

    @Suppress("RedundantSuspendModifier")
    @RpcMethod(name = "methodA")
    suspend fun methodA(arg: ByteArray): ByteArray {
        return arg
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod(name = "methodWrongReturn")
    suspend fun methodWrongReturn(arg: ByteArray): Int {
        return 1
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun methodC(arg: ByteArray): ByteArray {
        return arg
    }
}

@SpringBootTest
class RpcMethodTest {

    @Suppress("unused")
    @Autowired
    private lateinit var rpcService: ServiceA
    @Autowired
    private lateinit var rpcAutoDiscovery: RpcAutoDiscovery

    @Test
    fun discoverMethod() {
        assertTrue { rpcAutoDiscovery.hasMethod("serviceA", "methodA") }
    }

    @Test
    fun optionalName() {
        assertTrue { rpcAutoDiscovery.hasMethod("serviceA", "methodC") }
    }

    @Test
    fun callMethod() = runBlocking {
        val arg = "aaa".encodeToByteArray()
        val rez = rpcAutoDiscovery.call("serviceA","methodA", arg, mockSessionContext())
        assertEquals(arg, rez)
    }

    @Test
    fun callMissingMethod() = runBlocking {
        val arg = "aaa".encodeToByteArray()
        assertThrows<NoMethodException>{ runBlocking {
            rpcAutoDiscovery.call("serviceA","wrongMethod", arg, mockSessionContext()) } }
        Unit
    }

    @Test
    fun callMissingService() = runBlocking {
        val arg = "aaa".encodeToByteArray()
        assertThrows<NoServiceException>{ runBlocking {
            rpcAutoDiscovery.call("wrongService","wrongMethod", arg, mockSessionContext()) } }
        Unit
    }

    @Test
    fun unsupportedMethodReturnType() = runBlocking {
        assertThrows<UnsupportedRpcMethodReturnType>{ runBlocking {
            rpcAutoDiscovery.call("serviceA","methodWrongReturn", "".encodeToByteArray(), mockSessionContext()) } }
        Unit
    }
}