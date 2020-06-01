package com.ludd.gateway

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@KtorExperimentalAPI
@SpringBootTest(properties=["gateway.tcp_server.port=9000"])
internal class TcpServerTest {

    @Autowired
    private lateinit var tcpServer: TcpServer

    @Test
    fun connect() {
        runBlocking {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = tcpServer.getPort())
            val read = socket.openReadChannel()
            val write = socket.openWriteChannel(autoFlush = true)

            launch(Dispatchers.IO) {
                while (true) {
                    val line = read.readUTF8Line()
                    println("server: $line")
                }
            }

            val lines = arrayOf("aaa", "bbb", "ccc")

            for (line in lines) {
                println("client: $line")
                write.writeStringUtf8("$line\n")
            }
        }
    }
}