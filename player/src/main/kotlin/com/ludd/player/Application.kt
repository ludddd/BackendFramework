package com.ludd.player

import com.ludd.rpc.RpcServer
import io.ktor.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication(scanBasePackages = ["com.ludd.rpc", "com.ludd.player", "com.ludd.mongo"])
class Application

@KtorExperimentalAPI
@ConditionalOnProperty(name = ["player_server.autostart"], havingValue = "true")
@Component
class ServerRunner: ApplicationRunner {
    @Autowired
    private lateinit var server: RpcServer

    override fun run(args: ApplicationArguments?) {
        server.waitTillTermination()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}