package com.ludd.gateway

import com.ludd.auth.IAuthRepository
import com.ludd.auth.to.Auth
import com.ludd.rpc.conn.SocketWrapperFactory
import com.ludd.rpc.to.Message
import io.ktor.util.*
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
        val socketFactory = SocketWrapperFactory()
        val socket = socketFactory.connect("127.0.0.1", tcpServer.getPort())
        val authRequest = Auth.SignInRequest.newBuilder().build()
        val request = Message.RpcRequest
            .newBuilder()
            .setService("auth")
            .setMethod("signIn")
            .setArg(authRequest.toByteString())
            .build()
        socket.write(request)

        val response = socket.read(Message.RpcResponse::parseDelimitedFrom)
        val authResponse = withContext(Dispatchers.IO) {
            Auth.SignInResponse.parseDelimitedFrom(response.result.newInput())
        }
        assertEquals(Auth.SignInResponse.Code.UserNotFound, authResponse.code)
    }
}