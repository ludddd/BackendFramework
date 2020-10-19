package com.ludd.rpc.session

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ConnectionProvider {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.ackEnabled:false}")
    private lateinit var ackEnabled: java.lang.Boolean

    fun create(service: String, host: String, port: Int): Connection {
        return Connection(service, host, port, ackEnabled.booleanValue())
    }
}