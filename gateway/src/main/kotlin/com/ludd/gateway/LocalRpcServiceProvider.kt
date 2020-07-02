package com.ludd.gateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("gateway.service_provider", havingValue = "local")
class LocalRpcServiceProvider: IRpcServiceProvider {

    @Autowired
    private lateinit var echoService: IRpcService

    override fun get(service: String): IRpcService {
        return echoService
    }
}