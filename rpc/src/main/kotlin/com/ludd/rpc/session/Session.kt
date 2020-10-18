package com.ludd.rpc.session

import com.ludd.rpc.CallResult
import com.ludd.rpc.SessionContext

interface Session {
    suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult
    val isClosed: Boolean
}