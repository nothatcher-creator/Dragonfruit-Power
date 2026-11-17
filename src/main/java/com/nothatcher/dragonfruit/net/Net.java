package com.nothatcher.dragonfruit.net;

import com.nothatcher.dragonfruit.DragonfruitMod;
import com.nothatcher.dragonfruit.power.PowerManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class Net {
    /** C2S: varint ability index */
    public static final Identifier USE_ABILITY = DragonfruitMod.id("use_ability");
    /** S2C: fruit ordinal, cooldowns remaining, buff remaining */
    public static final Identifier STATE = DragonfruitMod.id("state");
    /** S2C: one visual effect (see Vfx) */
    public static final Identifier VFX = DragonfruitMod.id("vfx");

    private Net() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(USE_ABILITY, (server, player, handler, buf, responseSender) -> {
            int index = buf.readVarInt();
            server.execute(() -> PowerManager.useAbility(player, index));
        });
    }

    public static void sendState(ServerPlayerEntity player, int fruitOrdinal, int[] cooldownRemaining, int buffRemaining) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(fruitOrdinal);
        buf.writeVarInt(cooldownRemaining.length);
        for (int c : cooldownRemaining) buf.writeVarInt(Math.max(0, c));
        buf.writeVarInt(Math.max(0, buffRemaining));
        ServerPlayNetworking.send(player, STATE, buf);
    }
}
