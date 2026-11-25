package com.nothatcher.vehicleforge.traffic;

import com.nothatcher.vehicleforge.ModRegistry;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleForge;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the city's streets busy: a handful of traffic cars and buses on the roads around each player, spawned out
 * of sight on the lanes and taken away again when they're far off (traffic is never saved). With Outbreak the
 * streets are busy only on the first day: from day two the drivers panic and the cars are left where they stop.
 */
public final class TrafficManager {
    private static final Set<VehicleEntity> CARS = new HashSet<>();
    private static final boolean OUTBREAK = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("outbreak");
    private static final String[] POOL = {"sedan", "sedan", "sedan", "hatchback", "hatchback", "suv", "suv", "pickup_truck", "pickup_truck",
            "taxi", "taxi", "van", "muscle_car", "sports_car", "classic_cruiser", "police_car", "limousine", "ambulance"};
    private static final int[] COLORS = {0x2E4E8F, 0xB71C1C, 0xF5F5F5, 0x222226, 0x8C8F94, 0x3C6E3A, 0x6B1E2A, 0x1F6FB5, 0xD9C27A, 0x5A3A22,
            0xE0E0E0, 0x41454B, 0x0E5E6F, 0xF07818};
    private static final int PER_PLAYER = 9, RADIUS = 112, DESPAWN = 144;

    private TrafficManager() {}

    public static void tick(MinecraftServer server) {
        ServerWorld w = server.getOverworld();
        CARS.removeIf(v -> v.isRemoved() || !v.aiDriven());
        long day = w.getTimeOfDay() / 24000L;
        boolean collapse = OUTBREAK && day >= 1;
        if (w.getTime() % 20 != 0) return;
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerPlayerEntity p : w.getPlayers()) if (!p.isSpectator()) players.add(p);
        // retire traffic nobody's near, and cars whose driver gave up
        for (VehicleEntity v : new ArrayList<>(CARS)) {
            double near = Double.MAX_VALUE;
            for (ServerPlayerEntity p : players) near = Math.min(near, p.squaredDistanceTo(v));
            if (v.brain == null || near > DESPAWN * DESPAWN || (v.brain.dead && near > 24 * 24)) {
                v.discard();
                continue;
            }
            if (collapse && v.brain.panicTicks == 0 && v.getWorld().getRandom().nextInt(45) == 0) TrafficAI.panic(v);
        }
        if (collapse || !w.getGameRules().getBoolean(VehicleForge.TRAFFIC_RULE)) return;
        Random r = w.getRandom();
        for (ServerPlayerEntity p : players) {
            if (p.getY() < 40 || p.getY() > 140) continue;
            int around = 0;
            for (VehicleEntity v : CARS) if (v.squaredDistanceTo(p) < RADIUS * RADIUS) around++;
            int want = OUTBREAK ? PER_PLAYER / 2 + 2 : PER_PLAYER;
            for (int i = 0; i < 6 && around < want; i++) {
                if (trySpawn(w, p, r, players)) around++;
            }
        }
    }

    private static boolean trySpawn(ServerWorld w, ServerPlayerEntity p, Random r, List<ServerPlayerEntity> players) {
        double ang = r.nextDouble() * Math.PI * 2, dist = 44 + r.nextDouble() * 50;
        double cx = p.getX() + Math.cos(ang) * dist, cz = p.getZ() + Math.sin(ang) * dist;
        boolean ns = r.nextBoolean();
        Direction dir = ns ? (r.nextBoolean() ? Direction.SOUTH : Direction.NORTH) : (r.nextBoolean() ? Direction.EAST : Direction.WEST);
        boolean bus = r.nextInt(9) == 0;
        int lane = bus ? 0 : r.nextInt(2);
        // snap to the nearest road band across, keep the spot along
        double crossRaw = ns ? cx : cz;
        int band = (int) Math.round((crossRaw - 14) / TrafficAI.CELL) * TrafficAI.CELL;
        double laneC = TrafficAI.laneCoord(dir, lane, band + 14);
        double alongC = ns ? cz : cx;
        int v = Math.floorMod((int) Math.floor(alongC), TrafficAI.CELL);
        if (v < 36 || v > 72) return false;
        double x = ns ? laneC : alongC, z = ns ? alongC : laneC;
        BlockPos at = BlockPos.ofFloored(x, TrafficAI.GROUND, z);
        if (!w.isChunkLoaded(at) || !TrafficAI.isRoad(w.getBlockState(at))) return false;
        for (int y = 1; y <= 3; y++) if (!w.getBlockState(at.up(y)).getCollisionShape(w, at.up(y)).isEmpty()) return false;
        for (ServerPlayerEntity o : players) if (o.squaredDistanceTo(x, o.getY(), z) < 36 * 36) return false;
        if (!w.getEntitiesByClass(VehicleEntity.class, new Box(x - 9, at.getY(), z - 9, x + 9, at.getY() + 5, z + 9), e -> true).isEmpty()) return false;
        String id = bus ? "city_bus" : POOL[r.nextInt(POOL.length)];
        EntityType<VehicleEntity> type = ModRegistry.ENTITY_TYPES.get(id);
        if (type == null) return false;
        VehicleEntity car = type.create(w);
        if (car == null) return false;
        car.refreshPositionAndAngles(x, TrafficAI.BASE, z, TrafficAI.yawOf(dir), 0);
        boolean livery = id.equals("taxi") || id.equals("police_car") || id.equals("ambulance") || id.equals("city_bus") || id.equals("limousine");
        if (!livery) car.setPaint(COLORS[r.nextInt(COLORS.length)]);
        car.setFuel(0.4f + r.nextFloat() * 0.6f);
        float cruise = (bus ? 0.36f : 0.44f + r.nextFloat() * 0.1f) * (OUTBREAK ? 1.25f : 1f);   // people are fleeing on day one
        car.brain = new TrafficAI.Brain(dir, lane, bus, cruise);
        car.setAi(true);
        car.addCommandTag("vehicleforge_traffic");
        w.spawnEntity(car);
        CARS.add(car);
        return true;
    }

    public static void clear() {
        CARS.clear();
    }
}
