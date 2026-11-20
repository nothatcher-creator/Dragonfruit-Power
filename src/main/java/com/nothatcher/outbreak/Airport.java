package com.nothatcher.outbreak;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * The Vehicle Forge airport is overrun by a fixed crowd of the dead, placed when the airport was built (tagged
 * "outbreak_horde"). Nothing else spawns there: clear it and it stays clear. The airport is recognised by the
 * marker blocks Vehicle Forge buries under it.
 */
public final class Airport {
    public static final String TAG = "outbreak_horde";
    private static final Identifier MARKER = new Identifier("vehicleforge", "airport_marker");
    private static final List<Entity> DOOMED = new ArrayList<>();

    private Airport() {}

    /** is this spot at the airport? (looks a short way down for the buried marker) */
    public static boolean at(ServerWorld w, BlockPos pos) {
        BlockPos.Mutable m = pos.mutableCopy();
        int bottom = Math.max(w.getBottomY(), pos.getY() - 48);
        for (int y = pos.getY(); y >= bottom; y--) {
            m.setY(y);
            if (Registries.BLOCK.getId(w.getBlockState(m).getBlock()).equals(MARKER)) return true;
        }
        return false;
    }

    /** called as entities are added to the world: just note them; the checks need loaded chunks, so they wait a tick */
    static void onLoad(Entity e, ServerWorld w) {
        if (!(e instanceof HostileEntity)) return;
        synchronized (DOOMED) {
            DOOMED.add(e);
        }
    }

    static void tick() {
        List<Entity> batch;
        synchronized (DOOMED) {
            if (DOOMED.isEmpty()) return;
            batch = new ArrayList<>(DOOMED);
            DOOMED.clear();
        }
        for (Entity e : batch) {
            if (!e.isAlive() || !(e.getWorld() instanceof ServerWorld w) || !(e instanceof HostileEntity mob)) continue;
            if (mob.getCommandTags().contains(TAG)) {
                if (mob instanceof ZombieEntity z && !z.getCommandTags().contains("outbreak_dressed")) {
                    ZombieSpawner.dress(z, w.random, w);
                    z.addCommandTag("outbreak_dressed");
                }
                continue;
            }
            if (mob.hasCustomName() || mob.isPersistent()) continue;
            BlockPos p = mob.getBlockPos();
            if (w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4) && at(w, p)) mob.discard();   // stray spawns don't belong here
        }
    }

    /** for the Listen action */
    static Text report(ServerWorld w, BlockPos pos) {
        int n = w.getEntitiesByClass(ZombieEntity.class, new Box(pos).expand(220, 60, 220), z -> z.isAlive() && z.getCommandTags().contains(TAG)).size();
        return n == 0 ? Text.translatable("msg.outbreak.airport_clear") : Text.translatable("msg.outbreak.airport_left", n);
    }
}
