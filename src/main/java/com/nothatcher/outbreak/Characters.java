package com.nothatcher.outbreak;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Each world remembers a character for each player: the first time you join a world, a Project Zomboid-style
 * creation screen picks where you start, your occupation, traits, name and look. Until you've made one you're
 * held safe where you stand. Your character stays with that world, through deaths and all.
 */
public final class Characters {
    public static final Identifier OPEN = Outbreak.id("char_open");
    public static final Identifier CREATE = Outbreak.id("char_create");
    public static final Identifier SYNC = Outbreak.id("char_sync");

    public record Sheet(String name, String occupation, Set<String> traits, int look) {
        NbtCompound write() {
            NbtCompound c = new NbtCompound();
            c.putString("Name", name);
            c.putString("Occupation", occupation);
            c.putInt("Look", look);
            NbtList l = new NbtList();
            for (String t : traits) l.add(NbtString.of(t));
            c.put("Traits", l);
            return c;
        }

        static Sheet read(NbtCompound c) {
            Set<String> t = new LinkedHashSet<>();
            NbtList l = c.getList("Traits", NbtElement.STRING_TYPE);
            for (int i = 0; i < l.size(); i++) t.add(l.getString(i));
            return new Sheet(c.getString("Name"), c.getString("Occupation"), t, c.getInt("Look"));
        }

        public boolean has(String trait) {
            return traits.contains(trait);
        }
    }

    static class State extends PersistentState {
        final Map<UUID, Sheet> sheets = new HashMap<>();
        /** joined for the first time and still choosing */
        final Set<UUID> pending = new HashSet<>();

        static State read(NbtCompound nbt) {
            State s = new State();
            NbtCompound c = nbt.getCompound("Sheets");
            for (String k : c.getKeys()) s.sheets.put(UUID.fromString(k), Sheet.read(c.getCompound(k)));
            NbtList p = nbt.getList("Pending", NbtElement.STRING_TYPE);
            for (int i = 0; i < p.size(); i++) s.pending.add(UUID.fromString(p.getString(i)));
            return s;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            NbtCompound c = new NbtCompound();
            sheets.forEach((k, v) -> c.put(k.toString(), v.write()));
            nbt.put("Sheets", c);
            NbtList p = new NbtList();
            for (UUID u : pending) p.add(NbtString.of(u.toString()));
            nbt.put("Pending", p);
            return nbt;
        }
    }

    private static final Map<UUID, Long> OPEN_AT = new HashMap<>();

    private Characters() {}

    static State state(MinecraftServer s) {
        return s.getOverworld().getPersistentStateManager().getOrCreate(State::read, State::new, "outbreak_characters");
    }

    public static Sheet get(PlayerEntity p) {
        MinecraftServer s = p.getServer();
        return s == null ? null : state(s).sheets.get(p.getUuid());
    }

    public static String occupation(PlayerEntity p) {
        Sheet sh = get(p);
        return sh == null ? "" : sh.occupation();
    }

    public static boolean has(PlayerEntity p, String trait) {
        Sheet sh = get(p);
        return sh != null && sh.has(trait);
    }

    /** bite infection: thick skin halves it, being prone to illness doubles it */
    public static float infectionMul(PlayerEntity p) {
        Sheet sh = get(p);
        if (sh == null) return 1f;
        return sh.has("thick_skinned") ? 0.5f : sh.has("prone_to_illness") ? 2f : 1f;
    }

    // ------------------------------------------------------------------ joining

