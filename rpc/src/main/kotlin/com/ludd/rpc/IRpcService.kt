package com.ludd.rpc

data class CallResult(val result: ByteArray?, val error: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CallResult

        if (result != null) {
            if (other.result == null) return false
            if (!result.contentEquals(other.result)) return false
        } else if (other.result != null) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result1 = result?.contentHashCode() ?: 0
        result1 = 31 * result1 + (error?.hashCode() ?: 0)
        return result1
    }

    override fun toString(): String {
        return "CallResult(result=${result?.contentToString()}, error=$error)"
    }


}

interface IRpcService {
    suspend fun call(method: String, arg: ByteArray, sessionContext: SessionContext): CallResult
}

interface IRpcServiceProvider {
    fun get(service: String): IRpcService
}