package com.ludd.test_utils

import com.ludd.rpc.SessionContext
import org.mockito.Mockito
import java.net.SocketAddress

//TODO: remove duplication with rpc module. Something with java-test-fixtures and testImplementation(textFixtures(project(::rpc)))
fun mockSessionContext() = SessionContext(Mockito.mock(SocketAddress::class.java))