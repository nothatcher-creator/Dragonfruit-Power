package com.nothatcher.vehicleforge.power;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.nothatcher.vehicleforge.block.CityBlocks;
import com.nothatcher.vehicleforge.block.NeonSignBlock;
import com.nothatcher.vehicleforge.block.ToggleProp;
import com.nothatcher.vehicleforge.block.TrafficLightBlock;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * The city's mains: electricity and running water. On their own they never fail. With Outbreak, each world rolls
 * the day the water stops (day 5-12) and the day the power grid fails (day 7-14), and an emergency broadcast counts
 * down to it. When the power goes, every street lamp, traffic signal, neon sign, office light, TV and elevator in
 * the loaded city goes dark at once (and the rest as they load). Your own generators, solar panels and turbines
 * keep things running near them. /vfgrid shows and sets it.
 */
public class Mains extends PersistentState {
    /** current state, for code without a server at hand (world generation, block ticks) */
    public static volatile boolean POWER = true, WATER = true;
    private static final boolean OUTBREAK = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("outbreak");

    public boolean power = true, water = true, scheduled;
    public int powerOffDay = -1, waterOffDay = -1, broadcastDay = -1;

    private static final ArrayDeque<ChunkPos> SWEEP = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    public static Mains get(MinecraftServer s) {
        return s.getOverworld().getPersistentStateManager().getOrCreate(Mains::read, Mains::new, "vehicleforge_mains");
    }

