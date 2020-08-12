package com.ludd.auth

interface IAuthRepository {
    fun hasPlayer(type: String, id: String): Boolean
    fun addPlayer(name: String, id: String)
}