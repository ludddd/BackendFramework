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
    @RpcMethod
    suspend fun signIn(arg: Auth.SignInRequest): Auth.SignInResponse
    {
        val code = if (authRepository.hasPlayer(arg.type.name, arg.id)) {
            Auth.SignInResponse.Code.Ok
        } else {
            Auth.SignInResponse.Code.UserNotFound
        }
        return Auth.SignInResponse
            .newBuilder()
            .setCode(code)
            .build()
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun register(request: Auth.RegisterRequest): Auth.RegisterResponse {
        val code = if (authRepository.hasPlayer(request.type.name, request.id)) {
            Auth.RegisterResponse.Code.AlreadyRegistered
        } else {
            authRepository.addPlayer(request.type.name, request.id)
            Auth.RegisterResponse.Code.Ok
        }
        return Auth.RegisterResponse.newBuilder().setCode(code).build()
    }
}
