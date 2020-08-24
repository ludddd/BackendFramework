package com.ludd.auth

import com.ludd.auth.to.Auth
import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import com.ludd.rpc.SessionContext
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
    suspend fun signIn(arg: Auth.SignInRequest, sessionContext: SessionContext): Auth.SignInResponse
    {
        val playerId = authRepository.findPlayer(arg.type.name, arg.id)
        val rez = Auth.SignInResponse.newBuilder()
        if (playerId == null) {
            Auth.SignInResponse.Code.UserNotFound
            rez.code = Auth.SignInResponse.Code.UserNotFound
        } else {
            rez.code = Auth.SignInResponse.Code.Ok
            sessionContext.authenticate(playerId)
        }
        return rez.build()
    }

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun register(request: Auth.RegisterRequest, sessionContext: SessionContext): Auth.RegisterResponse {
        val rez = Auth.RegisterResponse.newBuilder()
        val playerId = authRepository.findPlayer(request.type.name, request.id)
        if (playerId != null) {
            rez.code = Auth.RegisterResponse.Code.AlreadyRegistered
        } else {
            val newPlayerId = authRepository.addPlayer(request.type.name, request.id)
            rez.code = Auth.RegisterResponse.Code.Ok
            sessionContext.authenticate(newPlayerId)
        }
        return rez.build()
    }
}
