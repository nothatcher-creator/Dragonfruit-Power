package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Project Zomboid-style skills that level up as you use them (0 to 10), plus each player's safehouse.
 * Aiming: tighter, harder-hitting shots. Melee: harder hits. Fitness: faster on foot. Carpentry, Mechanics,
 * First Aid and Scavenging: their timed actions go quicker. Your occupation gives you a head start.
 */
public final class Skills {
    public enum Skill { AIMING, MELEE, FITNESS, CARPENTRY, MECHANICS, FIRST_AID, SCAVENGING;
        public String id() {
            return name().toLowerCase();
        }
    }

    public static final int MAX = 10;
    public static final Identifier SYNC = Outbreak.id("skills_sync");

    /** total xp needed to reach a level */
    public static int xpFor(int level) {
        return 30 * level * (level + 1);
    }

    public static int levelOf(int xp) {
        int l = 0;
        while (l < MAX && xp >= xpFor(l + 1)) l++;
        return l;
    }

    static class State extends PersistentState {
        final Map<UUID, int[]> xp = new HashMap<>();
        final Map<UUID, BlockBox> safehouses = new HashMap<>();

        static State read(NbtCompound nbt) {
            State s = new State();
            NbtCompound x = nbt.getCompound("Xp");
            for (String k : x.getKeys()) {
                int[] a = x.getIntArray(k), full = new int[Skill.values().length];
                System.arraycopy(a, 0, full, 0, Math.min(a.length, full.length));
                s.xp.put(UUID.fromString(k), full);
            }
            NbtCompound h = nbt.getCompound("Safehouses");
            for (String k : h.getKeys()) {
                int[] b = h.getIntArray(k);
                if (b.length == 6) s.safehouses.put(UUID.fromString(k), new BlockBox(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
            return s;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            NbtCompound x = new NbtCompound();
            xp.forEach((k, v) -> x.putIntArray(k.toString(), v));
            nbt.put("Xp", x);
            NbtCompound h = new NbtCompound();
            safehouses.forEach((k, b) -> h.putIntArray(k.toString(), new int[]{b.getMinX(), b.getMinY(), b.getMinZ(), b.getMaxX(), b.getMaxY(), b.getMaxZ()}));
            nbt.put("Safehouses", h);
            return nbt;
        }
    }

    private Skills() {}

    static State state(MinecraftServer s) {
        return s.getOverworld().getPersistentStateManager().getOrCreate(State::read, State::new, "outbreak_skills");
    }

    private static int[] xp(ServerPlayerEntity p) {
        return state(p.getServer()).xp.computeIfAbsent(p.getUuid(), u -> new int[Skill.values().length]);
    }

    public static int level(PlayerEntity p, Skill s) {
        if (!(p instanceof ServerPlayerEntity sp) || sp.getServer() == null) return 0;
        return levelOf(xp(sp)[s.ordinal()]);
    }

    /** how much quicker a skilled timed action is: 6% a level, down to 40% of the time */
    public static float speed(PlayerEntity p, Skill s) {
        return Math.max(0.4f, 1f - 0.06f * level(p, s));
    }

    public static void add(ServerPlayerEntity p, Skill s, int amount) {
        if (p.getServer() == null || p.isCreative()) return;
        int[] a = xp(p);
        int before = levelOf(a[s.ordinal()]);
        a[s.ordinal()] += amount;
        state(p.getServer()).markDirty();
        int after = levelOf(a[s.ordinal()]);
        if (after > before) {
            p.sendMessage(Text.translatable("msg.outbreak.skill_up", Text.translatable("skill.outbreak." + s.id()), after).formatted(Formatting.GOLD), true);
            p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.4f, 1.4f);
            apply(p);
        }
        sync(p);
    }

    /** a new character's head start */
    static void startWith(ServerPlayerEntity p, String occupation) {
        int[] a = xp(p);
        java.util.Arrays.fill(a, 0);
        switch (occupation) {
            case "police_officer" -> a[Skill.AIMING.ordinal()] = xpFor(3);
            case "veteran" -> {
                a[Skill.AIMING.ordinal()] = xpFor(4);
                a[Skill.MELEE.ordinal()] = xpFor(2);
            }
            case "firefighter" -> {
                a[Skill.FITNESS.ordinal()] = xpFor(2);
                a[Skill.MELEE.ordinal()] = xpFor(1);
            }
            case "construction_worker" -> a[Skill.CARPENTRY.ordinal()] = xpFor(3);
            case "mechanic" -> a[Skill.MECHANICS.ordinal()] = xpFor(3);
            case "doctor" -> a[Skill.FIRST_AID.ordinal()] = xpFor(3);
            case "burglar" -> a[Skill.SCAVENGING.ordinal()] = xpFor(3);
            case "park_ranger" -> {
                a[Skill.FITNESS.ordinal()] = xpFor(1);
                a[Skill.SCAVENGING.ordinal()] = xpFor(2);
                a[Skill.AIMING.ordinal()] = xpFor(1);
            }
            case "lumberjack" -> {
                a[Skill.MELEE.ordinal()] = xpFor(2);
                a[Skill.CARPENTRY.ordinal()] = xpFor(1);
            }
            case "fitness_instructor" -> a[Skill.FITNESS.ordinal()] = xpFor(3);
            case "chef" -> {
                a[Skill.MELEE.ordinal()] = xpFor(1);
                a[Skill.SCAVENGING.ordinal()] = xpFor(1);
            }
            default -> { }
        }
        state(p.getServer()).markDirty();
        apply(p);
        sync(p);
    }

    private static final UUID SPEED = UUID.fromString("5e6c1d1a-6b9e-4c1a-9e1b-0b7c0a000011");
    private static final UUID DAMAGE = UUID.fromString("5e6c1d1a-6b9e-4c1a-9e1b-0b7c0a000012");

    /** Fitness and Melee live in attributes */
    static void apply(ServerPlayerEntity p) {
        mod(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, "outbreak fitness", 0.012 * level(p, Skill.FITNESS), EntityAttributeModifier.Operation.MULTIPLY_BASE);
        mod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, "outbreak melee", 0.2 * level(p, Skill.MELEE), EntityAttributeModifier.Operation.ADDITION);
    }

