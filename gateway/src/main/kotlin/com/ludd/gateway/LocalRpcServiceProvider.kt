package com.ludd.gateway

import com.ludd.rpc.IRpcService
import com.ludd.rpc.IRpcServiceProvider
import com.ludd.rpc.NoServiceException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("gateway.service_provider", havingValue = "local")
class LocalRpcServiceProvider: IRpcServiceProvider {

    @Autowired
    private lateinit var echoService: IRpcService

    override fun get(service: String): IRpcService {
        if (service == "echo") return echoService
        throw NoServiceException(service)
    }
}