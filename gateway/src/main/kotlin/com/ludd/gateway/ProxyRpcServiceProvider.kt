package com.ludd.gateway

import io.ktor.util.KtorExperimentalAPI
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@KtorExperimentalAPI
@Service
@ConditionalOnProperty("gateway.service_provider", havingValue = "proxy")
class ProxyRpcServiceProvider: IRpcServiceProvider {

    private val echoProxy = ProxyRpcService("echo", "0:0:0:0:0:0:0:0", 9001)

    override fun get(service: String): IRpcService {
        return echoProxy
    }
}