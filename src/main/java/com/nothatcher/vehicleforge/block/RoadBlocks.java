package com.nothatcher.vehicleforge.block;

import com.nothatcher.vehicleforge.ModRegistry;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleForge;
import com.nothatcher.vehicleforge.VehicleType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Road furniture that works. Fuel pumps hold a limited supply each (100-1000 litres): right-click one for a fuel
 * can (20 litres) until it runs dry. Railroad crossings flash, ring and drop their arm while a train is near.
 * Parking barriers lift for vehicles. Garage doors roll up and down (right-click or redstone), whole doors at once.
 */
public final class RoadBlocks {
    public static final List<Item> ITEMS = new ArrayList<>();
    public static Block FUEL_PUMP, CROSSING, BARRIER, GARAGE_DOOR;

    private RoadBlocks() {}

    public static void register() {
        FUEL_PUMP = reg("fuel_pump", new FuelPump(AbstractBlock.Settings.create().mapColor(MapColor.RED).strength(3f).sounds(BlockSoundGroup.METAL).nonOpaque()));
        CROSSING = reg("railroad_crossing", new Crossing(AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(2f).sounds(BlockSoundGroup.METAL).nonOpaque()
                .luminance(s -> s.get(Crossing.PHASE) > 0 ? 9 : 0)));
        BARRIER = reg("parking_barrier", new Barrier(AbstractBlock.Settings.create().mapColor(MapColor.YELLOW).strength(2f).sounds(BlockSoundGroup.METAL).nonOpaque()));
        GARAGE_DOOR = reg("roller_door", new GarageDoor(AbstractBlock.Settings.create().mapColor(MapColor.WHITE).strength(3f).sounds(BlockSoundGroup.METAL).nonOpaque()));
    }

    private static Block reg(String id, Block b) {
        Registry.register(Registries.BLOCK, VehicleForge.id(id), b);
        ITEMS.add(Registry.register(Registries.ITEM, VehicleForge.id(id), new BlockItem(b, new Item.Settings())));
        return b;
    }

    /** blocks that face the way the player was looking from */
    public abstract static class Facing extends HorizontalFacingBlock {
        Facing(Settings s) {
            super(s);
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }
    }

    // ------------------------------------------------------------------ fuel pump

    static class PumpState extends PersistentState {
        final Map<Long, Integer> left = new HashMap<>();

        static PumpState read(NbtCompound nbt) {
            PumpState s = new PumpState();
            long[] k = nbt.getLongArray("K");
            int[] v = nbt.getIntArray("V");
            for (int i = 0; i < Math.min(k.length, v.length); i++) s.left.put(k[i], v[i]);
            return s;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            long[] k = new long[left.size()];
            int[] v = new int[left.size()];
            int i = 0;
            for (Map.Entry<Long, Integer> e : left.entrySet()) {
                k[i] = e.getKey();
                v[i++] = e.getValue();
            }
            nbt.putLongArray("K", k);
            nbt.putIntArray("V", v);
            return nbt;
        }
    }

    public static class FuelPump extends Facing {
        private static final VoxelShape SHAPE = Block.createCuboidShape(3, 0, 3, 13, 16, 13);

        FuelPump(Settings s) {
            super(s);
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }

