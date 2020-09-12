package com.ludd.player

import com.ludd.player.to.Player
import com.ludd.rpc.to.Message
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Testcontainers
class PlayerIntegrationTest {

    @Test
    @Timeout(10, unit = TimeUnit.MINUTES)
    fun directConnect() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("localhost", port = 30000)
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()

        val arg = Player.SetNameRequest
            .newBuilder()
            .setName("aaa")
            .build()

        val message = Message.InnerRpcRequest
            .newBuilder()
            .setService("playerInfo")
            .setMethod("setName")
            .setArg(arg.toByteString())
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
        kotlin.test.assertEquals("Player should be authorized", response.error)
    }
}