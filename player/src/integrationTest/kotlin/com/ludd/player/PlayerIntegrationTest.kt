package com.ludd.player

import com.ludd.auth.to.Auth
import com.ludd.player.to.Player
import com.ludd.rpc.to.Message
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Testcontainers
class PlayerIntegrationTest {

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun notAuthorizedPlayer() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("localhost", port = 30000)
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()

        val arg = Player.SetNameRequest
            .newBuilder()
            .setName("aaa")
            .build()

        val message = Message.RpcRequest
            .newBuilder()
            .setService("player")
            .setMethod("setName")
            .setArg(arg.toByteString())
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
        //TODO: replace with contains
        assertEquals("java.lang.IllegalArgumentException: Player should be authorized", response.error)
    }

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun setName() = runBlocking{
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("localhost", port = 30000)
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()

        registerPlayer(write, read)

        val arg = Player.SetNameRequest
            .newBuilder()
            .setName("aaa")
            .build()

        val message = Message.RpcRequest
            .newBuilder()
            .setService("player")
            .setMethod("setName")
            .setArg(arg.toByteString())
            .build()
        withContext(Dispatchers.IO) {
            message.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            val msg = Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
            Player.SetNameResponse.parseDelimitedFrom(msg.result.newInput())
        }
        assertEquals(Player.SetNameResponse.Code.Ok, response.code)
    }

    private suspend fun registerPlayer(write: ByteWriteChannel, read: ByteReadChannel) {
        val arg = Auth.RegisterRequest.newBuilder()
            .setType(Auth.IdType.DEVICE_ID)
            .setId("testPlayer")
            .build()
        val request = Message.RpcRequest
            .newBuilder()
            .setService("auth")
            .setMethod("register")
            .setArg(arg.toByteString())
            .build()
        withContext(Dispatchers.IO) {
            request.writeDelimitedTo(write.toOutputStream())
        }
        val response = withContext(Dispatchers.IO) {
            val msg = Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
            Auth.RegisterResponse.parseDelimitedFrom(msg.result.newInput())
        }
        assertEquals(Auth.RegisterResponse.Code.Ok, response.code)
    }
}