/*
 * MIT License
 *
 * Copyright (c) 2024 HollowHorizon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ru.hollowhorizon.hc.common.capabilities

import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import ru.hollowhorizon.hc.api.ICapabilityDispatcher
import ru.hollowhorizon.hc.common.capabilities.containers.HollowContainer
import ru.hollowhorizon.hc.common.network.sendAllInDimension
import ru.hollowhorizon.hc.common.network.sendTrackingEntity
import ru.hollowhorizon.hc.common.network.sendTrackingEntityAndSelf

@Suppress("API_STATUS_INTERNAL")
open class CapabilityInstance {
    val properties = ArrayList<CapabilityProperty<CapabilityInstance, *>>()
    var notUsedTags = CompoundTag()
    open val canOtherPlayersAccess: Boolean = true
    lateinit var provider: ICapabilityDispatcher //Будет инициализированно инжектом
    val containers = ArrayList<HollowContainer>()
    var isChanged = false
    fun <T> syncable(default: T) = CapabilityProperty<CapabilityInstance, T>(default).apply {
        properties += this
    }

    fun synchronize() {
        when (val target = provider) {
            is Entity -> {
                if (target.level().isClientSide) {
                    if (canAcceptFromClient(Minecraft.getInstance().player!!)) SSyncEntityCapabilityPacket(
                        target.id,
                        javaClass.name,
                        serializeNBT()
                    ).send()
                } else CSyncEntityCapabilityPacket(target.id, javaClass.name, serializeNBT()).sendTrackingEntityAndSelf(target)
            }

            is Level -> {
                if (target.isClientSide) {
                    if (canAcceptFromClient(Minecraft.getInstance().player!!)) SSyncLevelCapabilityPacket(
                        target.dimension().location().toString(),
                        javaClass.name, serializeNBT()
                    ).send()
                } else CSyncLevelCapabilityPacket(javaClass.name, serializeNBT()).sendAllInDimension(target)
            }
        }
    }

    fun serializeNBT() = notUsedTags.copy().apply {
        properties.forEach { it.serialize(this) }
    }

    fun deserializeNBT(nbt: Tag) {
        properties.forEach { if (it.deserialize(nbt as? CompoundTag ?: return)) nbt.remove(it.defaultName) }
        val tag = nbt as? CompoundTag ?: return
        notUsedTags.merge(tag)
    }

    open fun canAcceptFromClient(player: Player): Boolean {
        return false
    }

    inline fun <reified T : Any> syncableList(list: MutableList<T> = ArrayList()) =
        syncable(SyncableListImpl(list, T::class.java) { isChanged = true })

    inline fun <reified T : Any> syncableList(vararg elements: T) = syncableList(elements.toMutableList())

    inline fun <reified K : Any, reified V : Any> syncableMap() =
        syncable(SyncableMapImpl(HashMap(), K::class.java, V::class.java) { isChanged = true })
}