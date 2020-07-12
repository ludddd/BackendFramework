package com.ludd.gateway

import io.ktor.util.KtorExperimentalAPI
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@KtorExperimentalAPI
@Service
@ConditionalOnProperty("gateway.service_provider", havingValue = "proxy")
class ProxyRpcServiceProvider: IRpcServiceProvider {

    @Value("\${echo_server.host}")
    private lateinit var echoHost: String
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${echo_server.port}")
    private lateinit var echoPort: Integer

    private val echoProxy: ProxyRpcService by lazy { ProxyRpcService("echo", echoHost, echoPort.toInt()) }

    override fun get(service: String): IRpcService {
        return echoProxy
    }
}