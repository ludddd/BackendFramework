package com.ludd.fail

import com.google.protobuf.ByteString
import com.ludd.rpc.TestClient
import com.ludd.rpc.to.Message
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

private val logger = KotlinLogging.logger {}

@KtorExperimentalAPI
@Testcontainers
class ServiceFailTest {

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun echo() = runBlocking{
        val client = TestClient()

        client.sendRpc("fail", "echo", ByteString.copyFromUtf8("aaa"))
        val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
        assertEquals("aaa", response.result.toString(Charset.defaultCharset()))
    }

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun restartAfterCrash() = runBlocking{
        val client = TestClient()

        client.sendRpc("fail", "echo", ByteString.copyFromUtf8("aaa"))
        client.receive(Message.RpcResponse::parseDelimitedFrom)
        client.sendRpc("fail", "crash", ByteString.copyFromUtf8(""))
        client.sendRpc("fail", "echo", ByteString.copyFromUtf8("bbb"))
        val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
        assertEquals("bbb", response.result.toString(Charset.defaultCharset()))
    }
}