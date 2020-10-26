package com.ludd.gateway

import com.ludd.rpc.IRpcService
import com.ludd.rpc.IRpcServiceProvider
import com.ludd.rpc.NoServiceException
import com.ludd.rpc.RpcAutoDiscovery
import com.ludd.rpc.session.RemoteRpcService
import com.ludd.rpc.session.RemoteRpcServiceConstructor
import io.ktor.util.*
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.ClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct


private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Service
@ConditionalOnProperty("gateway.service_provider", havingValue = "proxy")
class ProxyRpcServiceProvider(
    @Value("\${gateway.services:}") private val servicesInProperties: List<String>
    ): IRpcServiceProvider {

    private val services = mutableMapOf<String, RemoteRpcService>()
    @Autowired
    private lateinit var autoDiscovery: RpcAutoDiscovery
    @Autowired
    private lateinit var remoteRpcServiceConstructor: RemoteRpcServiceConstructor

    override fun get(service: String): IRpcService {
        if (autoDiscovery.hasService(service)) return autoDiscovery.getService(service)
        return services[service] ?: throw NoServiceException(service)
    }

    @PostConstruct
    fun init() = runBlocking {
            servicesFromProperties().forEach {
                services[it.key] = it.value
            }
            discover().forEach {
                services[it.key] = it.value
            }
        }

    private fun servicesFromProperties(): Map<String, RemoteRpcService> {
        return servicesInProperties
            .map { ServiceDescriptor.read(it) }
            .map { it.name to remoteRpcServiceConstructor.create(it.name, it.host, it.port) }
            .toMap()
    }

    private suspend fun discover(): Map<String, RemoteRpcService> {
        try {
            val client = withContext(Dispatchers.IO) {ClientBuilder.cluster().build()}
            Configuration.setDefaultApiClient(client)
            val api = CoreV1Api()
            val k8sServices = api.listServiceForAllNamespaces(false, null, null, "exposed=true", null, null, null, null, null)
            return k8sServices.items.mapNotNull {
                val name = it.metadata?.name
                val port = it.spec?.ports?.get(0)?.targetPort
                logger.info("found service $name:$port")
                when {
                    name == null -> {
                        logger.error("service is missing name")
                        null
                    }
                    port == null -> {
                        logger.error("service $name is missing targetPort")
                        null
                    }
                    else -> {
                        name to remoteRpcServiceConstructor.create(name, name, port.intValue)
                    }
                }
            }.toMap()
        } catch (e: Exception) {
            logger.error(e) { "failed to get list of services" }
            return emptyMap()
        }
    }

    data class ServiceDescriptor(val name: String,
                                 val host: String,
                                 val port: Int) {

        companion object {
            fun read(str: String): ServiceDescriptor {
                val items = str.split(":")
                if (items.size != 3 || items[2].toIntOrNull() == null) {
                    throw WrongServiceStringFormatException(str)
                }
                return ServiceDescriptor(items[0], items[1], items[2].toInt())
            }
        }

    }

    fun servicesList() = services.map { "${it.key}:${it.value.url}" }
}

class WrongServiceStringFormatException(value: String): java.lang.Exception("Wrong service string format: $value. Expecting: 'name:host:port'")

