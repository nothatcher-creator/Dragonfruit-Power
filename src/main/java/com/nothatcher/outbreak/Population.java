package com.nothatcher.outbreak;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

/**
 * The dead aren't infinite. The world is split into 128x128 areas; each starts with outbreakAreaPopulation zombies'
 * worth of "stock". Every zombie killed in an area uses some up, and the spawner stops refilling an area that's been
 * cleared. Stragglers wander back in slowly (about one every 30 seconds per area).
 */
public class Population extends PersistentState {
    private static final int REGROW_TICKS = 600;
    /** kills still counted against an area, as of a tick (a long: a float tick loses precision in old worlds) */
    private static final class Area {
        float kills;
        long tick;

        Area(float kills, long tick) {
            this.kills = kills;
            this.tick = tick;
        }
    }

    private final Map<Long, Area> killed = new HashMap<>();

    static long key(int x, int z) {
        return ((long) (x >> 7) << 32) ^ ((z >> 7) & 0xffffffffL);
    }

    public static Population of(ServerWorld w) {
        return w.getPersistentStateManager().getOrCreate(Population::read, Population::new, "outbreak_population");
    }

    private float kills(long k, long now) {
        Area v = killed.get(k);
        if (v == null) return 0;
        float left = Math.max(0, v.kills - (now - v.tick) / (float) REGROW_TICKS);
        if (left <= 0) {
            killed.remove(k);
            markDirty();
            return 0;
        }
        return left;
    }

    /** 0..1: how much of this area's population is left */
    public float remaining(ServerWorld w, BlockPos at) {
        int pop = Math.max(1, w.getGameRules().getInt(Outbreak.AREA_POP));
        return Math.max(0, 1f - kills(key(at.getX(), at.getZ()), w.getTime()) / pop);
    }

    public void onKill(ServerWorld w, BlockPos at) {
        long k = key(at.getX(), at.getZ());
        float now = kills(k, w.getTime());
        killed.put(k, new Area(now + 1, w.getTime()));
        markDirty();
    }

    public Text describe(ServerWorld w, BlockPos at) {
        float r = remaining(w, at);
        String key = r > 0.75f ? "crawling" : r > 0.45f ? "busy" : r > 0.15f ? "thinning" : "quiet";
        return Text.translatable("msg.outbreak.area_" + key);
    }

    static Population read(NbtCompound nbt) {
        Population p = new Population();
        NbtCompound areas = nbt.getCompound("Areas");
        for (String k : areas.getKeys()) {
            NbtCompound a = areas.getCompound(k);
            try {
                p.killed.put(Long.parseLong(k), new Area(a.getFloat("Kills"), a.getLong("Tick")));
            } catch (NumberFormatException ignored) {
            }
        }
        return p;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound areas = new NbtCompound();
        killed.forEach((k, v) -> {
            NbtCompound a = new NbtCompound();
            a.putFloat("Kills", v.kills);
            a.putLong("Tick", v.tick);
            areas.put(Long.toString(k), a);
        });
        nbt.put("Areas", areas);
        return nbt;
    }
}
