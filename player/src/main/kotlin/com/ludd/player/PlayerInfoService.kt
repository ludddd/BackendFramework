package com.ludd.player

import com.ludd.player.to.Player
import com.ludd.rpc.RpcMethod
import com.ludd.rpc.RpcService
import com.ludd.rpc.SessionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@RpcService(name = "playerInfo")
class PlayerInfoService {

    @Autowired
    private lateinit var repository: PlayerInfoRepository

    @Suppress("RedundantSuspendModifier")
    @RpcMethod
    suspend fun setName(request: Player.SetNameRequest, sessionContext: SessionContext): Player.SetNameResponse {
        repository.setName(sessionContext.playerId, request.name)
        return Player.SetNameResponse.newBuilder().setCode(Player.SetNameResponse.Code.Ok).build()
    }
}