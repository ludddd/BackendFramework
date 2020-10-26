package com.ludd.fail

import com.google.protobuf.ByteString
import com.ludd.rpc.TestClient
import com.ludd.rpc.to.Message
import com.ludd.test_utils.KGenericContainer
import io.ktor.util.*
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull


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

    private val fail = KGenericContainer("ludd.fail:0.1")
        .withExposedPorts(9001)
        .withStartupTimeout(Duration.ofMinutes(5))

    @Test
    fun crash() = runBlocking {
        fail.start()
        val port = fail.firstMappedPort
        val client = TestClient("localhost", port)
        val req = Message.InnerRpcRequest.newBuilder()
            .setService("fail")
            .setMethod("crash")
            .build()
        client.send(req)
        fail.stop()
    }

    @Test
    @Timeout(1, unit = TimeUnit.MINUTES)
    fun restartAfterCrash() = runBlocking{
        val client = TestClient()
        client.sendRpc("fail", "echo", ByteString.copyFromUtf8("aaa"))
        client.receive(Message.RpcResponse::parseDelimitedFrom)
        client.sendRpc("fail",  "crash", ByteString.copyFromUtf8(""))
        val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
        if (response.hasError) {
            logger.info(response.error)
        }
        assertFalse(response.hasError)
        assertEquals("bbb", response.result.toString(Charset.defaultCharset()))
    }

    @Test
    fun hostName() = runBlocking {
        val client = TestClient()
        client.sendRpc("fail", "hostName", ByteString.copyFromUtf8(""))
        val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
        val hostName = response.result.toString(Charset.defaultCharset())
        assertThat(hostName, Matchers.startsWith("fail"))
    }

    @Test
    @Timeout(5, unit = TimeUnit.MINUTES)
    fun killPod() = runBlocking {
        val client = TestClient()

        for (i in 1..3) {
            client.sendRpc("fail", "hostName", ByteString.copyFromUtf8(""))
            val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
            assertNotNull(response.result)
            val hostName = response.result.toString(Charset.defaultCharset())

            val checkJob = checkJob(client, hostName)
            deletePod(hostName)

            checkJob.join()
        }
    }

    private fun CoroutineScope.checkJob(
        client: TestClient,
        hostName: String?
    ): Job {
        return launch {
            do {
                client.sendRpc("fail", "hostName", ByteString.copyFromUtf8(""))
                val response = client.receive(Message.RpcResponse::parseDelimitedFrom)
                if (response.hasError) logger.error(response.error)
                assertFalse(response.hasError)
                assertNotNull(response.result)
                val newHostName = response.result.toString(Charset.defaultCharset())
            } while (newHostName == hostName)
        }
    }

    private suspend fun deletePod(hostName: String) {
        withContext(Dispatchers.IO) {
            val k8Client: ApiClient = Config.defaultClient()
            Configuration.setDefaultApiClient(k8Client)
            val api = CoreV1Api()
            logger.info("Deleting pod $hostName")
            api.deleteNamespacedPod(hostName, "default", null, null, null, null, null, null)
            logger.info("Pod deleted")
        }
    }
}