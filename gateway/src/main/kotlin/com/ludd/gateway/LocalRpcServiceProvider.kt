package com.ludd.gateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LocalRpcServiceProvider: IRpcServiceProvider {

    @Autowired
    private lateinit var echoService: IRpcService

    override fun get(service: String): IRpcService {
        return echoService
    }
}