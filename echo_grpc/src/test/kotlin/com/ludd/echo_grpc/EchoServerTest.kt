package com.ludd.echo_grpc

import com.ludd.echo.to.Echo
import com.ludd.echo.to.EchoServiceGrpc
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EchoServerTest {

    @Test
    fun echo() {
        val port = 9000
        val server = EchoServer(port)
        server.start()

        val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
        val echoService = EchoServiceGrpc.newBlockingStub(channel)

        val msg = Echo.Message.newBuilder().setData("aaa").build()
        val response = echoService.echo(msg)
        assertEquals("aaa", response.data)
    }
}