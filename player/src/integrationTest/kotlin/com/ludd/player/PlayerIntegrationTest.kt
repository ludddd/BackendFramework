package com.ludd.player

import com.ludd.auth.to.Auth
import com.ludd.player.to.Player
import com.ludd.rpc.to.Message
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
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
        val client = TestClient()

        val arg = Player.SetNameRequest
            .newBuilder()
            .setName("aaa")
            .build()
        client.sendRpc("player", "setName", arg)

        val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
        //TODO: replace with contains
        assertEquals("java.lang.IllegalArgumentException: Player should be authorized", response.error)
    }

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun setName() = runBlocking{
        val client = TestClient()

        registerPlayer(client)

        val arg = Player.SetNameRequest
            .newBuilder()
            .setName("aaa")
            .build()
        client.sendRpc("player", "setName", arg)
        val response = client.receiveRpc(Player.SetNameResponse::parseDelimitedFrom)
        assertEquals(Player.SetNameResponse.Code.Ok, response.code)
    }

    private suspend fun registerPlayer(client: TestClient) {
        val arg = Auth.RegisterRequest.newBuilder()
            .setType(Auth.IdType.DEVICE_ID)
            .setId("testPlayer")
            .build()
        client.sendRpc("auth", "register", arg)
        val response = client.receiveRpc(Auth.RegisterResponse::parseDelimitedFrom)
        assertEquals(Auth.RegisterResponse.Code.Ok, response.code)
    }
}

