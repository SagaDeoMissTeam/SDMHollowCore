package ru.hollowhorizon.hc.common.network.packets

import kotlinx.serialization.Serializable
import net.minecraftforge.network.NetworkDirection
import ru.hollowhorizon.hc.client.gltf.IAnimated
import ru.hollowhorizon.hc.client.gltf.animations.PlayType
import ru.hollowhorizon.hc.common.network.HollowPacketV2
import ru.hollowhorizon.hc.common.network.Packet

@Serializable
data class StartAnimationContainer(
    val entityId: Int,
    val name: String,
    val priority: Float = 1.0f,
    val playType: PlayType = PlayType.ONCE,
    val speed: Float = 1.0f,
)

@Serializable
data class StopAnimationContainer(
    val entity: Int,
    val name: String,
    val priority: Float = 1.0f,
    val playType: PlayType = PlayType.ONCE,
    val speed: Float = 1.0f,
)

@HollowPacketV2(toTarget = NetworkDirection.PLAY_TO_CLIENT)
class StartAnimationPacket : Packet<StartAnimationContainer>({ player, container ->
    player.level.getEntity(container.entityId)?.let { entity ->
        if (entity is IAnimated) {
            entity.manager.startAnimation(container.name, container.priority, container.playType, container.speed)
        }
    }
})

@HollowPacketV2(toTarget = NetworkDirection.PLAY_TO_CLIENT)
class StopAnimationPacket : Packet<StopAnimationContainer>({ player, container ->
    player.level.getEntity(container.entity)?.let { entity ->
        if (entity is IAnimated) {
            entity.manager.stopAnimation(container.name)
        }
    }
})