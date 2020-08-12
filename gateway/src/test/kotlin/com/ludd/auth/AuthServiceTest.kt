package com.ludd.auth

import com.ludd.auth.to.Auth
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals

@SpringBootTest(properties = ["gateway.auth=enabled"])
class AuthServiceTest {

    @MockBean
    private lateinit var authRepository: IAuthRepository
    @Autowired
    private lateinit var authService: AuthService

    @Test
    fun addPlayer() = runBlocking {
        setMockUser("userA", false)
        val rez = authService.register(registerRequest("userA"))
        assertEquals(Auth.RegisterResponse.Code.Ok, rez.code)
        Mockito.verify(authRepository).addPlayer(Auth.IdType.DEVICE_ID.name, "userA")
    }

    private fun registerRequest(id: String) =
        Auth.RegisterRequest.newBuilder().setType(Auth.IdType.DEVICE_ID).setId(id).build()

    @Test
    fun addExistingPlayer() = runBlocking {
        setMockUser("userA", true)
        assertEquals(Auth.RegisterResponse.Code.AlreadyRegistered,
            authService.register(registerRequest("userA")).code)
    }

    private fun setMockUser(id: String, enabled: Boolean) {
        Mockito.`when`(authRepository.hasPlayer(Auth.IdType.DEVICE_ID.name, id)).thenReturn(enabled)
    }

    @Test
    fun signInOk() = runBlocking{
        setMockUser("userA", true)
        val rez = authService.signIn(signInRequest("userA"))
        assertEquals(Auth.SignInResponse.Code.Ok, rez.code)
    }

    private fun signInRequest(id: String): Auth.SignInRequest {
        return Auth.SignInRequest.newBuilder().setType(Auth.IdType.DEVICE_ID).setId(id).build()
    }

    @Test
    fun signInNoUser() = runBlocking{
        setMockUser("userA", false)
        val rez = authService.signIn(signInRequest("userA"))
        assertEquals(Auth.SignInResponse.Code.UserNotFound, rez.code)
    }
}