    private static Mains read(NbtCompound n) {
        Mains m = new Mains();
        m.power = !n.contains("Power") || n.getBoolean("Power");
        m.water = !n.contains("Water") || n.getBoolean("Water");
        m.scheduled = n.getBoolean("Scheduled");
        m.powerOffDay = n.contains("PowerOffDay") ? n.getInt("PowerOffDay") : -1;
        m.waterOffDay = n.contains("WaterOffDay") ? n.getInt("WaterOffDay") : -1;
        m.broadcastDay = n.contains("BroadcastDay") ? n.getInt("BroadcastDay") : -1;
        return m;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound n) {
        n.putBoolean("Power", power);
        n.putBoolean("Water", water);
        n.putBoolean("Scheduled", scheduled);
        n.putInt("PowerOffDay", powerOffDay);
        n.putInt("WaterOffDay", waterOffDay);
        n.putInt("BroadcastDay", broadcastDay);
        return n;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            Mains m = get(s);
            if (!m.scheduled) {
                m.scheduled = true;
                if (OUTBREAK) {
                    var r = s.getOverworld().getRandom();
                    m.waterOffDay = 5 + r.nextInt(8);
                    m.powerOffDay = 7 + r.nextInt(8);
                }
                m.markDirty();
            }
            POWER = m.power;
            WATER = m.water;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            POWER = true;
            WATER = true;
            SWEEP.clear();
            QUEUED.clear();
        });
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) queue(chunk.getPos());
        });
        CommandRegistrationCallback.EVENT.register((d, reg, env) -> d.register(CommandManager.literal("vfgrid").requires(src -> src.hasPermissionLevel(2))
                .executes(c -> {
                    Mains m = get(c.getSource().getServer());
                    long day = c.getSource().getServer().getOverworld().getTimeOfDay() / 24000L;
                    c.getSource().sendFeedback(() -> Text.translatable("msg.vehicleforge.grid_status", day + 1,
                            Text.translatable(m.power ? "msg.vehicleforge.grid_on" : "msg.vehicleforge.grid_off"),
                            m.power && m.powerOffDay >= 0 ? Text.translatable("msg.vehicleforge.grid_until", m.powerOffDay + 1) : Text.empty(),
                            Text.translatable(m.water ? "msg.vehicleforge.grid_on" : "msg.vehicleforge.grid_off"),
                            m.water && m.waterOffDay >= 0 ? Text.translatable("msg.vehicleforge.grid_until", m.waterOffDay + 1) : Text.empty()), false);
                    return 1;
                })
                .then(CommandManager.literal("power")
                        .then(CommandManager.literal("on").executes(c -> { setPower(c.getSource().getServer(), true, true); return 1; }))
                        .then(CommandManager.literal("off").executes(c -> { setPower(c.getSource().getServer(), false, true); return 1; })))
                .then(CommandManager.literal("water")
                        .then(CommandManager.literal("on").executes(c -> { setWater(c.getSource().getServer(), true, true); return 1; }))
                        .then(CommandManager.literal("off").executes(c -> { setWater(c.getSource().getServer(), false, true); return 1; })))
                .then(CommandManager.literal("schedule")
                        .then(CommandManager.argument("powerDay", IntegerArgumentType.integer(0, 10000))
                                .then(CommandManager.argument("waterDay", IntegerArgumentType.integer(0, 10000)).executes(c -> {
                                    Mains m = get(c.getSource().getServer());
                                    m.powerOffDay = IntegerArgumentType.getInteger(c, "powerDay") - 1;
                                    m.waterOffDay = IntegerArgumentType.getInteger(c, "waterDay") - 1;
                                    m.markDirty();
                                    c.getSource().sendFeedback(() -> Text.translatable("msg.vehicleforge.grid_scheduled", m.powerOffDay + 1, m.waterOffDay + 1), true);
                                    return 1;
                                }))))));
    }

    public static void tick(MinecraftServer s) {
        ServerWorld w = s.getOverworld();
        sweepSome(w);
        if (w.getTime() % 20 != 0) return;
        Mains m = get(s);
        long day = w.getTimeOfDay() / 24000L;
        long tod = w.getTimeOfDay() % 24000L;
        if (m.water && m.waterOffDay >= 0 && (day > m.waterOffDay || (day == m.waterOffDay && tod >= 6000))) setWater(s, false, false);
        if (m.power && m.powerOffDay >= 0 && (day > m.powerOffDay || (day == m.powerOffDay && tod >= 13500))) setPower(s, false, false);   // just after dusk
        if (OUTBREAK && day > m.broadcastDay && tod >= 1000 && tod < 12000) {
            m.broadcastDay = (int) day;
            m.markDirty();
            broadcast(s, m, (int) day);
        }
    }

    public static void setPower(MinecraftServer s, boolean on, boolean manual) {
        Mains m = get(s);
        if (m.power == on) return;
        m.power = on;
        POWER = on;
        if (manual && !on) m.powerOffDay = -1;
        m.markDirty();
        ServerWorld w = s.getOverworld();
        for (ServerPlayerEntity p : w.getPlayers()) {
            p.sendMessage(Text.translatable(on ? "msg.vehicleforge.power_back" : "msg.vehicleforge.power_out").formatted(on ? Formatting.GREEN : Formatting.DARK_RED), false);
            w.playSound(null, p.getBlockPos(), on ? SoundEvents.BLOCK_BEACON_ACTIVATE : SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.AMBIENT, 1.2f, 0.5f);
        }
        queueLoaded(w);
    }

    public static void setWater(MinecraftServer s, boolean on, boolean manual) {
        Mains m = get(s);
        if (m.water == on) return;
        m.water = on;
        WATER = on;
        if (manual && !on) m.waterOffDay = -1;
        m.markDirty();
        for (ServerPlayerEntity p : s.getOverworld().getPlayers())
            p.sendMessage(Text.translatable(on ? "msg.vehicleforge.water_back" : "msg.vehicleforge.water_out").formatted(on ? Formatting.GREEN : Formatting.DARK_AQUA), false);
    }

    /** the morning emergency broadcast: what's happening, and a countdown to the outages */
    private static void broadcast(MinecraftServer s, Mains m, int day) {
        Text line;
        if (!m.power) {
            line = Text.translatable("broadcast.vehicleforge.static").formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
            if (day > m.powerOffDay + 1) return;                       // just the one burst of static, then silence
        } else {
            String key = "broadcast.vehicleforge.day" + Math.min(day, 5);
            Text main = Text.translatable(key);
            int toPower = m.powerOffDay - day, toWater = m.waterOffDay - day;
            Text warn = Text.empty();
            if (m.water && m.waterOffDay >= 0 && toWater <= 3) warn = Text.translatable(toWater <= 0 ? "broadcast.vehicleforge.water_today" : "broadcast.vehicleforge.water_soon", toWater);
            else if (!m.water) warn = Text.translatable("broadcast.vehicleforge.water_gone");
            if (m.powerOffDay >= 0 && toPower <= 3)
                warn = Text.translatable(toPower <= 0 ? "broadcast.vehicleforge.power_today" : "broadcast.vehicleforge.power_soon", toPower);
            line = Text.translatable("broadcast.vehicleforge.frame", day + 1, main, warn);
        }
        Text head = Text.translatable("broadcast.vehicleforge.title").formatted(Formatting.RED, Formatting.BOLD);
        for (ServerPlayerEntity p : s.getOverworld().getPlayers()) {
            p.sendMessage(Text.empty().append(head).append(" ").append(line.copy().formatted(Formatting.GOLD)), false);
            p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.AMBIENT, 0.6f, 0.7f);
        }
    }

    // ------------------------------------------------------------------ blackout sweep

    private static void queue(ChunkPos cp) {
        if (QUEUED.add(cp.toLong())) SWEEP.add(cp);
    }

    /** every chunk players could be seeing, and the spawn chunks */
    private static void queueLoaded(ServerWorld w) {
        int view = w.getServer().getPlayerManager().getViewDistance() + 1;
        for (ServerPlayerEntity p : w.getPlayers()) {
            ChunkPos c = p.getChunkPos();
            for (int dx = -view; dx <= view; dx++)
                for (int dz = -view; dz <= view; dz++)
                    if (w.isChunkLoaded(c.x + dx, c.z + dz)) queue(new ChunkPos(c.x + dx, c.z + dz));
        }
        ChunkPos sp = new ChunkPos(w.getSpawnPos());
        for (int dx = -11; dx <= 11; dx++)
            for (int dz = -11; dz <= 11; dz++)
                if (w.isChunkLoaded(sp.x + dx, sp.z + dz)) queue(new ChunkPos(sp.x + dx, sp.z + dz));
    }

    private static void sweepSome(ServerWorld w) {
        for (int n = 0; n < 3 && !SWEEP.isEmpty(); n++) {
            ChunkPos cp = SWEEP.poll();
            QUEUED.remove(cp.toLong());
            if (!w.isChunkLoaded(cp.x, cp.z)) continue;
            sweep(w, w.getChunk(cp.x, cp.z));
        }
    }

    /** switch every grid-powered light in the chunk to match the mains (or a nearby generator) */
    private static void sweep(ServerWorld w, WorldChunk chunk) {
        ChunkSection[] secs = chunk.getSectionArray();
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int i = 0; i < secs.length; i++) {
            ChunkSection sec = secs[i];
            if (sec == null || sec.isEmpty() || !sec.getBlockStateContainer().hasAny(st -> wrong(st, POWER))) continue;
            int by = chunk.sectionIndexToCoord(i) << 4;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState st = sec.getBlockState(x, y, z);
                        if (!wrong(st, POWER)) continue;
                        p.set(chunk.getPos().getStartX() + x, by + y, chunk.getPos().getStartZ() + z);
                        boolean on = PowerGrid.cityPowered(w, p);
                        BlockState to = switched(st, on);
                        if (to != null && to != st) w.setBlockState(p, to, Block.NOTIFY_LISTENERS);
                    }
        }
    }

    /** could this state be in the wrong state for mains being {@code on}? (cheap: no world access) */
    private static boolean wrong(BlockState st, boolean on) {
        Block b = st.getBlock();
        if (b == CityBlocks.STREET_LAMP || b == CityBlocks.NEON_SIGN || b == CityBlocks.ELECTRIC_LIGHT) return st.get(Properties.LIT) != on;
        if (b == CityBlocks.TRAFFIC_LIGHT) return st.get(TrafficLightBlock.POWERED) != on;
        if (!on && b instanceof ToggleProp) return st.get(Properties.LIT);
        if (!on && b == Blocks.REDSTONE_LAMP) return st.get(Properties.LIT);
        return false;
    }

    /** the state this block should have when it is (or isn't) powered, or null if it isn't a grid light */
    public static BlockState switched(BlockState st, boolean on) {
        Block b = st.getBlock();
        if (b == CityBlocks.STREET_LAMP || b == CityBlocks.NEON_SIGN || b == CityBlocks.ELECTRIC_LIGHT) return st.with(Properties.LIT, on);
        if (b == CityBlocks.TRAFFIC_LIGHT) return st.with(TrafficLightBlock.POWERED, on);
        if (b instanceof ToggleProp) return on ? st : st.with(Properties.LIT, false);
        if (b == Blocks.REDSTONE_LAMP) return on ? st : st.with(Properties.LIT, false);
        return null;
    }
}
