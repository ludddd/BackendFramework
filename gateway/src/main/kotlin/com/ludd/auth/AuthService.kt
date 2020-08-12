package com.ludd.auth

import com.ludd.auth.to.Auth
import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@RpcService(name = "auth")
@Service
@ConditionalOnProperty(name = ["gateway.auth"], havingValue = "enabled")
class AuthService {
    @Autowired
    private lateinit var authRepository: IAuthRepository

    @Suppress("RedundantSuspendModifier")
    @RpcMethod(name = "signIn")
    suspend fun signIn(arg: Auth.SignInRequest): Auth.SignInResponse
    {
        return Auth.SignInResponse
            .newBuilder()
            .setCode(Auth.SignInResponse.Code.UserNotFound)
            .build()
    }

    fun register(request: Auth.RegisterRequest): Auth.RegisterResponse {
        if (authRepository.hasPlayer(request.type.name, request.id)) {
            return Auth.RegisterResponse.newBuilder().setCode(Auth.RegisterResponse.Code.AlreadyRegistered).build()
        }
        authRepository.addPlayer(request.type.name, request.id)
        return Auth.RegisterResponse.newBuilder().setCode(Auth.RegisterResponse.Code.Ok).build()
    }
}
