package com.ludd.auth

import com.ludd.auth.to.Auth
import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@RpcService(name = "auth")
@Service
@ConditionalOnProperty(name = ["gateway.auth"], havingValue = "enabled")
class AuthService {

    @RpcMethod(name = "signIn")
    suspend fun signIn(arg: ByteArray): ByteArray
    {
        val rez = Auth.SignInResponse
            .newBuilder()
            .setCode(Auth.SignInResponse.Code.UserNotFound)
            .build()
        val out = ByteArrayOutputStream()
        rez.writeDelimitedTo(out)
        return out.toByteArray()
    }
}