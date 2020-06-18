package com.ludd.echo_grpc

import com.ludd.echo.to.Echo
import com.ludd.echo.to.EchoServiceGrpc
import io.grpc.stub.StreamObserver

class EchoService : EchoServiceGrpc.EchoServiceImplBase() {

    override fun echo(request: Echo.Message?, responseObserver: StreamObserver<Echo.Message>?) {
        responseObserver?.onNext(request)
        responseObserver?.onCompleted()
    }
}