package com.nothatcher.dragonfruit.power;

import com.nothatcher.dragonfruit.FruitType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saved with the world (data/dragonfruit_powers.dat). Powers survive death and relogs. */
public class PowerState extends PersistentState {
    private static final String KEY = "dragonfruit_powers";
    private final Map<UUID, FruitType> fruits = new HashMap<>();

    public static PowerState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(PowerState::fromNbt, PowerState::new, KEY);
    }

    public static PowerState fromNbt(NbtCompound nbt) {
        PowerState state = new PowerState();
        NbtCompound players = nbt.getCompound("players");
        for (String key : players.getKeys()) {
            try {
                FruitType t = FruitType.byId(players.getString(key));
                if (t != FruitType.NONE) state.fruits.put(UUID.fromString(key), t);
            } catch (IllegalArgumentException ignored) {
                // bad uuid in file, skip
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();
        fruits.forEach((uuid, type) -> players.putString(uuid.toString(), type.id));
        nbt.put("players", players);
        return nbt;
    }

    public FruitType getFruit(UUID uuid) {
        return fruits.getOrDefault(uuid, FruitType.NONE);
    }

    public void setFruit(UUID uuid, FruitType type) {
        if (type == FruitType.NONE) fruits.remove(uuid);
        else fruits.put(uuid, type);
        markDirty();
    }
}
