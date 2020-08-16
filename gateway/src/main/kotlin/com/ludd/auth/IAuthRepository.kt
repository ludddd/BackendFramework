package com.ludd.auth

interface IAuthRepository {
    fun findPlayer(type: String, id: String): String?
    fun addPlayer(name: String, id: String): String
}