package com.ludd.rpc

import com.google.protobuf.ByteString
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

data class MethodWithBoundArgument(val arg: Any, val method: KFunction<*>) {}

class NoMethodException(service: String, method: String): Exception("No method $method in service $service")
class NoServiceException(service: String): Exception("No service $service found")

private val logger = KotlinLogging.logger {}

@Component
class RpcAutoDiscovery {

    @Autowired
    private lateinit var context: ApplicationContext

    @OptIn(ExperimentalStdlibApi::class)
    private fun getRpcMethods(obj: Any): Map<String, MethodWithBoundArgument> {
        val annotated = obj.javaClass.kotlin.memberFunctions.filter { it.hasAnnotation<RpcMethod>() }
        return annotated.map {
            it.findAnnotation<RpcMethod>()!!.name to MethodWithBoundArgument(obj, it)
        }.toMap()
    }

    private val methodMap: Map<String, Map<String, MethodWithBoundArgument>> by lazy {
        logger.info("Scanning for local rpc services")
        val services = context.getBeansWithAnnotation(RpcService::class.java)
        logger.info("found ${services.size} services: ${services.keys.joinToString(",")}")
        services.values.map { it.javaClass.kotlin.findAnnotation<RpcService>()!!.name to getRpcMethods(it) }.toMap()
    }

    suspend fun call(service: String, method: String, arg: ByteArray): ByteArray {
        val serviceMap = methodMap[service] ?: throw NoServiceException(service)
        val func = serviceMap[method] ?: throw NoMethodException(service, method)
        val rez = func.method.callSuspend(func.arg, arg)
        return (rez as ByteArray)
    }

    fun hasMethod(service: String, method: String) = methodMap[service]?.containsKey(method) ?: false
    fun hasService(service: String): Boolean = methodMap.containsKey(service)
    fun getService(service: String): IRpcService {
        return object : IRpcService {
            override suspend fun call(method: String, arg: ByteArray): ByteArray {
                return call(service, method, arg)
            }
        }

    }
}

