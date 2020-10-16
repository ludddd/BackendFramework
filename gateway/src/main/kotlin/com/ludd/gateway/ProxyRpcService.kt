package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import io.ktor.util.*
import mu.KotlinLogging
import java.lang.Integer.max

private val logger = KotlinLogging.logger {}

data class RpcOptions(val retryCount: Int,
                      val ackEnabled: Boolean)

@KtorExperimentalAPI
class ProxyRpcService(
    private val serviceName: String,
    private val host: String,
    private val port: Int,
    private val rpcOptions: RpcOptions,
    private val connectionFactory: ConnectionFactory = SocketConnectionFactory()
): IRpcService
{
    interface ConnectionFactory {
        suspend fun connect(serviceName: String,
                    host: String,
                    port: Int,
                    rpcOptions: RpcOptions): Connection
    }

    interface Connection {
        suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult
        val isClosed: Boolean
    }

    private var connection: Connection? = null

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        var failCause: Exception? = null
        for (i in 1..getRetryCount()) {
            if (!isConnected()) {
                try {
                    connection = connectionFactory.connect(serviceName, host, port, rpcOptions)
                } catch (e: Exception) {
                    failCause = e
                    continue
                }
            }

            try {
                return connection!!.call(method, arg, sessionContext)
            } catch (e: ConnectionLost) {
                logger.info("Connection with host $host is lost, retrying")
                failCause = e
                continue
            }
        }
        return CallResult(null, failCause!!.toString())
    }

    private fun getRetryCount() = max(1, rpcOptions.retryCount)

    private fun isConnected() = connection?.isClosed == false
}

interface IRpcMessageChannel {
    suspend fun write(msg: Message.InnerRpcRequest)
    suspend fun read(): Message.RpcResponse?
    fun isClosed(): Boolean
}

fun SessionContext.toRequestContext(): Message.RequestContext {
    val builder = Message.RequestContext.newBuilder()
    if (playerId != null) builder.playerId = playerId
    return builder.build()
}
