package com.ludd.rpc

import java.net.SocketAddress

class SessionContext(val remoteAddress: SocketAddress) {

    var playerId: String? = null
        private set

    fun authenticate(playerId: String) {
        assert(this.playerId == null) {"session is already authenticated"}
        this.playerId = playerId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionContext

        if (remoteAddress != other.remoteAddress) return false
        if (playerId != other.playerId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = remoteAddress.hashCode()
        result = 31 * result + (playerId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "SessionContext(remoteAddress=$remoteAddress, playerId=$playerId)"
    }


}