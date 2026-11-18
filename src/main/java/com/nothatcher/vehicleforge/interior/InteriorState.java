package com.nothatcher.vehicleforge.interior;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Which RV owns which interior room, and where that RV was last seen. Saved with the world. */
public class InteriorState extends PersistentState {
    public static final class Room {
        public Identifier dimension;
        public UUID vehicle;
        public Vec3d lastPos = Vec3d.ZERO;
        public boolean built;
    }

    private int nextId = 1;
    private final Map<Integer, Room> rooms = new HashMap<>();

    public static InteriorState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(InteriorState::fromNbt, InteriorState::new, "vehicleforge_interiors");
    }

    public static InteriorState fromNbt(NbtCompound nbt) {
        InteriorState s = new InteriorState();
        s.nextId = Math.max(1, nbt.getInt("NextId"));
        NbtCompound list = nbt.getCompound("Rooms");
        for (String key : list.getKeys()) {
            NbtCompound c = list.getCompound(key);
            Room r = new Room();
            r.dimension = new Identifier(c.getString("Dim"));
            if (c.containsUuid("Vehicle")) r.vehicle = c.getUuid("Vehicle");
            r.lastPos = new Vec3d(c.getDouble("X"), c.getDouble("Y"), c.getDouble("Z"));
            r.built = c.getBoolean("Built");
            try {
                s.rooms.put(Integer.parseInt(key), r);
            } catch (NumberFormatException ignored) {
            }
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("NextId", nextId);
        NbtCompound list = new NbtCompound();
        rooms.forEach((id, r) -> {
            NbtCompound c = new NbtCompound();
            c.putString("Dim", r.dimension == null ? "minecraft:overworld" : r.dimension.toString());
            if (r.vehicle != null) c.putUuid("Vehicle", r.vehicle);
            c.putDouble("X", r.lastPos.x);
            c.putDouble("Y", r.lastPos.y);
            c.putDouble("Z", r.lastPos.z);
            c.putBoolean("Built", r.built);
            list.put(Integer.toString(id), c);
        });
        nbt.put("Rooms", list);
        return nbt;
    }

    public int allocate() {
        int id = nextId++;
        rooms.put(id, new Room());
        markDirty();
        return id;
    }

    public Room room(int id) {
        return rooms.computeIfAbsent(id, k -> new Room());
    }

    public Room find(int id) {
        return rooms.get(id);
    }

    public void track(int id, Identifier dim, UUID vehicle, Vec3d pos) {
        Room r = room(id);
        r.dimension = dim;
        r.vehicle = vehicle;
        r.lastPos = pos;
        markDirty();
    }
}
