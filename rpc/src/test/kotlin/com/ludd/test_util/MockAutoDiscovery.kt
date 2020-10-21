package com.ludd.test_util

import com.ludd.rpc.CallResult
import com.ludd.rpc.IRpcAutoDiscovery
import com.ludd.rpc.SessionContext

open class MockAutoDiscovery(private val function: () -> CallResult) : IRpcAutoDiscovery {
    override suspend fun call(
        service: String,
        method: String,
        arg: ByteArray,
        sessionContext: SessionContext
    ): CallResult {
        return function()
    }
}