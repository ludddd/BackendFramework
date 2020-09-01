package com.ludd.rpc

import com.google.protobuf.ByteString
import com.ludd.rpc.to.Message
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import java.net.InetSocketAddress

@Suppress("EXPERIMENTAL_API_USAGE")
class RpcServer(private val autoDiscovery: IRpcAutoDiscovery,
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                @Value("\${server.port}") port: Integer): AbstractTcpServer(port.toInt()) {

    @OptIn(KtorExperimentalAPI::class)
    override suspend fun processMessages(
        read: ByteReadChannel,
        write: ByteWriteChannel,
        sessionContext: SessionContext
    ) {
        val inMessage = withContext(Dispatchers.IO) {
            Message.InnerRpcRequest.parseDelimitedFrom(read.toInputStream(coroutineContext[Job]))
        }

        val rez = autoDiscovery.call(inMessage.service, inMessage.method, inMessage.arg.toByteArray(), inMessage.context.toSessionContext())

        val outMessage = Message.RpcResponse.newBuilder().setResult(ByteString.copyFrom(rez)).build()
        withContext(Dispatchers.IO) {
            outMessage.writeDelimitedTo(write.toOutputStream(coroutineContext[Job]))
        }
    }

    private fun Message.RequestContext.toSessionContext(): SessionContext {
        //TODO: fix inet address
        val requestContext = SessionContext(InetSocketAddress.createUnresolved("localhost", 0))
        requestContext.authenticate(playerId)
        return requestContext
    }
}