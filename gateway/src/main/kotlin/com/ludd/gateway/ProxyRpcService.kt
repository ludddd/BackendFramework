package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import com.ludd.rpc.session.Session
import com.ludd.rpc.session.SessionFactory
import io.ktor.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
class ProxyRpcService(
    private val serviceName: String,
    private val host: String,
    private val port: Int,
    private val sessionFactory: SessionFactory
): IRpcService
{
    private var session: Session? = null

    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        if (!isConnected()) {
            session = sessionFactory.connect(serviceName, host, port)
        }
        return session!!.call(method, arg, sessionContext)
    }

    private fun isConnected() = session?.isClosed == false
}

