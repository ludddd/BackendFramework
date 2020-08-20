package com.ludd.auth

interface IAuthRepository {
    suspend fun findPlayer(type: String, id: String): String?
    suspend fun addPlayer(type: String, id: String): String
}