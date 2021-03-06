package com.ludd.gateway

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcService
import com.ludd.rpc.SessionContext
import org.springframework.stereotype.Service

@Service
class EchoService: IRpcService {
    override suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult {
        return CallResult(arg, null)
    }
}