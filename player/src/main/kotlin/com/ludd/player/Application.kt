package com.ludd.player

import io.ktor.util.*
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication(scanBasePackages = ["com.ludd.rpc", "com.ludd.player"])
class Application

@KtorExperimentalAPI
@ConditionalOnProperty(name = ["player_server.autostart"], havingValue = "true")
@Component
class ServerRunner: ApplicationRunner {
    //@Autowired
    //private lateinit var server: EchoServer

    override fun run(args: ApplicationArguments?) {
        //server.waitTillTermination()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}