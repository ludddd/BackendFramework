package com.ludd.fail

import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.nio.charset.Charset
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@Component
@RpcService(name = "fail")
class FailService {

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun crash(arg: ByteArray): ByteArray {
        logger.info("Stopping app by request")
        withContext(Dispatchers.IO) {
            exitProcess(1)
        }
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun echo(arg: ByteArray): ByteArray {
        logger.info("echo is called")
        return arg
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun hostName(arg: ByteArray): ByteArray {
        return withContext(Dispatchers.IO) {
             InetAddress.getLocalHost().hostName.toByteArray(Charset.defaultCharset())
        }
    }
}