    private static void mod(ServerPlayerEntity p, net.minecraft.entity.attribute.EntityAttribute a, UUID id, String name, double v, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance i = p.getAttributeInstance(a);
        if (i == null) return;
        i.removeModifier(id);
        if (v != 0) i.addTemporaryModifier(new EntityAttributeModifier(id, name, v, op));
    }

    static void sync(ServerPlayerEntity p) {
        if (p.getServer() == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        int[] a = xp(p);
        buf.writeVarInt(a.length);
        for (int v : a) buf.writeVarInt(v);
        BlockBox b = state(p.getServer()).safehouses.get(p.getUuid());
        buf.writeBoolean(b != null);
        if (b != null) {
            buf.writeInt(b.getMinX()); buf.writeInt(b.getMinY()); buf.writeInt(b.getMinZ());
            buf.writeInt(b.getMaxX()); buf.writeInt(b.getMaxY()); buf.writeInt(b.getMaxZ());
        }
        ServerPlayNetworking.send(p, SYNC, buf);
    }

    // ------------------------------------------------------------------ fitness, and resting in a safehouse

    static void tick(ServerWorld w) {
        if (w.getTime() % 20 != 7) return;
        for (ServerPlayerEntity p : w.getPlayers()) {
            if (p.isSprinting() && !p.hasVehicle()) add(p, Skill.FITNESS, 1);
            if (w.getTime() % 100 == 7 && Safehouses.insideOwn(p) && p.getHealth() < p.getMaxHealth() && p.getHungerManager().getFoodLevel() >= 8)
                p.heal(1f);
        }
    }

    // ------------------------------------------------------------------ safehouses

    public static final class Safehouses {
        private Safehouses() {}

        public static boolean inside(ServerWorld w, BlockPos pos) {
            if (w.getRegistryKey() != net.minecraft.world.World.OVERWORLD) return false;
            for (BlockBox b : state(w.getServer()).safehouses.values()) if (b.contains(pos)) return true;
            return false;
        }

        public static boolean insideOwn(ServerPlayerEntity p) {
            BlockBox b = state(p.getServer()).safehouses.get(p.getUuid());
            return b != null && p.getWorld().getRegistryKey() == net.minecraft.world.World.OVERWORLD && b.contains(p.getBlockPos());
        }

        /** the building you're standing in: walk out from you until the walls, floors and ceiling */
        static BlockBox measure(ServerWorld w, BlockPos at) {
            int west = reach(w, at, -1, 0), east = reach(w, at, 1, 0), north = reach(w, at, 0, -1), south = reach(w, at, 0, 1);
            int minX = at.getX() - west, maxX = at.getX() + east, minZ = at.getZ() - north, maxZ = at.getZ() + south;
            int up = 0;
            while (up < 12 && !w.getBlockState(at.up(up + 1)).isOpaqueFullCube(w, at.up(up + 1))) up++;
            int floors = Math.max(1, up / 3 + 1);
            return new BlockBox(minX - 1, at.getY() - 2, minZ - 1, maxX + 1, at.getY() + Math.max(up + 2, floors * 4 + 2), maxZ + 1);
        }

        private static int reach(ServerWorld w, BlockPos at, int dx, int dz) {
            for (int i = 1; i <= 16; i++) {
                BlockPos p = at.add(dx * i, 0, dz * i), q = p.up();
                if (w.getBlockState(p).isOpaqueFullCube(w, p) && w.getBlockState(q).isOpaqueFullCube(w, q)) return i;
            }
            return 8;
        }

        static void claim(ServerPlayerEntity p) {
            ServerWorld w = p.getServerWorld();
            if (w.getRegistryKey() != net.minecraft.world.World.OVERWORLD || w.isSkyVisible(p.getBlockPos().up())) {
                p.sendMessage(Text.translatable("msg.outbreak.safehouse_outside"), true);
                return;
            }
            State st = state(p.getServer());
            BlockBox box = measure(w, p.getBlockPos());
            for (Map.Entry<UUID, BlockBox> e : st.safehouses.entrySet())
                if (!e.getKey().equals(p.getUuid()) && e.getValue().intersects(box)) {
                    p.sendMessage(Text.translatable("msg.outbreak.safehouse_taken"), true);
                    return;
                }
            st.safehouses.put(p.getUuid(), box);
            st.markDirty();
            // anything already inside is cleared out of the population's way? no: you still have to clear it yourself
            p.sendMessage(Text.translatable("msg.outbreak.safehouse_claimed").formatted(Formatting.GREEN), false);
            p.setSpawnPoint(w.getRegistryKey(), p.getBlockPos(), p.getYaw(), true, false);
            sync(p);
        }

        static void abandon(ServerPlayerEntity p) {
            State st = state(p.getServer());
            if (st.safehouses.remove(p.getUuid()) != null) {
                st.markDirty();
                p.sendMessage(Text.translatable("msg.outbreak.safehouse_abandoned"), true);
                sync(p);
            }
        }
    }
}
