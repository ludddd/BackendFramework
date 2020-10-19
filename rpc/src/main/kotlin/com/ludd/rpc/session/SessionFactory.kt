package com.ludd.rpc.session

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

interface SessionFactory {
    suspend fun connect(): Session
}

interface SessionFactoryConstructor {
    fun create(service: String, host: String, port: Int): SessionFactory
}

@Component
class DefaultSessionFactoryConstructor: SessionFactoryConstructor {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.ackEnabled:false}")
    private lateinit var ackEnabled: java.lang.Boolean

    override fun create(service: String, host: String, port: Int): SessionFactory {
        return SocketSessionFactory(service, host, port, ackEnabled.booleanValue())
    }
}