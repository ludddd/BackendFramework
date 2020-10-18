package com.ludd.fail

import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.rpc.RpcServer
import io.ktor.util.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

@SpringBootApplication(scanBasePackages = ["com.ludd.rpc", "com.ludd.fail"])
class Application

@Component
class Server(@Autowired autoDiscovery: IRpcAutoDiscovery,
             @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
             @Value("\${server.port}") port: Integer): RpcServer(autoDiscovery, port, 1)
{
    @PostConstruct
    fun onPostConstruct() {
        start()
    }

    @PreDestroy
    fun onPreDestroy() {
        super.stop()
    }
}

@KtorExperimentalAPI
@ConditionalOnProperty(name = ["fail_server.autostart"], havingValue = "true")
@Component
class ServerRunner: ApplicationRunner {
    @Autowired
    private lateinit var server: Server

    override fun run(args: ApplicationArguments?) {
        server.waitTillTermination()
        logger.info("Application is terminated")
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}