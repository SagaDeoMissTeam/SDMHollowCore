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

package ru.hollowhorizon.hc.common.objects.items

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.Item
import ru.hollowhorizon.hc.client.utils.HollowPack
import ru.hollowhorizon.hc.client.utils.rl
import ru.hollowhorizon.hc.common.registry.RegistryObject

class HollowArmor<T : ArmorItem>(
    armor: (ArmorMaterial, EquipmentSlot, Item.Properties) -> T,
    material: ArmorMaterial,
    properties: Item.Properties,
) {
    val helmet: () -> T = { armor(material, EquipmentSlot.HEAD, properties) }
    val chest: () -> T = { armor(material, EquipmentSlot.CHEST, properties) }
    val legs: () -> T = { armor(material, EquipmentSlot.LEGS, properties) }
    val boots: () -> T = { armor(material, EquipmentSlot.FEET, properties) }

    fun registerItems(name: String): RegistryObject<T> {
        Registry.register(BuiltInRegistries.ITEM, (name + "_boots").rl, boots())
        Registry.register(BuiltInRegistries.ITEM, (name + "_legs").rl, legs())
        Registry.register(BuiltInRegistries.ITEM, (name + "_chest").rl, chest())
        return RegistryObject { Registry.register(BuiltInRegistries.ITEM, (name + "_helmet").rl, helmet()) }
    }

    fun registerModels(modid: String, name: String) {
        HollowPack.genItemModels.add("$modid:${name + "_helmet"}".rl)
        HollowPack.genItemModels.add("$modid:${name + "_chest"}".rl)
        HollowPack.genItemModels.add("$modid:${name + "_legs"}".rl)
        HollowPack.genItemModels.add("$modid:${name + "_boots"}".rl)
    }

    companion object {
        fun isFullSet(entity: LivingEntity, armor: HollowArmor<*>): Boolean {
            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
            val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
            val feet = entity.getItemBySlot(EquipmentSlot.FEET)
            return head.item === armor.helmet && chest.item === armor.chest && legs.item === armor.legs && feet.item === armor.boots
        }

        fun damagePart(entity: LivingEntity, target: EquipmentSlot, damage: Int) {
            val armorItem = entity.getItemBySlot(target)
            //? if <1.21 {
            /*armorItem.hurtAndBreak(damage, entity) {}
            *///?} else {
            
            armorItem.hurtAndBreak(damage, entity, target)
            //?}
        }

        fun hasPart(entity: LivingEntity, armor: HollowArmor<*>, target: EquipmentSlot?): Boolean {
            val arm = entity.getItemBySlot(target!!)
            return arm.item === armor.helmet || arm.item === armor.chest || arm.item === armor.legs || arm.item === armor.boots
        }

        fun isContainsAnyPart(entity: LivingEntity, armor: HollowArmor<*>): Boolean {
            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            val chest = entity.getItemBySlot(EquipmentSlot.CHEST)
            val legs = entity.getItemBySlot(EquipmentSlot.LEGS)
            val feet = entity.getItemBySlot(EquipmentSlot.FEET)
            return head.item === armor.helmet || chest.item === armor.chest || legs.item === armor.legs || feet.item === armor.boots
        }
    }
}
