package com.ludd.gateway

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@OptIn(KtorExperimentalAPI::class)
class SocketConnectionFactory: ProxyRpcService.ConnectionFactory {

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)

    override suspend fun connect(serviceName: String,
                                 host: String,
                                 port: Int,
                                 rpcOptions: RpcOptions
    ): ProxyRpcService.Connection {
        logger.info("Connecting to $host:$port")
        val socket = aSocket(selectorManager).tcp().connect(host, port)
        return ProxyConnection(serviceName, SocketRpcMessageChannel(socket), rpcOptions.ackEnabled)
    }
}