package com.ludd.gateway

import com.google.protobuf.ByteString

interface IRpcService {
    fun call(arg: ByteString): ByteString
}

interface IRpcServiceProvider {
    fun get(service: String): IRpcService
}