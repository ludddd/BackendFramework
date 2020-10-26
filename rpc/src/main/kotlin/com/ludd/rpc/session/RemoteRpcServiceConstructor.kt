package com.ludd.rpc.session

import com.ludd.rpc.conn.RpcSocketFactory
import com.ludd.rpc.conn.SocketWrapperFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

enum class SocketFactoryType() {
    Simple,
    Pooled;
}

@Component
class RemoteRpcServiceConstructor {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.ackEnabled:false}")
    private lateinit var ackEnabled: java.lang.Boolean
    @Value("\${rpc.socket:Simple}")
    private lateinit var factoryType: SocketFactoryType
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Value("\${rpc.conn_pool_size:10}")
    private lateinit var connPoolSize: Integer

    fun create(service: String, host: String, port: Int): RemoteRpcService {
        return RemoteRpcService(service, host, port, ackEnabled.booleanValue(), createFactory(factoryType))
    }

    private fun createFactory(type: SocketFactoryType): RpcSocketFactory {
        return when(type) {
            SocketFactoryType.Simple -> SocketWrapperFactory()
            SocketFactoryType.Pooled -> PooledSocketFactory(connPoolSize.toInt())
        }
    }
}