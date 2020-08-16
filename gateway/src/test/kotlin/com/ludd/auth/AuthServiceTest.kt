package com.ludd.auth

import com.ludd.auth.to.Auth
import com.ludd.test_utils.mockSessionContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest(properties = ["gateway.auth=enabled"])
class AuthServiceTest {

    @MockBean
    private lateinit var authRepository: IAuthRepository
    @Autowired
    private lateinit var authService: AuthService

    @Test
    fun addPlayer() = runBlocking {
        setMockUser("userA", false)
        val sessionContext = mockSessionContext()
        val rez = authService.register(registerRequest("userA"), sessionContext)
        assertEquals(Auth.RegisterResponse.Code.Ok, rez.code)
        Mockito.verify(authRepository).addPlayer(Auth.IdType.DEVICE_ID.name, "userA")
        assertEquals("UUID_userA", sessionContext.playerId)
    }

    private fun registerRequest(id: String) =
        Auth.RegisterRequest.newBuilder().setType(Auth.IdType.DEVICE_ID).setId(id).build()

    @Test
    fun addExistingPlayer() = runBlocking {
        setMockUser("userA", true)
        assertEquals(Auth.RegisterResponse.Code.AlreadyRegistered,
            authService.register(registerRequest("userA"), mockSessionContext()).code)
    }

    private fun setMockUser(id: String, enabled: Boolean) {
        Mockito.`when`(authRepository.findPlayer(Auth.IdType.DEVICE_ID.name, id)).thenReturn(
            if (enabled) "UUID_$id" else null
        )
        Mockito.`when`(authRepository.addPlayer(Auth.IdType.DEVICE_ID.name, id)).thenReturn("UUID_$id")
    }

    @Test
    fun signInOk() = runBlocking{
        setMockUser("userA", true)
        val sessionContext = mockSessionContext()
        val rez = authService.signIn(signInRequest("userA"), sessionContext)
        assertEquals(Auth.SignInResponse.Code.Ok, rez.code)
        assertEquals("UUID_userA", sessionContext.playerId)
    }

    private fun signInRequest(id: String): Auth.SignInRequest {
        return Auth.SignInRequest.newBuilder().setType(Auth.IdType.DEVICE_ID).setId(id).build()
    }

    @Test
    fun signInNoUser() = runBlocking{
        setMockUser("userA", false)
        val sessionContext = mockSessionContext()
        val rez = authService.signIn(signInRequest("userA"), sessionContext)
        assertEquals(Auth.SignInResponse.Code.UserNotFound, rez.code)
        assertNull(sessionContext.playerId)
    }
}