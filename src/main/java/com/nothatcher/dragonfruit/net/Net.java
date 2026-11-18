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
    /** S2C: list of (player uuid, fruit ordinal) so clients can draw everyone's fruit body glow */
    public static final Identifier FRUITS = DragonfruitMod.id("fruits");
    /** C2S: fire the ship's cannons (no payload) */
    public static final Identifier FIRE_CANNONS = DragonfruitMod.id("fire_cannons");
    /** C2S: toggle fruit form (no payload) */
    public static final Identifier TOGGLE_FORM = DragonfruitMod.id("toggle_form");
    /** S2C: list of (player uuid, in form?) */
    public static final Identifier FORMS = DragonfruitMod.id("forms");

    private Net() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(USE_ABILITY, (server, player, handler, buf, responseSender) -> {
            int index = buf.readVarInt();
            server.execute(() -> PowerManager.useAbility(player, index));
        });
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_FORM, (server, player, handler, buf, responseSender) ->
                server.execute(() -> PowerManager.toggleForm(player)));
        ServerPlayNetworking.registerGlobalReceiver(FIRE_CANNONS, (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    if (player.getVehicle() instanceof com.nothatcher.dragonfruit.content.ShipEntity ship) ship.fireCannons(player);
                }));
    }

    public static void sendForms(ServerPlayerEntity player, java.util.List<java.util.UUID> ids, java.util.List<Boolean> on) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeUuid(ids.get(i));
            buf.writeBoolean(on.get(i));
        }
        ServerPlayNetworking.send(player, FORMS, buf);
    }

    public static void sendFruits(ServerPlayerEntity player, java.util.List<java.util.UUID> ids, java.util.List<Integer> fruits) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            buf.writeUuid(ids.get(i));
            buf.writeVarInt(fruits.get(i));
        }
        ServerPlayNetworking.send(player, FRUITS, buf);
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
