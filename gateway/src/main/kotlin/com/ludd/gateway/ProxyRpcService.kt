package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import com.ludd.rpc.session.Connection
import com.ludd.rpc.session.Session
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
class ProxyRpcService(
    private val connection: Connection
): IRpcService
{
    private var session: Session? = null

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        if (!isConnected()) {
            session = connection.openSession()
        }
        return session!!.call(method, arg, sessionContext)
    }

    private fun isConnected() = session?.isClosed == false
}

