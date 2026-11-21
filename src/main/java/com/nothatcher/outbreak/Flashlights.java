package com.nothatcher.outbreak;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Switched-on flashlights: every couple of ticks each one lights the spot it's pointed at (up to 26 blocks), by
 * putting a self-removing invisible light there, and drains a second of battery a second.
 */
public final class Flashlights {
    private static final double RANGE = 26;
    private static final Map<ServerWorld, Map<Long, Long>> EXPIRY = new WeakHashMap<>();
    private static final Map<UUID, BlockPos> LIT = new HashMap<>();

    private Flashlights() {}

    static long expiry(ServerWorld w, BlockPos pos) {
        Map<Long, Long> m = EXPIRY.get(w);
        Long t = m == null ? null : m.get(pos.asLong());
        return t == null ? 0 : t;
    }

    public static void tick(ServerWorld w) {
        if (w.getTime() % 2 != 0) return;
        Map<Long, Long> exp = EXPIRY.computeIfAbsent(w, k -> new HashMap<>());
        for (ServerPlayerEntity p : w.getPlayers()) {
            Hand hand = FlashlightItem.isOn(p.getMainHandStack()) ? Hand.MAIN_HAND : FlashlightItem.isOn(p.getOffHandStack()) ? Hand.OFF_HAND : null;
            if (hand == null || p.isSpectator()) {
                LIT.remove(p.getUuid());
                continue;
            }
            if (w.getTime() % 20 == 0 && !p.getAbilities().creativeMode) {
                var st = p.getStackInHand(hand);
                st.setDamage(Math.min(st.getMaxDamage() - 1, st.getDamage() + 1));
            }
            Vec3d eye = p.getEyePos(), dir = p.getRotationVec(1f);
            BlockHitResult hit = w.raycast(new RaycastContext(eye, eye.add(dir.multiply(RANGE)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (hit.getType() != HitResult.Type.BLOCK) {
                LIT.remove(p.getUuid());
                continue;
            }
            // just in front of whatever it lands on
            BlockPos spot = BlockPos.ofFloored(hit.getPos().subtract(dir.multiply(0.6)));
            BlockState here = w.getBlockState(spot);
            Block light = OutbreakBlocks.FLASHLIGHT_LIGHT;
            if (!here.isAir() && !here.isOf(light)) continue;
            exp.put(spot.asLong(), w.getTime() + 6);
            if (!here.isOf(light)) {
                w.setBlockState(spot, light.getDefaultState(), Block.NOTIFY_ALL);
                w.scheduleBlockTick(spot, light, 6);
            }
            LIT.put(p.getUuid(), spot);
        }
        // forget expired entries now and then
        if (w.getTime() % 200 == 0) exp.values().removeIf(t -> t < w.getTime() - 40);
    }
}
