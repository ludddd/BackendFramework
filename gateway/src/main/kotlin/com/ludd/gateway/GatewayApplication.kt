package com.ludd.gateway

import io.ktor.util.KtorExperimentalAPI
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication(scanBasePackages = ["com.ludd", "com.ludd.rpc"])
class GatewayApplication

fun main(args: Array<String>) {
	runApplication<GatewayApplication>(*args)
}

@KtorExperimentalAPI
@ConditionalOnProperty(name = ["gateway_server.autostart"], havingValue = "true")
@Component
class ServerRunner: ApplicationRunner {
	@Autowired
	private lateinit var server: GatewayServer

	override fun run(args: ApplicationArguments?) {
		server.waitTillTermination()
	}
}
