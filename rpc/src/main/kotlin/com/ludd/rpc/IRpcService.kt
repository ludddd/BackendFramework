package com.ludd.rpc

interface IRpcService {
    suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): ByteArray
}

interface IRpcServiceProvider {
    fun get(service: String): IRpcService
}