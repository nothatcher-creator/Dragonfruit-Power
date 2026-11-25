package com.nothatcher.vehicleforge.power;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple electrical grid. Generators, solar panels, wind turbines and charged batteries are sources; power poles
 * relay. Every node reports itself once a second. A spot is powered if a chain of nodes, each within {@link #RANGE}
 * blocks of the next, links it to a live source.
 */
public final class PowerGrid {
    public static final int RANGE = 16;
    /** without Outbreak the city's mains are on: city props just work. With Outbreak the power is out. */
    @Deprecated
    public static final boolean MAINS_DOWN = false;

    /** is the city's grid down? (with Outbreak it fails a week or two in: see {@link Mains}) */
    public static boolean mainsDown() {
        return !Mains.POWER;
    }

    /** source: feeds the grid. producer: makes power (generator, solar, wind) rather than storing it (battery) */
    private record Node(BlockPos pos, boolean source, boolean producer, long seen) {}

    private static final Map<RegistryKey<World>, Map<Long, Map<Long, Node>>> NODES = new HashMap<>();
    private static final Map<RegistryKey<World>, Map<Long, long[]>> CACHE = new HashMap<>();

    private PowerGrid() {}

    private static long cell(int x, int z) {
        return ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
    }

    public static void report(World w, BlockPos pos, boolean source, boolean producer) {
        if (w.isClient) return;
        NODES.computeIfAbsent(w.getRegistryKey(), k -> new HashMap<>())
                .computeIfAbsent(cell(pos.getX(), pos.getZ()), k -> new HashMap<>())
                .put(pos.asLong(), new Node(pos.toImmutable(), source, producer && source, w.getTime()));
    }

    public static void remove(World w, BlockPos pos) {
        if (w.isClient) return;
        Map<Long, Map<Long, Node>> m = NODES.get(w.getRegistryKey());
        if (m == null) return;
        Map<Long, Node> c = m.get(cell(pos.getX(), pos.getZ()));
        if (c != null) c.remove(pos.asLong());
    }

    private static List<Node> near(World w, BlockPos pos) {
        List<Node> out = new ArrayList<>();
        Map<Long, Map<Long, Node>> m = NODES.get(w.getRegistryKey());
        if (m == null) return out;
        long now = w.getTime();
        for (int cx = (pos.getX() - RANGE) >> 4; cx <= (pos.getX() + RANGE) >> 4; cx++) {
            for (int cz = (pos.getZ() - RANGE) >> 4; cz <= (pos.getZ() + RANGE) >> 4; cz++) {
                long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
                Map<Long, Node> c = m.get(key);
                if (c == null) continue;
                c.values().removeIf(n -> now - n.seen > 80);
                if (c.isEmpty()) {
                    m.remove(key);
                    continue;
                }
                for (Node n : c.values()) if (n.pos.getSquaredDistance(pos) <= RANGE * RANGE) out.add(n);
            }
        }
        return out;
    }

    /** is there a live source linked to this spot? (the node at {@code exclude}, if any, doesn't count as a source) */
    public static boolean isPowered(World w, BlockPos pos, BlockPos exclude) {
        return isPowered(w, pos, exclude, false);
    }

    /** is anything on this world's grid at all? (lets the thousands of unlit city lights skip the search) */
    private static boolean empty(World w) {
        Map<Long, Map<Long, Node>> m = NODES.get(w.getRegistryKey());
        return m == null || m.isEmpty();
    }

    /**
     * @param producersOnly only generators, solar panels and turbines count: batteries charge from these, so two
     *                      batteries side by side can't keep charging each other forever
     */
    public static boolean isPowered(World w, BlockPos pos, BlockPos exclude, boolean producersOnly) {
        if (empty(w)) return false;
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>(near(w, pos));
        int budget = 400;
        while (!queue.isEmpty() && budget-- > 0) {
            Node n = queue.poll();
            if (!seen.add(n.pos.asLong())) continue;
            boolean counts = producersOnly ? n.producer : n.source;
            if (counts && !n.pos.equals(exclude)) return true;
            if (!n.producer) queue.addAll(near(w, n.pos));             // poles and batteries relay
        }
        return false;
    }

    /** cached for a second per spot */
    public static boolean isPowered(World w, BlockPos pos) {
        if (empty(w)) return false;
        Map<Long, long[]> c = CACHE.computeIfAbsent(w.getRegistryKey(), k -> new HashMap<>());
        long[] v = c.get(pos.asLong());
        long now = w.getTime();
        if (v != null && now - v[0] < 20) return v[1] == 1;
        if (c.size() > 4096) c.clear();
        boolean p = isPowered(w, pos, null);
        c.put(pos.asLong(), new long[]{now, p ? 1 : 0});
        return p;
    }

    /** city appliances: work off the mains, or off your own generator once the mains are down */
    public static boolean cityPowered(World w, BlockPos pos) {
        return Mains.POWER || isPowered(w, pos) || w.isReceivingRedstonePower(pos);
    }

    public static void clear() {
        NODES.clear();
        CACHE.clear();
    }
}
