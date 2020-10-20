package com.ludd.rpc.session

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["rpc.socket=Pooled"])
internal class ConnectionProviderTest {
    @Autowired
    private lateinit var connectionProvider: ConnectionProvider

    @Test
    fun pooledProvider() {
        assertThat(connectionProvider.create("test", "localhost", 80).socketFactory(),
            Matchers.instanceOf(PooledSocketFactory::class.java))
    }
}