package com.ludd.rpc.session

import com.ludd.rpc.conn.RpcSocketFactory
import com.ludd.rpc.conn.SocketWrapperFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

enum class SocketFactoryType(val type: RpcSocketFactory) {
    Simple(SocketWrapperFactory());
}

@Component
class ConnectionProvider {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.ackEnabled:false}")
    private lateinit var ackEnabled: java.lang.Boolean
    @Value("\${rpc.socket:Simple}")
    private lateinit var factoryType: SocketFactoryType

    fun create(service: String, host: String, port: Int): Connection {
        return Connection(service, host, port, ackEnabled.booleanValue(), factoryType.type)
    }
}