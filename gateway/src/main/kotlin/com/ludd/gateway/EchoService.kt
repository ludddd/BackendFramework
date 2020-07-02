package com.ludd.gateway

import com.google.protobuf.ByteString
import org.springframework.stereotype.Service

@Service
class EchoService: IRpcService {
    override suspend fun call(arg: ByteString): ByteString {
        return arg
    }
}