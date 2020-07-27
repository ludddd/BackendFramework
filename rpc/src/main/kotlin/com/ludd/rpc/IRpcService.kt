package com.ludd.rpc

import com.google.protobuf.ByteString

interface IRpcService {
    suspend fun call(method: String, arg: ByteString): ByteString
}

interface IRpcServiceProvider {
    fun get(service: String): IRpcService
}