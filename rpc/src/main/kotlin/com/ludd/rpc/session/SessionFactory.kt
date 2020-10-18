package com.ludd.rpc.session

interface SessionFactory {
    suspend fun connect(serviceName: String,
                        host: String,
                        port: Int): Session
}