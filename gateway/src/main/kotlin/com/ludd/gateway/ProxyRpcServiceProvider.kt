package com.ludd.gateway

import com.ludd.rpc.IRpcService
import com.ludd.rpc.IRpcServiceProvider
import com.ludd.rpc.NoServiceException
import com.ludd.rpc.RpcAutoDiscovery
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

    private val services = mutableListOf<ServiceProxy>()
    @Autowired
    private lateinit var autoDiscovery: RpcAutoDiscovery
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "RemoveRedundantQualifierName")
    @Value("\${rpc.retry:3}")
    private lateinit var retryCount: java.lang.Integer
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.ackEnabled:false}")
    private lateinit var ackEnabled: java.lang.Boolean

    override fun get(service: String): IRpcService {
        if (autoDiscovery.hasService(service)) return autoDiscovery.getService(service)
        return services.find { it.name == service }?.proxy ?: throw NoServiceException(service)
    }

    @PostConstruct
    fun init() = runBlocking {
            services.addAll(servicesFromProperties())
            services.addAll(discover())
        }

    private fun servicesFromProperties(): List<ServiceProxy> {
        return servicesInProperties.map {
            ServiceProxy.parse(it, rpcOptions())
        }
    }

    private suspend fun discover(): List<ServiceProxy> {
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
                        ServiceProxy(name, name, port.intValue, rpcOptions())
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "failed to get list of services" }
            return emptyList()
        }
    }

    fun rpcOptions() = RpcOptions(retryCount.toInt(), ackEnabled.booleanValue())

    class ServiceProxy(val name: String, val host: String, val port: Int, val rpcOptions: RpcOptions) {
        val proxy: ProxyRpcService by lazy { ProxyRpcService(name, host, port, rpcOptions) }

        companion object {
            fun parse(str: String, rpcOptions: RpcOptions): ServiceProxy {
                val items = str.split(":")
                if (items.size != 3 || items[2].toIntOrNull() == null) {
                    throw WrongServiceStringFormatException(str)
                }
                return ServiceProxy(items[0], items[1], items[2].toInt(), rpcOptions)
            }
        }
    }

    fun servicesList() = services.map { "${it.name}:${it.host}:${it.port}" }
}

class WrongServiceStringFormatException(value: String): java.lang.Exception("Wrong service string format: $value. Expecting: 'name:host:port'")

