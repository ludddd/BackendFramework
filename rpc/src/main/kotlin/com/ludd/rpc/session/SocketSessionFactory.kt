package com.ludd.rpc.session

import com.ludd.rpc.conn.SocketWrapperFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SocketSessionFactory(private val serviceName: String,
                           private val host: String,
                           private val port: Int,
                           private val ackEnabled: Boolean): SessionFactory {

    private val socketFactory = SocketWrapperFactory()

    override suspend fun connect(): Session {
        logger.info("Connecting to $host:$port")
        val socket = try {
            socketFactory.connect(host, port)
        } catch (e: Exception) {
            logger.error(e) {"error while connecting to $host:$port"}
            throw e
        }
        return SocketSession(serviceName, socket, ackEnabled)
    }

}