package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import com.ludd.rpc.session.Session
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
class ProxyRpcService(
    private val session: Session
): IRpcService
{
    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        return session.call(method, arg, sessionContext)
    }
}

