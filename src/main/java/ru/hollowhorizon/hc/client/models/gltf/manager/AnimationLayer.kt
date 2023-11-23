package ru.hollowhorizon.hc.client.models.gltf.manager

import com.mojang.math.Quaternion
import kotlinx.serialization.Serializable
import net.minecraft.Util
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import ru.hollowhorizon.hc.HollowCore
import ru.hollowhorizon.hc.client.models.gltf.GltfTree
import ru.hollowhorizon.hc.client.models.gltf.Transformation
import ru.hollowhorizon.hc.client.models.gltf.animations.Animation
import ru.hollowhorizon.hc.client.models.gltf.animations.AnimationType
import ru.hollowhorizon.hc.client.models.gltf.animations.PlayType
import kotlin.properties.Delegates

@Serializable
data class AnimationLayer(
    val animation: String,
    val priority: Float,
    val type: PlayType,
    val speed: Float,
    var time: Int,
    var fadeIn: Int = 10,
    var fadeOut: Int = 10,
) {
    var markToRemove by Delegates.observable(false) { _, _, new ->
        if(new) time = 0
    }

    fun isEnd(
        nameToAnimationMap: Map<String, Animation>,
        currentTick: Int,
        partialTick: Float,
    ): Boolean {
        val animation = nameToAnimationMap[animation] ?: return true
        val rawTime = (currentTick - time + partialTick) / 20f
        if(rawTime >= animation.maxTime) markToRemove = true
        return rawTime >= animation.maxTime + fadeOut
    }

    fun computeTransform(
        node: GltfTree.Node,
        bindPose: Animation,
        nameToAnimationMap: Map<String, Animation>,
        currentTick: Int,
        partialTick: Float,
    ): Transformation? {
        val animation = nameToAnimationMap[animation] ?: return null

        if (time == 0) time = currentTick
        var rawTime = (currentTick - time + partialTick) / 20f * speed

        if(fadeIn > 0 && rawTime < fadeIn) {
            return Transformation.lerp(
                bindPose.compute(node, 0f)!!,
                animation.compute(node, rawTime) ?: return null,
                rawTime / fadeIn
            )
        } else if (currentTick - time == fadeIn) time = 0

        if(markToRemove) {
            if(type == PlayType.ONCE) rawTime = animation.maxTime - rawTime

            return Transformation.lerp(
                animation.compute(node, rawTime % animation.maxTime) ?: return null,
                bindPose.compute(node, 0f)!!,
                rawTime / fadeOut
            )
        }

        val currentTime = when(type) {
            PlayType.LOOPED -> rawTime % animation.maxTime
            PlayType.LAST_FRAME -> rawTime.coerceAtMost(animation.maxTime)
            PlayType.REVERSED -> {
                val isReversed = (rawTime / animation.maxTime).toInt() % 2 == 1
                if (!isReversed) rawTime % animation.maxTime
                else (animation.maxTime - rawTime) % animation.maxTime
            }
            PlayType.ONCE -> rawTime
        }

        return animation.compute(node, currentTime)
    }
}

class DefinedLayer {
    var currentAnimation = AnimationType.IDLE
    var currentStartTime = 0
    var lastAnimation = AnimationType.IDLE
    var lastStartTime = 0
    var priority = 0f

    fun update(animationType: AnimationType, currentTick: Int, partialTick: Float) {
        priority = ((currentTick - currentStartTime + partialTick) / 10f).coerceAtMost(1f)
        if (animationType == currentAnimation) return
        lastAnimation = currentAnimation
        currentAnimation = animationType
        priority = 1f - priority
        lastStartTime = currentStartTime
        currentStartTime = currentTick
    }

    fun computeTransform(
        node: GltfTree.Node,
        bindPose: Animation,
        animationCache: Map<AnimationType, Animation>,
        currentTick: Int,
        partialTick: Float,
    ): Transformation? {
        val f = animationCache[currentAnimation] ?: animationCache[lastAnimation] ?: return null
        val s = animationCache[lastAnimation] ?: f
        val firstTime = (currentTick - lastStartTime + partialTick) / 20 % f.maxTime
        val secondTime = (currentTick - currentStartTime + partialTick) / 20 % s.maxTime

        return Transformation.lerp(
            f.compute(node, firstTime) ?: bindPose.compute(node, 0f)!!,
            s.compute(node, secondTime) ?: bindPose.compute(node, 0f)!!,
            1.0f - priority
        )
    }
}

class HeadLayer {
    fun computeRotation(
        animatable: LivingEntity,
        partialTick: Float,
    ): Quaternion {

        val bodyYaw = -Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot)
        val headYaw = -Mth.rotLerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot)
        val netHeadYaw = headYaw - bodyYaw
        val headPitch = -Mth.rotLerp(partialTick, animatable.xRotO, animatable.xRot)

        return Quaternion(headPitch, netHeadYaw, 0f, true)

    }
}