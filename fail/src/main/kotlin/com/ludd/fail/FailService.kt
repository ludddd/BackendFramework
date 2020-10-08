package com.ludd.fail

import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

@Component
@RpcService(name = "fail")
class FailService {

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun crash(arg: ByteArray): ByteArray {
        logger.info("Stopping app by request")
        exitProcess(1)
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun echo(arg: ByteArray): ByteArray {
        logger.info("echo is called")
        return arg
    }
}