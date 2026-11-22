package com.nothatcher.outbreak;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;

import java.util.HashSet;
import java.util.Set;

/**
 * What a zombie did for a living. Cops turn up around police cars, doctors around ambulances, firefighters by fire
 * trucks, builders on construction sites, mechanics near tow trucks, pilots and soldiers at the airport, and a few
 * of each everywhere else. Each has its own uniform (skin), gear and pockets.
 */
public enum ZombieJob {
    CIVILIAN, COP, PILOT, DOCTOR, SOLDIER, FIREFIGHTER, BUILDER, MECHANIC, PRISONER;

    /** a zombie with a job: added to ZombieEntity by mixin, synced to clients and saved */
    public interface Holder {
        ZombieJob outbreak$job();

        void outbreak$setJob(ZombieJob job);
    }

    public String id() {
        return name().toLowerCase();
    }

    public static ZombieJob of(Entity e) {
        return e instanceof Holder h ? h.outbreak$job() : CIVILIAN;
    }

    public static ZombieJob byOrdinal(int i) {
        ZombieJob[] v = values();
        return i >= 0 && i < v.length ? v[i] : CIVILIAN;
    }

    private static final Set<String> CONSTRUCTION = Set.of("excavator", "bulldozer", "wheel_loader", "road_roller", "mobile_crane", "dump_truck", "cement_mixer");
    private static final Set<String> ARMY = Set.of("army_jeep", "tank", "rocket_artillery", "apc");

    /** pick a job for a zombie appearing here, from what's parked nearby */
    public static ZombieJob pick(ServerWorld w, double x, double y, double z, Random r) {
        Set<String> near = new HashSet<>();
        for (Entity e : w.getOtherEntities(null, new Box(x - 32, y - 12, z - 32, x + 32, y + 12, z + 32),
                e -> Registries.ENTITY_TYPE.getId(e.getType()).getNamespace().equals("vehicleforge"))) {
            near.add(Registries.ENTITY_TYPE.getId(e.getType()).getPath());
        }
        boolean airport = Airport.at(w, net.minecraft.util.math.BlockPos.ofFloored(x, y, z));
        int[] weight = new int[values().length];
        weight[CIVILIAN.ordinal()] = 82;
        weight[COP.ordinal()] = 3 + (near.contains("police_car") ? 30 : 0);
        weight[PRISONER.ordinal()] = 1 + (near.contains("police_car") ? 8 : 0);
        weight[DOCTOR.ordinal()] = 2 + (near.contains("ambulance") ? 35 : 0);
        weight[FIREFIGHTER.ordinal()] = 2 + (near.contains("fire_truck") ? 35 : 0);
        weight[MECHANIC.ordinal()] = 2 + (near.contains("tow_truck") ? 30 : 0);
        weight[BUILDER.ordinal()] = 4 + (near.stream().anyMatch(CONSTRUCTION::contains) ? 35 : 0);
        weight[SOLDIER.ordinal()] = 2 + (near.stream().anyMatch(ARMY::contains) ? 35 : 0) + (airport ? 10 : 0);
        weight[PILOT.ordinal()] = 1 + (airport ? 30 : 0) + (near.contains("airliner") || near.contains("propeller_plane") ? 20 : 0);
        int total = 0;
        for (int v : weight) total += v;
        int roll = r.nextInt(total);
        for (ZombieJob j : values()) {
            roll -= weight[j.ordinal()];
            if (roll < 0) return j;
        }
        return CIVILIAN;
    }
}
