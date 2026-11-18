package com.nothatcher.vehicleforge;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class Net {
    /** C2S: byte action (0 = primary: weapon/horn, 1 = secondary: siren/horn) */
    public static final Identifier ACTION = VehicleForge.id("action");

    private Net() {}

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, responseSender) -> {
            int action = buf.readByte();
            server.execute(() -> {
                if (player.getVehicle() instanceof VehicleEntity v && v.getControllingPassenger() == player) {
                    Weapons.action(v, player, action);
                }
            });
        });
    }
}
