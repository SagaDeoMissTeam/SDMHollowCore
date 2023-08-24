package ru.hollowhorizon.hc.client.gltf

import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.capabilities.ICapabilityProvider
import ru.hollowhorizon.hc.client.gltf.animations.manager.AnimatedEntityCapability
import ru.hollowhorizon.hc.client.gltf.animations.manager.IModelManager
import ru.hollowhorizon.hc.client.utils.rl
import ru.hollowhorizon.hc.common.capabilities.CapabilityInstance
import ru.hollowhorizon.hc.common.capabilities.CapabilityStorage

interface IAnimated {
    val model: ResourceLocation
        get() = (this as ICapabilityProvider)
            .getCapability(CapabilityStorage.getCapability(AnimatedEntityCapability::class.java))
            .orElseThrow { IllegalStateException("Model not found!") }
            .model.rl
    val manager: IModelManager

}