        @Override
        public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
            if (w.isClient) return ActionResult.SUCCESS;
            if (!com.nothatcher.vehicleforge.power.PowerGrid.cityPowered(w, pos)) {             // pumps need power: bring a generator
                p.sendMessage(Text.translatable("msg.vehicleforge.pump_no_power").formatted(Formatting.RED), true);
                return ActionResult.SUCCESS;
            }
            PumpState ps = ((ServerWorld) w).getPersistentStateManager().getOrCreate(PumpState::read, PumpState::new, "vehicleforge_pumps");
            int left = ps.left.computeIfAbsent(pos.asLong(), k -> 100 + (int) Math.floorMod(k * 0x9E3779B97F4A7C15L >>> 20, 900));
            if (left < 20) {
                p.sendMessage(Text.translatable("msg.vehicleforge.pump_dry").formatted(Formatting.RED), true);
                return ActionResult.SUCCESS;
            }
            ps.left.put(pos.asLong(), left - 20);
            ps.markDirty();
            p.getInventory().offerOrDrop(new ItemStack(ModRegistry.FUEL_CAN));
            w.playSound(null, pos, ModRegistry.sound("fuel_fill"), SoundCategory.BLOCKS, 0.8f, 1f);
            p.sendMessage(Text.translatable("msg.vehicleforge.pump_left", left - 20).formatted(Formatting.GOLD), true);
            return ActionResult.SUCCESS;
        }
    }

    // ------------------------------------------------------------------ railroad crossing

    public static class Crossing extends Facing {
        public static final IntProperty PHASE = IntProperty.of("phase", 0, 2);
        private static final VoxelShape SHAPE = Block.createCuboidShape(6, 0, 6, 10, 16, 10);

        Crossing(Settings s) {
            super(s);
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(PHASE, 0));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(FACING, PHASE);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }

        @Override
        public void onBlockAdded(BlockState st, World w, BlockPos pos, BlockState old, boolean moved) {
            if (!w.isClient) w.scheduleBlockTick(pos, this, 10);
        }

        @Override
        public void scheduledTick(BlockState st, ServerWorld w, BlockPos pos, Random r) {
            boolean train = !w.getEntitiesByClass(VehicleEntity.class, new Box(pos).expand(36, 8, 36), v -> v.type.category == VehicleType.Category.TRAIN).isEmpty();
            int phase = train ? (st.get(PHASE) == 1 ? 2 : 1) : 0;
            if (phase != st.get(PHASE)) w.setBlockState(pos, st.with(PHASE, phase));
            if (train) w.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.2f, phase == 1 ? 1.5f : 1.3f);
            w.scheduleBlockTick(pos, this, 10);
        }
    }

    // ------------------------------------------------------------------ parking barrier

    public static class Barrier extends Facing {
        public static final BooleanProperty OPEN = BooleanProperty.of("open");
        private static final VoxelShape SHAPE = Block.createCuboidShape(4, 0, 4, 12, 16, 12);

        Barrier(Settings s) {
            super(s);
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(FACING, OPEN);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }

        @Override
        public void onBlockAdded(BlockState st, World w, BlockPos pos, BlockState old, boolean moved) {
            if (!w.isClient) w.scheduleBlockTick(pos, this, 10);
        }

        @Override
        public void scheduledTick(BlockState st, ServerWorld w, BlockPos pos, Random r) {
            boolean car = !w.getEntitiesByClass(VehicleEntity.class, new Box(pos).expand(6, 3, 6), v -> true).isEmpty();
            if (car != st.get(OPEN)) {
                w.setBlockState(pos, st.with(OPEN, car));
                w.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.4f, car ? 1.4f : 1.1f);
            }
            w.scheduleBlockTick(pos, this, 10);
        }
    }

    // ------------------------------------------------------------------ garage door

    public static class GarageDoor extends Facing {
        public static final BooleanProperty OPEN = BooleanProperty.of("open");
        private static final VoxelShape[] SHAPES = {
                Block.createCuboidShape(0, 0, 13, 16, 16, 16), Block.createCuboidShape(0, 0, 0, 3, 16, 16),
                Block.createCuboidShape(0, 0, 0, 16, 16, 3), Block.createCuboidShape(13, 0, 0, 16, 16, 16)};
        private static final VoxelShape ROLLED = Block.createCuboidShape(0, 13, 0, 16, 16, 16);

        GarageDoor(Settings s) {
            super(s);
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(FACING, OPEN);
        }

        private static VoxelShape closed(BlockState st) {
            return SHAPES[st.get(FACING).getHorizontal()];
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return st.get(OPEN) ? ROLLED : closed(st);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return st.get(OPEN) ? VoxelShapes.empty() : closed(st);
        }

        @Override
        public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
            if (!w.isClient) set(w, pos, !st.get(OPEN));
            return ActionResult.success(w.isClient);
        }

        @Override
        public void neighborUpdate(BlockState st, World w, BlockPos pos, Block src, BlockPos from, boolean notify) {
            if (w.isClient) return;
            boolean power = w.isReceivingRedstonePower(pos);
            if (power && !st.get(OPEN)) set(w, pos, true);
        }

        /** open or close the whole door: every garage door panel joined to this one */
        static void set(World w, BlockPos start, boolean open) {
            List<BlockPos> todo = new ArrayList<>(List.of(start));
            java.util.Set<BlockPos> seen = new java.util.HashSet<>();
            while (!todo.isEmpty() && seen.size() < 48) {
                BlockPos p = todo.remove(todo.size() - 1);
                if (!seen.add(p) || !(w.getBlockState(p).getBlock() instanceof GarageDoor)) continue;
                w.setBlockState(p, w.getBlockState(p).with(OPEN, open));
                for (Direction d : Direction.values()) todo.add(p.offset(d));
            }
            w.playSound(null, start, open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1f, 0.7f);
        }
    }
}