    static void onJoin(ServerPlayerEntity p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        State st = state(server);
        sendAll(p);
        Skills.apply(p);
        Skills.sync(p);
        Sheet sh = st.sheets.get(p.getUuid());
        if (sh != null) {
            apply(p, sh);
            broadcast(server, p.getUuid(), sh);
            return;
        }
        boolean firstTime = p.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.LEAVE_GAME)) == 0;
        if (!firstTime && !st.pending.contains(p.getUuid())) return;      // they were here before characters existed
        st.pending.add(p.getUuid());
        st.markDirty();
        hold(p);
        OPEN_AT.put(p.getUuid(), p.getServerWorld().getTime() + 30);
    }

    private static void hold(ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 4, false, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 6, false, false, false));
    }

    static void tick(ServerWorld w) {
        if (w.getTime() % 20 != 11) return;
        State st = state(w.getServer());
        for (ServerPlayerEntity p : w.getPlayers()) {
            UUID id = p.getUuid();
            if (st.pending.contains(id)) {
                hold(p);
                Long at = OPEN_AT.get(id);
                if (at == null || w.getTime() >= at) {
                    OPEN_AT.put(id, w.getTime() + 20 * 30);                 // nudge again now and then if it didn't open
                    ServerPlayNetworking.send(p, OPEN, PacketByteBufs.create());
                }
                continue;
            }
            Sheet sh = st.sheets.get(id);
            if (sh == null || p.isDead()) continue;
            var hm = p.getHungerManager();
            if (sh.has("light_eater") && hm.getExhaustion() >= 0.1f) hm.addExhaustion(-0.1f);
            if (sh.has("hearty_appetite")) hm.addExhaustion(0.1f);
            boolean healer = sh.has("fast_healer") || sh.occupation().equals("doctor");
            if (healer && w.getTime() % 100 == 11 && p.getHealth() < p.getMaxHealth() && hm.getFoodLevel() >= 6) p.heal(1f);
        }
    }

    // ------------------------------------------------------------------ creating

    static void receive(MinecraftServer server, ServerPlayerEntity p, PacketByteBuf buf) {
        int spawn = buf.readVarInt();
        String occ = buf.readString(64);
        int n = Math.min(32, buf.readVarInt());
        Set<String> traits = new LinkedHashSet<>();
        for (int i = 0; i < n; i++) traits.add(buf.readString(64));
        String name = buf.readString(64);
        int look = buf.readVarInt();
        server.execute(() -> create(p, spawn, occ, traits, name, look));
    }

    private static void create(ServerPlayerEntity p, int spawn, String occ, Set<String> traits, String rawName, int look) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        State st = state(server);
        if (!st.pending.contains(p.getUuid())) return;
        if (!CharacterDefs.valid(occ, traits) || look < -1 || look >= CharacterDefs.LOOKS || spawn < 0 || spawn >= CharacterDefs.SPAWNS.size()) {
            ServerPlayNetworking.send(p, OPEN, PacketByteBufs.create());
            return;
        }
        String name = rawName.replaceAll("[^\\p{L}\\p{N} .'-]", "").trim();
        if (name.length() > 24) name = name.substring(0, 24);
        if (name.isEmpty()) name = p.getGameProfile().getName();
        Sheet sh = new Sheet(name, occ, traits, look);
        st.sheets.put(p.getUuid(), sh);
        st.pending.remove(p.getUuid());
        st.markDirty();
        OPEN_AT.remove(p.getUuid());
        p.removeStatusEffect(StatusEffects.RESISTANCE);
        p.removeStatusEffect(StatusEffects.SLOWNESS);

        ServerWorld w = server.getOverworld();
        BlockPos at = start(w, CharacterDefs.SPAWNS.get(spawn), w.random);
        p.teleport(w, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, w.random.nextFloat() * 360f, 0f);
        p.setSpawnPoint(w.getRegistryKey(), at, 0f, true, false);
        apply(p, sh);
        kit(p, occ);
        Skills.startWith(p, occ);
        p.setHealth(p.getMaxHealth());
        p.getHungerManager().setFoodLevel(20);
        w.playSound(null, at, SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 0.6f, 0.6f);
        p.sendMessage(Text.translatable("msg.outbreak.character_start", name).formatted(Formatting.GRAY), false);
        broadcast(server, p.getUuid(), sh);
    }

    /** where a new character wakes up: inside a building if we can find one, else on the street */
    private static BlockPos start(ServerWorld w, String where, Random r) {
        if (where.equals("random")) where = CharacterDefs.SPAWNS.get(r.nextInt(3));
        BlockPos spawn = w.getSpawnPos();
        if (where.equals("spawn")) return w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn);
        double lo, hi, cx = 40, cz = 40;
        switch (where) {
            case "downtown" -> { lo = 20; hi = 260; }
            case "suburbs" -> { lo = 880; hi = 1180; }
            default -> { lo = 1420; hi = 2100; }
        }
        boolean indoors = !where.equals("countryside");
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, d = lo + r.nextDouble() * (hi - lo);
            int x = (int) (cx + Math.cos(a) * d), z = (int) (cz + Math.sin(a) * d);
            w.getChunk(x >> 4, z >> 4);                                     // generate it
            boolean city = w.getBiome(new BlockPos(x, 64, z)).getKey().map(k -> k.getValue().getNamespace().equals("vehicleforge")).orElse(false);
            for (int k = 0; k < 24; k++) {
                int px = (x & ~15) + r.nextInt(16), pz = (z & ~15) + r.nextInt(16);
                BlockPos s = city ? ZombieSpawner.findSpot(w, px, 64, pz)
                        : w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(px, 0, pz));
                if (s == null || !w.getFluidState(s).isEmpty() || !w.getFluidState(s.down()).isEmpty()) continue;
                if (!indoors || !city || !w.isSkyVisible(s.up())) return s;
                if (fallback == null) fallback = s;
            }
        }
        return fallback != null ? fallback : w.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn);
    }

    // ------------------------------------------------------------------ what it gives you

    private static final UUID SPEED = UUID.fromString("5e6c1d1a-6b9e-4c1a-9e1b-0b7c0a000001");
    private static final UUID DAMAGE = UUID.fromString("5e6c1d1a-6b9e-4c1a-9e1b-0b7c0a000002");
    private static final UUID HEALTH = UUID.fromString("5e6c1d1a-6b9e-4c1a-9e1b-0b7c0a000003");
    private static final UUID LUCK = UUID.fromString("5e6c1d1a-6b9e-4c1a-9e1b-0b7c0a000004");

    /** traits and occupation perks that live in attributes; re-applied on every join and respawn */
    static void apply(ServerPlayerEntity p, Sheet sh) {
        double speed = (sh.has("fast") ? 0.08 : 0) - (sh.has("slow") ? 0.08 : 0) + (sh.occupation().equals("fitness_instructor") ? 0.1 : 0);
        double dmg = (sh.has("strong") ? 1.5 : 0) - (sh.has("weak") ? 1.0 : 0) + (sh.occupation().equals("lumberjack") ? 0.5 : 0);
        double hp = (sh.has("tough") ? 4 : 0) - (sh.has("frail") ? 4 : 0) + (sh.occupation().equals("firefighter") ? 2 : 0);
        double luck = (sh.has("lucky") ? 1 : 0) - (sh.has("unlucky") ? 1 : 0);
        mod(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, "outbreak speed", speed, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        mod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, "outbreak strength", dmg, EntityAttributeModifier.Operation.ADDITION);
        mod(p, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, "outbreak health", hp, EntityAttributeModifier.Operation.ADDITION);
        mod(p, EntityAttributes.GENERIC_LUCK, LUCK, "outbreak luck", luck, EntityAttributeModifier.Operation.ADDITION);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void mod(ServerPlayerEntity p, EntityAttribute a, UUID id, String name, double v, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance i = p.getAttributeInstance(a);
        if (i == null) return;
        i.removeModifier(id);
        if (v != 0) i.addTemporaryModifier(new EntityAttributeModifier(id, name, v, op));
    }

    private static void give(ServerPlayerEntity p, ItemStack s) {
        if (s.getItem() instanceof ArmorItem a && p.getEquippedStack(a.getSlotType()).isEmpty()) {
            p.equipStack(a.getSlotType(), s);
            return;
        }
        p.getInventory().offerOrDrop(s);
    }

    private static void give(ServerPlayerEntity p, Item item, int n) {
        give(p, new ItemStack(item, n));
    }

    static void kit(ServerPlayerEntity p, String occ) {
        give(p, OutbreakItems.BOTTLED_WATER, 1);
        give(p, OutbreakItems.CHIPS, 1);
        switch (occ) {
            case "police_officer" -> {
                give(p, OutbreakItems.PISTOL.loaded());
                give(p, OutbreakItems.AMMO_9MM, 12);
                give(p, OutbreakItems.POLICE_VEST, 1);
                give(p, OutbreakItems.POLICE_BATON, 1);
            }
            case "firefighter" -> {
                give(p, OutbreakItems.FIRE_AXE, 1);
                give(p, OutbreakItems.FIREFIGHTER_HELMET, 1);
                give(p, OutbreakItems.FIREFIGHTER_JACKET, 1);
            }
            case "construction_worker" -> {
                give(p, OutbreakItems.CLAW_HAMMER, 1);
                give(p, OutbreakItems.NAILS, 16);
                give(p, Items.OAK_PLANKS, 8);
                give(p, OutbreakItems.HARD_HAT, 1);
                give(p, OutbreakItems.HI_VIS_VEST, 1);
            }
            case "doctor" -> {
                give(p, OutbreakItems.FIRST_AID_KIT, 1);
                give(p, OutbreakItems.BANDAGE, 3);
                give(p, OutbreakItems.ANTIBIOTICS, 1);
                give(p, OutbreakItems.PAINKILLERS, 1);
            }
            case "mechanic" -> {
                give(p, OutbreakItems.PIPE_WRENCH, 1);
                give(p, OutbreakItems.CAR_BATTERY, 1);
                give(p, OutbreakItems.SCRAP_METAL, 4);
                give(p, OutbreakItems.DUCT_TAPE, 2);
            }
            case "burglar" -> {
                give(p, OutbreakItems.CROWBAR, 1);
                give(p, OutbreakItems.FLASHLIGHT, 1);
                give(p, OutbreakItems.BACKPACK, 1);
            }
            case "chef" -> {
                give(p, OutbreakItems.KITCHEN_KNIFE, 1);
                give(p, OutbreakItems.FRYING_PAN, 1);
                give(p, OutbreakItems.CANNED_SOUP, 2);
                give(p, OutbreakItems.CANNED_BEANS, 2);
            }
            case "park_ranger" -> {
                give(p, OutbreakItems.MACHETE, 1);
                give(p, OutbreakItems.FLASHLIGHT, 1);
                give(p, OutbreakItems.BACKPACK, 1);
                give(p, OutbreakItems.MRE, 1);
            }
            case "lumberjack" -> {
                give(p, OutbreakItems.FIRE_AXE, 1);
                give(p, Items.OAK_LOG, 8);
                give(p, OutbreakItems.WORK_BOOTS, 1);
            }
            case "fitness_instructor" -> {
                give(p, OutbreakItems.SNEAKERS, 1);
                give(p, OutbreakItems.ENERGY_BAR, 3);
                give(p, OutbreakItems.ENERGY_DRINK, 2);
            }
            case "veteran" -> {
                give(p, OutbreakItems.PISTOL.loaded());
                give(p, OutbreakItems.AMMO_9MM, 8);
                give(p, OutbreakItems.MILITARY_VEST, 1);
                give(p, OutbreakItems.MILITARY_HELMET, 1);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ telling clients

    private static void write(PacketByteBuf buf, UUID id, Sheet sh) {
        buf.writeUuid(id);
        buf.writeVarInt(sh.look());
        buf.writeString(sh.name());
        buf.writeString(sh.occupation());
        buf.writeVarInt(sh.traits().size());
        for (String t : sh.traits()) buf.writeString(t);
    }

    private static void sendAll(ServerPlayerEntity p) {
        State st = state(p.getServer());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(st.sheets.size());
        st.sheets.forEach((id, sh) -> write(buf, id, sh));
        ServerPlayNetworking.send(p, SYNC, buf);
    }

    private static void broadcast(MinecraftServer server, UUID id, Sheet sh) {
        for (ServerPlayerEntity q : server.getPlayerManager().getPlayerList()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(1);
            write(buf, id, sh);
            ServerPlayNetworking.send(q, SYNC, buf);
        }
    }

    /** the occupations whose skills speed up some timed actions */
    public static float actionSpeed(PlayerEntity p, Actions.Type t) {
        String occ = occupation(p);
        return switch (t) {
            case BARRICADE, BARRICADE_METAL, UNBARRICADE -> (occ.equals("construction_worker") ? 0.7f : 1f) * Skills.speed(p, Skills.Skill.CARPENTRY);
            case SALVAGE_VEHICLE, SALVAGE_PROP -> (occ.equals("mechanic") ? 0.7f : 1f) * Skills.speed(p, Skills.Skill.MECHANICS);
            case SEARCH_VEHICLE, LOOT_ALL -> (occ.equals("burglar") ? 0.75f : 1f) * Skills.speed(p, Skills.Skill.SCAVENGING);
            case USE_ITEM -> (occ.equals("doctor") ? 0.75f : 1f) * Skills.speed(p, Skills.Skill.FIRST_AID);
            default -> 1f;
        };
    }
}
