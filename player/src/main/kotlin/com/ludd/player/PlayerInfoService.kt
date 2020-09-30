package com.ludd.player

import com.ludd.player.to.Player
import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import com.ludd.rpc.SessionContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@RpcService(name = "player")
class PlayerInfoService {

    @Autowired
    private lateinit var repository: PlayerInfoRepository

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun setName(request: Player.SetNameRequest, sessionContext: SessionContext): Player.SetNameResponse {
        require(sessionContext.playerId != null) { "Player should be authorized" }
        repository.setName(sessionContext.playerId!!, request.name)
        return Player.SetNameResponse.newBuilder().setCode(Player.SetNameResponse.Code.Ok).build()
    }
}