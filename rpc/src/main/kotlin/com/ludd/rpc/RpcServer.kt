package com.ludd.rpc

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.net.InetSocketAddress

private val logger = KotlinLogging.logger {}

@Suppress("EXPERIMENTAL_API_USAGE")
open class RpcServer(private val autoDiscovery: IRpcAutoDiscovery,
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                port: Integer,
                 shutdownTimeoutMs: Long = 30_000): AbstractTcpServer(port.toInt(), shutdownTimeoutMs) {

    @OptIn(KtorExperimentalAPI::class)
    override suspend fun processMessages(
        read: ByteReadChannel,
        write: ByteWriteChannel,
        sessionContext: SessionContext
    ) {
        val inMessage = withContext(Dispatchers.IO) {
            Message.InnerRpcRequest.parseDelimitedFrom(read.toInputStream(coroutineContext[Job]))
        }
        logger.debug("Rpc call ${inMessage.service}:${inMessage.method} is received")
        val responseBuilder = Message.RpcResponse.newBuilder()
        try {
            val rez = autoDiscovery.call(
                inMessage.service,
                inMessage.method,
                inMessage.arg.toByteArray(),
                inMessage.context.toSessionContext()
            )
            responseBuilder.initFromCallResult(rez)
        } catch (e: Exception) {
            logger.error(e) {
                "Error while calling service ${inMessage.service} method ${inMessage.method} with context ${inMessage.context}"
            }
            responseBuilder.hasError = true
            responseBuilder.error = e.toString()
        }
        if (responseBuilder.hasError) {
            logger.debug("Responding to ${inMessage.service}:${inMessage.method} call with error: ${responseBuilder.error}")
        }
        withContext(Dispatchers.IO) {
            responseBuilder.build().writeDelimitedTo(write.toOutputStream(coroutineContext[Job]))
            write.flush()
        }
    }

    private fun Message.RpcResponse.Builder.initFromCallResult(
        rez: CallResult
    ) {
        if (rez.error != null) {
            logger.debug("CallResult has error: ${rez.error}")
            hasError = true
            error = rez.error
        } else {
            result = ByteString.copyFrom(rez.result)
        }
    }
}

fun Message.RequestContext.toSessionContext(): SessionContext {
    //TODO: fix inet address
    val requestContext = SessionContext(InetSocketAddress.createUnresolved("localhost", 0))
    if (!playerIdBytes.isEmpty) {
        requestContext.authenticate(playerId)
    }
    return requestContext
}
