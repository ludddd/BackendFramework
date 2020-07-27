package com.ludd.gateway

import com.google.protobuf.ByteString
import com.ludd.rpc.IRpcService
import org.springframework.stereotype.Service

@Service
class EchoService: IRpcService {
    override suspend fun call(method: String, arg: ByteString): ByteString {
        return arg
    }
}