package com.ludd.rpc.session

import com.ludd.rpc.conn.Channel
import com.ludd.rpc.conn.ChannelPool
import com.ludd.rpc.conn.ChannelProvider
import com.ludd.rpc.conn.SocketChannelProvider

class PooledSocketFactory(private val poolSize:Int) : ChannelProvider {
    private val poolMap = mutableMapOf<Pair<String, Int>, ChannelPool>()
    private val socketFactory = SocketChannelProvider()

    override suspend fun acquire(host: String, port: Int): Channel {
        val url = host to port
        if (!poolMap.containsKey(url)) {
            poolMap[url] = ChannelPool(host, port, poolSize, socketFactory)
        }
        return poolMap[url]!!.openChannel()
    }
}
