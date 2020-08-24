package com.ludd.rpc

@Target(AnnotationTarget.FUNCTION)
annotation class RpcMethod(val name: String = "null")
