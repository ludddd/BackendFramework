package com.ludd.rpc

import com.ludd.test_util.mockSessionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun methodD(arg: ByteArray): CallResult {
        return CallResult(arg, null)
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun methodError(arg: ByteArray): CallResult {
        return CallResult(null, "error")
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun exception(arg: ByteArray): CallResult {
        throw Exception("TestException")
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun echo(request: com.ludd.rpc.to.Test.TestMessage, sessionContext: SessionContext): com.ludd.rpc.to.Test.TestMessage
    {
        return request
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
        assertEquals(arg, rez.result)
    }

    @Test
    fun callMethodReturningCallResult() = runBlocking {
        val arg = "aaa".encodeToByteArray()
        val rez = rpcAutoDiscovery.call("serviceA","methodD", arg, mockSessionContext())
        assertEquals(arg, rez.result)
    }

    @Test
    fun callMethodReturningError() = runBlocking {
        val arg = "aaa".encodeToByteArray()
        val rez = rpcAutoDiscovery.call("serviceA","methodError", arg, mockSessionContext())
        assertEquals("error", rez.error)
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

    @Test
    fun exception() = runBlocking {
        val arg = "aaa".encodeToByteArray()
        val rez = rpcAutoDiscovery.call("serviceA","exception", arg, mockSessionContext())
        assertEquals("java.lang.Exception: TestException", rez.error)
    }

    @Test
    fun fullSignature() = runBlocking {
        val arg = com.ludd.rpc.to.Test.TestMessage.newBuilder().setData("aaa").build()
        val rez = rpcAutoDiscovery.call("serviceA","echo", arg.toByteArray(), mockSessionContext())
        val outMsg = withContext(Dispatchers.IO) {
            com.ludd.rpc.to.Test.TestMessage.parseDelimitedFrom(rez.result!!.inputStream())
        }
        assertEquals("aaa", outMsg.data)
    }
}