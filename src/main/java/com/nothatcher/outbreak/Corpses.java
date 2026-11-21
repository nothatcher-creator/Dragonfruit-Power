package com.nothatcher.outbreak;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;

/** Which deaths leave a corpse, and the body whose drops are being collected right now. */
public final class Corpses {
    /** the zombie whose drop() is running: its dropped stacks go into its corpse instead of onto the ground */
    public static LivingEntity capturing;

    private Corpses() {}

    /** what LivingEntity gets from CorpseDropMixin: the stacks collected for its corpse */
    public interface Holder {
        java.util.List<net.minecraft.item.ItemStack> outbreak$loot();
    }

    public static boolean wants(LivingEntity e) {
        return e.getClass() == ZombieEntity.class && e.getWorld() instanceof ServerWorld w && w.getGameRules().getBoolean(Outbreak.CORPSES);
    }
}
