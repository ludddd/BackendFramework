package com.ludd.auth

import com.ludd.auth.to.Auth

class AuthService {

    suspend fun signIn(arg: Auth.SignInRequest): Auth.SignInResponse
    {
        return Auth.SignInResponse.newBuilder().build()
    }
}