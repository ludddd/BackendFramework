package com.ludd.rpc.session

import com.ludd.rpc.conn.ConnectionPool
import com.ludd.rpc.conn.RpcSocket
import com.ludd.rpc.conn.RpcSocketFactory
import com.ludd.rpc.conn.SocketWrapperFactory
import io.ktor.network.selector.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers

class PooledSocketFactory(private val poolSize:Int) : RpcSocketFactory {
    @OptIn(KtorExperimentalAPI::class)
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val poolMap = mutableMapOf<Pair<String, Int>, ConnectionPool>()
    private val socketFactory = SocketWrapperFactory()

    override suspend fun connect(host: String, port: Int): RpcSocket {
        val url = host to port
        if (!poolMap.containsKey(url)) {
            poolMap[url] = ConnectionPool(host, port, poolSize, socketFactory)
        }
        return poolMap[url]!!.connect()
    }
}
