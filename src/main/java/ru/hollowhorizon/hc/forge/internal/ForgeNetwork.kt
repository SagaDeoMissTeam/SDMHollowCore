package ru.hollowhorizon.hc.forge.internal
//? if forge && >=1.21 {


import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.resources.ResourceLocation
import ru.hollowhorizon.hc.HollowCore
import ru.hollowhorizon.hc.HollowCore.MODID
import ru.hollowhorizon.hc.client.utils.nbt.NBTFormat
import ru.hollowhorizon.hc.client.utils.nbt.deserializeNoInline
import ru.hollowhorizon.hc.client.utils.nbt.serializeNoInline
import ru.hollowhorizon.hc.common.network.HollowPacketV2
import ru.hollowhorizon.hc.common.network.HollowPacketV3
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

fun <T : HollowPacketV3<T>> registerPacket(type: Class<T>) {
    val annotation = type.getAnnotation(HollowPacketV2::class.java)
    val location = CustomPacketPayload.Type<T>(ResourceLocation.fromNamespaceAndPath(MODID, type.name.lowercase()))

    val codec: StreamCodec<FriendlyByteBuf, T> = CustomPacketPayload.codec(
        { packet, buffer ->
            val tag = NBTFormat.serializeNoInline(packet, type)
            if (tag is CompoundTag) buffer.writeNbt(tag)
            else buffer.writeNbt(CompoundTag().apply { put("data", tag) })
        },
        { buffer ->
            try {
                val tag = buffer.readNbt() ?: throw IllegalStateException("NBT is null")
                if (tag.contains("data")) NBTFormat.deserializeNoInline(tag.get("%%data")!!, type)
                else NBTFormat.deserializeNoInline(tag, type)
            } catch (e: Exception) {
                // Без этого эта ошибка затеряется фиг пойми где, а так будет хоть какая-то информация
                HollowCore.LOGGER.error("Can't decode ${type.name} packet!", e)
                throw e
            }
        }
    )

    when (annotation.toTarget) {
        HollowPacketV2.Direction.TO_CLIENT -> {
            ForgeNetworkHelper.hollowCoreChannel
                .messageBuilder(type)
                .direction(PacketFlow.CLIENTBOUND)
                .encoder { packet, buffer -> codec.encode(buffer, packet) }
                .decoder(codec::decode)
                .consumerMainThread { t, u ->
                    t.handle(Minecraft.getInstance().player ?: throw IllegalStateException("Sender of packet is null!"))
                }
                .add()
        }

        HollowPacketV2.Direction.TO_SERVER -> {
            ForgeNetworkHelper.hollowCoreChannel
                .messageBuilder(type)
                .direction(PacketFlow.SERVERBOUND)
                .encoder { packet, buffer -> codec.encode(buffer, packet) }
                .decoder(codec::decode)
                .consumerMainThread { t, u ->
                    t.handle(u.sender ?: throw IllegalStateException("Sender of packet is null!"))
                }
                .add()
        }

        HollowPacketV2.Direction.ANY -> {
            ForgeNetworkHelper.hollowCoreChannel
                .messageBuilder(type)
                .encoder { packet, buffer -> codec.encode(buffer, packet) }
                .decoder(codec::decode)
                .consumerMainThread { t, u ->
                    if (u.isClientSide) t.handle(
                        Minecraft.getInstance().player ?: throw IllegalStateException("Sender of packet is null!")
                    )
                    else t.handle(u.sender ?: throw IllegalStateException("Sender of packet is null!"))
                }
                .add()
        }
    }
}
//?} elif forge {
/*import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.PacketFlow
import net.minecraftforge.network.NetworkDirection
import ru.hollowhorizon.hc.HollowCore
import ru.hollowhorizon.hc.client.utils.nbt.NBTFormat
import ru.hollowhorizon.hc.client.utils.nbt.deserializeNoInline
import ru.hollowhorizon.hc.client.utils.nbt.serializeNoInline
import ru.hollowhorizon.hc.common.network.HollowPacketV2
import ru.hollowhorizon.hc.common.network.HollowPacketV3
import java.util.function.BiConsumer

var id = 0

fun <T : HollowPacketV3<T>> registerPacket(type: Class<T>) {
    val annotation = type.getAnnotation(HollowPacketV2::class.java)

    val encoder: BiConsumer<T, FriendlyByteBuf> = BiConsumer { packet: T, buffer: FriendlyByteBuf ->
        val tag = NBTFormat.serializeNoInline(packet, type)
        if (tag is CompoundTag) buffer.writeNbt(tag)
        else buffer.writeNbt(CompoundTag().apply { put("data", tag) })
    }
    val decoder = { buffer: FriendlyByteBuf ->
        try {
            val tag = buffer.readNbt() ?: throw IllegalStateException("NBT is null")
            if (tag.contains("data")) NBTFormat.deserializeNoInline(tag.get("%%data")!!, type)
            else NBTFormat.deserializeNoInline(tag, type)
        } catch (e: Exception) {
            // Без этого эта ошибка затеряется фиг пойми где, а так будет хоть какая-то информация
            HollowCore.LOGGER.error("Can't decode ${type.name} packet!", e)
            throw e
        }
    }

    when (annotation.toTarget) {
        HollowPacketV2.Direction.TO_CLIENT -> {
            ForgeNetworkHelper.hollowCoreChannel
                .messageBuilder(type, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread { t, u ->
                    t.handle(Minecraft.getInstance().player ?: throw IllegalStateException("Sender of packet is null!"))
                }
                .add()
        }

        HollowPacketV2.Direction.TO_SERVER -> {
            ForgeNetworkHelper.hollowCoreChannel
                .messageBuilder(type, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread { t, u ->
                    t.handle(u.get().sender ?: throw IllegalStateException("Sender of packet is null!"))
                }
                .add()
        }

        HollowPacketV2.Direction.ANY -> {
            ForgeNetworkHelper.hollowCoreChannel
                .messageBuilder(type, id++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread { t, u ->
                    if (u.get().direction == NetworkDirection.PLAY_TO_CLIENT) t.handle(
                        Minecraft.getInstance().player ?: throw IllegalStateException("Sender of packet is null!")
                    )
                    else t.handle(u.get().sender ?: throw IllegalStateException("Sender of packet is null!"))
                }
                .add()
        }
    }
}
//}*/