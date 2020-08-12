package com.ludd.gateway

import com.ludd.auth.IAuthRepository
import com.ludd.auth.to.Auth
import com.ludd.rpc.to.Message
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import kotlin.test.assertEquals

@KtorExperimentalAPI
@SpringBootTest(properties=["gateway.tcp_server.port=9000",
    "gateway.service_provider=proxy",
    "gateway.auth=enabled"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GatewayAuthTest {

    @Autowired
    private lateinit var tcpServer: GatewayServer
    @MockBean
    private lateinit var authRepository: IAuthRepository

    @Test
    fun callAuth() = runBlocking {
        val selectorManager = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
        val authRequest = Auth.SignInRequest.newBuilder().build()
        val request = Message.RpcRequest
            .newBuilder()
            .setService("auth")
            .setMethod("signIn")
            .setArg(authRequest.toByteString())
            .build()
        val write = socket.openWriteChannel(autoFlush = true)
        val read = socket.openReadChannel()
        withContext(Dispatchers.IO) {
            request.writeDelimitedTo(write.toOutputStream())
        }

        val response = withContext(Dispatchers.IO) {
            Message.RpcResponse.parseDelimitedFrom(read.toInputStream())
        }
        val authResponse = withContext(Dispatchers.IO) {
            Auth.SignInResponse.parseDelimitedFrom(response.result.newInput())
        }
        assertEquals(Auth.SignInResponse.Code.UserNotFound, authResponse.code)
    }
}