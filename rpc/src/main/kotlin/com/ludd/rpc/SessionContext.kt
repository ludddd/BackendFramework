package com.ludd.rpc

import java.net.SocketAddress

class SessionContext(val remoteAddress: SocketAddress) {

    var playerId: String? = null
        private set

    fun authenticate(playerId: String) {
        this.playerId = playerId
    }
}