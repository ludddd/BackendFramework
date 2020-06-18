package com.ludd.echo_grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class EchoServer(
    private val port: Int,
    val server: Server?) {

    constructor(port: Int) : this(serverBuilder = ServerBuilder.forPort(port), port = port)

    constructor(
        serverBuilder: ServerBuilder<*>,
        port: Int
    ) : this(
        port = port,
        server = serverBuilder.addService(EchoService()).build()
    )

    fun start() {
        if (server == null) {
            logger.error("server not initialized")
            return
        }
        server.start()
        logger.info("Server started, listening on $port")
    }

    @Throws(InterruptedException::class)
    fun stop() {
        logger.info("Stopping server")
        server?.shutdown()?.awaitTermination(30, TimeUnit.SECONDS)
        logger.info("Server is stopped")
    }


    fun awaitTermination() {
        if (server == null) {
            logger.error("server not initialized")
            return
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Stopping server due to jvm shutdown")
            stop()
        })
        server.awaitTermination()
    }

    companion object {
        const val DEFAULT_PORT = 9000

        @JvmStatic
        fun main(args: Array<String>) {
            val port = DEFAULT_PORT
            val server = EchoServer(port)
            server.start()
            server.awaitTermination()
        }
    }
}