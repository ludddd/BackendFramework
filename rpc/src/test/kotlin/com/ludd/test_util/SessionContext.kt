package com.ludd.test_util

import com.ludd.rpc.SessionContext
import org.mockito.Mockito
import java.net.SocketAddress

fun mockSessionContext() = SessionContext(Mockito.mock(SocketAddress::class.java))