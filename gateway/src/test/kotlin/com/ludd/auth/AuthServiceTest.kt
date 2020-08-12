package com.ludd.auth

import com.ludd.auth.to.Auth
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
    fun addPlayer() {
        Mockito.`when`(authRepository.hasPlayer(Auth.IdType.DEVICE_ID.name, "userA")).thenReturn(false)
        val rez = authService.register(registerRequest("userA"))
        assertEquals(Auth.RegisterResponse.Code.Ok, rez.code)
        Mockito.verify(authRepository).addPlayer(Auth.IdType.DEVICE_ID.name, "userA")
    }

    private fun registerRequest(id: String) =
        Auth.RegisterRequest.newBuilder().setType(Auth.IdType.DEVICE_ID).setId(id).build()

    @Test
    fun addExistingPlayer() {
        Mockito.`when`(authRepository.hasPlayer(Auth.IdType.DEVICE_ID.name, "userA")).thenReturn(true)
        assertEquals(Auth.RegisterResponse.Code.AlreadyRegistered,
            authService.register(registerRequest("userA")).code)
    }
}