package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.AbstractTcpServer
import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcServiceProvider
import com.ludd.rpc.SessionContext
import com.ludd.rpc.to.Message
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Component
@ConditionalOnProperty(name = ["gateway.tcp_server.port"])
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class GatewayServer(@Value("\${gateway.tcp_server.port}") port: Integer):
    AbstractTcpServer(port.toInt()) {

    @Autowired
    private lateinit var serviceProvider: IRpcServiceProvider

    override suspend fun processMessages(
        read: ByteReadChannel,
        write: ByteWriteChannel,
        sessionContext: SessionContext
    ) {
        val message = withContext(Dispatchers.IO) {
            Message.RpcRequest.parseDelimitedFrom(read.toInputStream(coroutineContext[Job]))
        }
        val response = callRpc(message, sessionContext)
        withContext(Dispatchers.IO) {
            response.writeDelimitedTo(write.toOutputStream(coroutineContext[Job]))
        }
    }

    private suspend fun callRpc(
        message: Message.RpcRequest,
        sessionContext: SessionContext
    ): Message.RpcResponse {
        val responseBuilder = Message.RpcResponse.newBuilder()
        try {
            logger.info("message for service ${message.service} received")
            val service = serviceProvider.get(message.service)
            val result = service.call(message.method, message.arg.toByteArray(), sessionContext)
            if (result.error != null) {
                logger.debug("Error received from service ${message.service}: ${result.error}")
            }
            responseBuilder.initFromCallResult(result)
        } catch (e: Exception) {
            logger.error(e) {"Error while calling service ${message.service} method ${message.method} with context $sessionContext"}
            responseBuilder.initFromError(e)
        }
        return responseBuilder.build()
    }

    private fun Message.RpcResponse.Builder.initFromError(e: Exception) {
        hasError = true
        error = e.toString()
    }

    private fun Message.RpcResponse.Builder.initFromCallResult(
        callResult: CallResult
    ) {
        if (callResult.error != null) {
            hasError = true
            error = callResult.error
        } else {
            result = ByteString.copyFrom(callResult.result)
        }
    }

    @PostConstruct
    fun onPostConstruct() {
        start()
    }

    @PreDestroy
    fun onPreDestroy() {
        super.stop()
    }
}

