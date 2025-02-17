//? if fabric {
/*package ru.hollowhorizon.hc.fabric.internal;

import kotlin.Unit;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import ru.hollowhorizon.hc.client.utils.JavaHacks;
import ru.hollowhorizon.hc.common.network.HollowPacketV3Kt;

public class NetworkHelper {
    public static void register() {
        HollowPacketV3Kt.registerPacket = (type) -> {
            FabricNetworkKt.registerPacket(JavaHacks.forceCast(type));
            return Unit.INSTANCE;
        };
        HollowPacketV3Kt.sendPacketToClient = (player, hollowPacketV3) -> {
            player.connection.send(HollowPacketV3Kt.asVanillaPacket(hollowPacketV3, true));
            return Unit.INSTANCE;
        };
        HollowPacketV3Kt.sendPacketToServer = (hollowPacketV3) -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) connection.send(HollowPacketV3Kt.asVanillaPacket(hollowPacketV3, false));
            return Unit.INSTANCE;
        };
        HollowPacketV3Kt.registerPackets.invoke();
    }
}
*///?}