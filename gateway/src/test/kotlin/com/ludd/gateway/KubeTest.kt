package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
class KubeTest {

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun test() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket =  aSocket(selectorManager).tcp().connect("localhost", port = 30000)

        val response = GatewayServerTest.sendEchoMessage(socket, "aaa")
        kotlin.test.assertNotNull(response)
        kotlin.test.assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }
}