package com.ludd.rpc

import org.junit.jupiter.api.Test
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import kotlin.test.assertTrue

@Component
class RpcService {

    @RpcMethod(name = "methodA")
    suspend fun methodA(arg: String): String {
        return arg
    }
}

@SpringBootTest
class RpcMethodTest {

    @Autowired
    private lateinit var rpcService: RpcService
    @Autowired
    private lateinit var rpcAutoDiscovery: RpcAutoDiscovery

    @Test
    fun discoverMethod() {
        val reflections = Reflections("com.ludd", MethodAnnotationsScanner())
        val methods = reflections.getMethodsAnnotatedWith(RpcMethod::class.java)
        assertTrue { rpcAutoDiscovery.hasMethod("methodA") }
    }
}