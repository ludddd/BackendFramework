package com.ludd.gateway

import io.ktor.util.KtorExperimentalAPI
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.ClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service


private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Service
@ConditionalOnProperty("gateway.service_provider", havingValue = "proxy")
class ProxyRpcServiceProvider(
    @Value("\${gateway.services:}") private val servicesInProperties: List<String>
    ): IRpcServiceProvider {

    private val services = mutableListOf<ServiceProxy>()

    override fun get(service: String): IRpcService {
        return services.find { it.name == service }?.proxy ?: throw Exception("No service named $service")
    }

    init {
        runBlocking {
            services.addAll(servicesFromProperties())
            services.addAll(discover())
        }
    }

    private fun servicesFromProperties(): List<ServiceProxy> {
        return servicesInProperties.map {
            val items = it.split(":")
            ServiceProxy(items[0], items[1], items[2].toInt())
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
                        ServiceProxy(name, name, port.intValue)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "failed to get list of services" }
            return emptyList()
        }
    }

    class ServiceProxy(val name: String, val host: String, val port: Int) {
        val proxy: ProxyRpcService by lazy { ProxyRpcService(name, host, port) }
    }

    fun servicesList() = services.map { "${it.name}:${it.host}:${it.port}" }
}

