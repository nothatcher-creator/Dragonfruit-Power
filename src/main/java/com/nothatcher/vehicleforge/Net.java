package com.nothatcher.vehicleforge;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public final class Net {
    /** C2S: byte action (0 = primary: weapon/horn, 1 = secondary: siren/horn) */
    public static final Identifier ACTION = VehicleForge.id("action");
    /** C2S: float impact speed (blocks/tick) when the driver's vehicle slams into something */
    public static final Identifier CRASH = VehicleForge.id("crash");
    /** C2S: block hit + hand. Isometric view: place the held block exactly where the cursor points, at any distance */
    public static final Identifier ISO_PLACE = VehicleForge.id("iso_place");
    public static final Identifier ELEVATOR = VehicleForge.id("elevator");
    /** how far away the isometric view may place blocks */
    private static final double ISO_PLACE_RANGE = 64;

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
        ServerPlayNetworking.registerGlobalReceiver(ELEVATOR, (server, player, handler, buf, responseSender) -> {
            boolean up = buf.readBoolean();
            server.execute(() -> com.nothatcher.vehicleforge.block.ElevatorBlock.ride(player, up));
        });
        ServerPlayNetworking.registerGlobalReceiver(ISO_PLACE, (server, player, handler, buf, responseSender) -> {
            net.minecraft.util.hit.BlockHitResult hit = buf.readBlockHitResult();
            net.minecraft.util.Hand hand = buf.readEnumConstant(net.minecraft.util.Hand.class);
            server.execute(() -> isoPlace(player, hit, hand));
        });
        ServerPlayNetworking.registerGlobalReceiver(CRASH, (server, player, handler, buf, responseSender) -> {
            float impact = buf.readFloat();
            server.execute(() -> {
                if (player.getVehicle() instanceof VehicleEntity v && v.getControllingPassenger() == player
                        && Float.isFinite(impact) && impact > 0) {
                    v.crash(impact);
                }
            });
        });
    }

    /** place a block from a distance, with the same checks the game makes (game mode, spawn protection, border) */
    private static void isoPlace(net.minecraft.server.network.ServerPlayerEntity p, net.minecraft.util.hit.BlockHitResult hit, net.minecraft.util.Hand hand) {
        net.minecraft.server.world.ServerWorld w = p.getServerWorld();
        net.minecraft.util.math.BlockPos pos = hit.getBlockPos(), into = pos.offset(hit.getSide());
        if (p.isSpectator() || !p.getAbilities().allowModifyWorld) return;
        if (p.getEyePos().squaredDistanceTo(hit.getPos()) > ISO_PLACE_RANGE * ISO_PLACE_RANGE) return;
        if (!w.isChunkLoaded(pos) || !w.canPlayerModifyAt(p, pos) || !w.canPlayerModifyAt(p, into)) return;
        net.minecraft.item.ItemStack stack = p.getStackInHand(hand);
        if (!(stack.getItem() instanceof net.minecraft.item.BlockItem)) return;
        net.minecraft.util.ActionResult r = stack.useOnBlock(new net.minecraft.item.ItemUsageContext(p, hand, hit));
        if (r.shouldSwingHand()) p.swingHand(hand, true);
    }
}
