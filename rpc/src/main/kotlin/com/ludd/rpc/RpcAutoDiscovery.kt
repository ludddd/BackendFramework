package com.ludd.rpc

import com.google.protobuf.ByteString
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
class RpcAutoDiscovery: IRpcService {

    private val methodMap: Map<String, Method> by lazy {
        val reflections = Reflections(
            "com.ludd",
            MethodAnnotationsScanner()
        )
        val methods = reflections.getMethodsAnnotatedWith(RpcMethod::class.java)
        methods.map { it.name to it }.toMap()
    }

    override suspend fun call(method: String, arg: ByteString): ByteString {
        return methodMap[method]?.let { it(arg) as ByteString }!!
    }

    fun hasMethod(method: String) = methodMap.containsKey(method)
}