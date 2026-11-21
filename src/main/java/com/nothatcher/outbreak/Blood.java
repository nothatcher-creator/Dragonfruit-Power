package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Fights leave blood on the ground: splatters and sprays where zombies and people get hit, a pool under the dead. */
public final class Blood {
    private Blood() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (amount >= 1f && bleeds(entity, source) && entity.getWorld() instanceof ServerWorld w && w.random.nextFloat() < 0.35f) {
                BlockState s = w.random.nextFloat() < 0.3f ? OutbreakBlocks.BLOOD_SPRAY.getDefaultState() : OutbreakBlocks.BLOOD.getDefaultState();
                splash(w, entity.getBlockPos(), s, 1);
            }
            return true;
        });
    }

    private static boolean bleeds(LivingEntity e, DamageSource source) {
        if (!(e instanceof ZombieEntity || e instanceof PlayerEntity)) return false;
        return source.getAttacker() != null || source.isIn(DamageTypeTags.IS_PROJECTILE) || source.isIn(DamageTypeTags.IS_EXPLOSION);
    }

    /** put a stain on open ground at or near this spot (within `spread` blocks), if there's room */
    public static void splash(ServerWorld w, BlockPos at, BlockState stain, int spread) {
        for (int tries = 0; tries < 4; tries++) {
            BlockPos p = at.add(w.random.nextInt(spread * 2 + 1) - spread, 0, w.random.nextInt(spread * 2 + 1) - spread);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos q = p.up(dy);
                if (w.getBlockState(q).isAir() && w.getBlockState(q.down()).isSideSolidFullSquare(w, q.down(), Direction.UP)) {
                    w.setBlockState(q, stain, 3);
                    return;
                }
            }
        }
    }
}
