package com.nothatcher.vehicleforge.interior;

import com.nothatcher.vehicleforge.ModRegistry;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleForge;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * RV interiors live in a void dimension (vehicleforge:interiors). Each RV gets its own room, 64 blocks
 * apart along X. Sneak + right-click the RV to go in; use the door inside to come back out beside the RV,
 * wherever it has driven to.
 */
public final class InteriorManager {
    public static final RegistryKey<World> INTERIORS = RegistryKey.of(RegistryKeys.WORLD, VehicleForge.id("interiors"));
    private static final int SPACING = 64, BASE_Y = 64, WIDTH = 7, LENGTH = 14, HEIGHT = 4;

    private InteriorManager() {}

    public static void register() {
        // nothing to hook yet; rooms are built lazily
    }

    public static BlockPos origin(int id) {
        return new BlockPos(id * SPACING, BASE_Y, 0);
    }

    public static void track(VehicleEntity rv) {
        if (rv.interiorId < 0) return;
        MinecraftServer server = rv.getServer();
        if (server == null) return;
        InteriorState.get(server).track(rv.interiorId, rv.getWorld().getRegistryKey().getValue(), rv.getUuid(), rv.getPos());
    }

    public static void enter(ServerPlayerEntity p, VehicleEntity rv) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerWorld room = server.getWorld(INTERIORS);
        if (room == null) {
            p.sendMessage(Text.literal("Interior dimension missing (datapack not loaded)").formatted(Formatting.RED), true);
            return;
        }
        InteriorState st = InteriorState.get(server);
        if (rv.interiorId < 0) rv.interiorId = st.allocate();
        track(rv);
        InteriorState.Room r = st.room(rv.interiorId);
        BlockPos o = origin(rv.interiorId);
        if (!r.built) {
            build(room, o);
            r.built = true;
            st.markDirty();
        }
        p.stopRiding();
        p.teleport(room, o.getX() + 3.5, o.getY() + 1, o.getZ() + 1.5, 0f, 0f);
        room.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.PLAYERS, 1f, 1f);
        p.sendMessage(Text.translatable("msg.vehicleforge.rv_inside").formatted(Formatting.AQUA), true);
    }

    public static void exit(ServerPlayerEntity p, BlockPos door) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        int id = Math.floorDiv(door.getX() + SPACING / 2, SPACING);
        InteriorState.Room r = InteriorState.get(server).find(id);
        ServerWorld target = server.getOverworld();
        Vec3d pos = Vec3d.ofBottomCenter(target.getSpawnPos());
        float yaw = p.getYaw();
        if (r != null && r.dimension != null) {
            ServerWorld w = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, r.dimension));
            if (w != null) {
                target = w;
                pos = r.lastPos;
                Entity e = r.vehicle == null ? null : w.getEntity(r.vehicle);
                if (e instanceof VehicleEntity v) {
                    pos = v.toWorld(v.type.width / 2 + 1.2, 0.1, 0.5);
                    yaw = v.getYaw() + 90;
                    // stand on the ground next to the RV
                    BlockPos bp = BlockPos.ofFloored(pos);
                    for (int i = 0; i < 6 && w.getBlockState(bp).isSolidBlock(w, bp); i++) bp = bp.up();
                    pos = new Vec3d(pos.x, bp.getY(), pos.z);
                }
            }
        }
        p.teleport(target, pos.x, pos.y, pos.z, yaw, 0f);
        target.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.PLAYERS, 1f, 1f);
    }

    /** A cosy empty camper: wood floor, panelled walls, windows, lights, door. Build whatever you want inside. */
    private static void build(ServerWorld w, BlockPos o) {
        BlockState floor = Blocks.OAK_PLANKS.getDefaultState();
        BlockState wall = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState trim = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState();
        BlockState ceiling = Blocks.WHITE_CONCRETE.getDefaultState();
        BlockState glass = Blocks.GLASS_PANE.getDefaultState();
        for (int x = -1; x <= WIDTH; x++) {
            for (int z = -1; z <= LENGTH; z++) {
                for (int y = 0; y <= HEIGHT + 1; y++) {
                    BlockPos p = o.add(x, y, z);
                    boolean edgeX = x == -1 || x == WIDTH, edgeZ = z == -1 || z == LENGTH;
                    BlockState s;
                    if (y == 0) s = edgeX || edgeZ ? trim : floor;
                    else if (y == HEIGHT + 1) s = (x % 3 == 1 && z % 4 == 1 && !edgeX && !edgeZ) ? Blocks.GLOWSTONE.getDefaultState() : ceiling;
                    else if (edgeX || edgeZ) {
                        boolean window = (y == 2 || y == 3) && edgeX && !edgeZ && z % 4 != 0;
                        boolean backWindow = (y == 2 || y == 3) && z == LENGTH && x >= 2 && x <= 4;
                        s = window || backWindow ? glass : (y == 1 ? trim : wall);
                    } else s = Blocks.AIR.getDefaultState();
                    w.setBlockState(p, s);
                }
            }
        }
        w.setBlockState(o.add(3, 1, -1), ModRegistry.RV_DOOR.getDefaultState());
        w.setBlockState(o.add(3, 2, -1), ModRegistry.RV_DOOR.getDefaultState());
        // a little rug so it doesn't feel empty
        for (int x = 2; x <= 4; x++) for (int z = 4; z <= 9; z++) w.setBlockState(o.add(x, 1, z), Blocks.RED_CARPET.getDefaultState());
    }
}
