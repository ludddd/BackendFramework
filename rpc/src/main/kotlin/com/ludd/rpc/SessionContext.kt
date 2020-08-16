package com.ludd.rpc

import java.net.SocketAddress

class SessionContext(val remoteAddress: SocketAddress) {

    var playerId: String? = null
        private set

    fun authenticate(playerId: String) {
        assert(this.playerId == null) {"session is already authenticated"}
        this.playerId = playerId
    }
}