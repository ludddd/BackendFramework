package com.ludd.echo_grpc

import com.ludd.echo.to.Echo
import com.ludd.echo.to.EchoServiceGrpc
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EchoIntegrationTest {

    @Test
    fun echo() {
        val port = EchoServer.DEFAULT_PORT

        val channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build()
        val echoService = EchoServiceGrpc.newBlockingStub(channel)

        val msg = Echo.Message.newBuilder().setData("aaa").build()
        val response = echoService.echo(msg)
        Assertions.assertEquals("aaa", response.data)
    }
}