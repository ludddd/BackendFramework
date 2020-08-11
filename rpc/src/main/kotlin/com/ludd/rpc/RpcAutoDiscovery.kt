package com.ludd.rpc

import com.google.protobuf.AbstractMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

data class MethodWithBoundArgument(val arg: Any, val method: KFunction<*>)

class NoMethodException(service: String, method: String): Exception("No method $method in service $service")
class NoServiceException(service: String): Exception("No service $service found")

private val logger = KotlinLogging.logger {}

@ExperimentalStdlibApi
@Component
class RpcAutoDiscovery {

    @Autowired
    private lateinit var context: ApplicationContext

    private fun getRpcMethods(obj: Any): Map<String, MethodWithBoundArgument> {
        val annotated = obj.javaClass.kotlin.memberFunctions.filter { it.hasAnnotation<RpcMethod>() }
        return annotated.map {
            it.findAnnotation<RpcMethod>()!!.name to MethodWithBoundArgument(obj, it)
        }.toMap()
    }

    //TODO: check method signature at start up and through exception if there is method with wrong signature
    private val methodMap: Map<String, Map<String, MethodWithBoundArgument>> by lazy {
        logger.info("Scanning for local rpc services")
        val services = context.getBeansWithAnnotation(RpcService::class.java)
        logger.info("found ${services.size} services: ${services.keys.joinToString(",")}")
        services.values.map { it.javaClass.kotlin.findAnnotation<RpcService>()!!.name to getRpcMethods(it) }.toMap()
    }

    suspend fun call(service: String, method: String, arg: ByteArray): ByteArray {
        val serviceMap = methodMap[service] ?: throw NoServiceException(service)
        val func = serviceMap[method] ?: throw NoMethodException(service, method)
        val argType = func.method.parameters[1].type
        val jvmErasure = argType.jvmErasure
        val convertedArg = convertArg(jvmErasure, arg) ?: throw UnsupportedRpcMethodArgumentType(service, method, argType)
        return when (val rez = func.method.callSuspend(func.arg, convertedArg)) {
            is ByteArray -> rez
            is AbstractMessage -> serializeMessage(rez)
            else -> {
                throw UnsupportedRpcMethodReturnType(service, method, rez?.javaClass)
            }
        }
    }

    private fun convertArg(
        jvmErasure: KClass<*>,
        arg: ByteArray
    ): Any? {
        return when {
            jvmErasure.isSubclassOf(ByteArray::class) -> arg
            jvmErasure.isSubclassOf(AbstractMessage::class) -> {
                deserializeMessage(jvmErasure, arg)
            }
            else -> null
        }
    }

    private fun deserializeMessage(msgClass: KClass<*>, arg: ByteArray): Any? {
        val parseMethod = msgClass.staticFunctions.find {
            it.name == "parseDelimitedFrom" &&
                    it.parameters.size == 1 &&
                    it.parameters[0].type.jvmErasure.isSubclassOf(InputStream::class)
        } ?: throw Exception("Failed to find static parseDelimitedFrom method in class $msgClass")
        val rez = parseMethod.call(arg.inputStream())
        return rez
    }

    private suspend fun serializeMessage(rez: AbstractMessage): ByteArray {
        val out = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            rez.writeDelimitedTo(out)
        }
        return out.toByteArray()
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

class UnsupportedRpcMethodArgumentType(service: String, method: String, type: KType) :
    Exception("Method $method in service $service has unsupported argument type $type")

class UnsupportedRpcMethodReturnType(service: String, method: String, javaClass: Class<Any>?)
    : Exception("Method $method in service $service returns object of unsupported class $javaClass")



