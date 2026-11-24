package com.nothatcher.outbreak;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Booby traps and set dressing for safehouses and the ruined city. */
public final class Traps {
    private Traps() {}

    /** a decoration: a shape, and optionally which way it faces */
    public static class Decor extends HorizontalFacingBlock {
        private final VoxelShape ns, ew;

        public Decor(Settings s, VoxelShape northSouth, VoxelShape eastWest) {
            super(s);
            this.ns = northSouth;
            this.ew = eastWest;
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return st.get(FACING).getAxis() == Direction.Axis.Z ? ns : ew;
        }
    }

    /** snaps shut on whatever steps in it: hurts and pins it in place; right-click to set it again */
    public static class BearTrap extends Block {
        public static final BooleanProperty ARMED = BooleanProperty.of("armed");
        private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 3, 14);

        public BearTrap(Settings s) {
            super(s);
            setDefaultState(getStateManager().getDefaultState().with(ARMED, true));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(ARMED);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }

        @Override
        public void onEntityCollision(BlockState st, World w, BlockPos pos, Entity e) {
            if (w.isClient || !st.get(ARMED) || !(e instanceof LivingEntity le)) return;
            if (le instanceof PlayerEntity p && (p.isSneaking() || p.isCreative())) return;    // careful steps
            le.damage(w.getDamageSources().generic(), 6f);
            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 6, false, false));
            w.setBlockState(pos, st.with(ARMED, false));
            w.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1.2f, 0.6f);
            Noise.emit((ServerWorld) w, Vec3d.ofCenter(pos), 10);
        }

        @Override
        public ActionResult onUse(BlockState st, World w, BlockPos pos, PlayerEntity p, Hand hand, BlockHitResult hit) {
            if (st.get(ARMED)) return ActionResult.PASS;
            if (!w.isClient) {
                w.setBlockState(pos, st.with(ARMED, true));
                w.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 1f, 0.8f);
            }
            return ActionResult.success(w.isClient);
        }
    }

    /** nails through a board: anything hostile walking over it gets hurt */
    public static class SpikeStrip extends Block {
        private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 2, 16);

        public SpikeStrip(Settings s) {
            super(s);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return SHAPE;
        }

        @Override
        public void onEntityCollision(BlockState st, World w, BlockPos pos, Entity e) {
            if (w.isClient || !(e instanceof LivingEntity le) || e.age % 10 != 0) return;
            if (le instanceof PlayerEntity p && (p.isSneaking() || p.isCreative())) return;
            le.damage(w.getDamageSources().cactus(), le instanceof HostileEntity ? 3f : 1f);
        }
    }

    /** tangles and cuts: slows anything to a crawl, hurts them while they struggle */
    public static class BarbedWire extends Block {
        public BarbedWire(Settings s) {
            super(s);
        }

        @Override
        public void onEntityCollision(BlockState st, World w, BlockPos pos, Entity e) {
            e.slowMovement(st, new Vec3d(0.3, 0.7, 0.3));
            if (!w.isClient && e instanceof LivingEntity le && e.age % 20 == 0 && !(le instanceof PlayerEntity p && p.isCreative()))
                le.damage(w.getDamageSources().cactus(), 1f);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return VoxelShapes.empty();
        }
    }

    /** a string of tin cans: anything that walks through rattles them, and you hear about it from a way off */
    public static class TinCanAlarm extends HorizontalFacingBlock {
        public static final BooleanProperty RATTLED = BooleanProperty.of("rattled");
        private static final VoxelShape NS = Block.createCuboidShape(0, 2, 6, 16, 10, 10), EW = Block.createCuboidShape(6, 2, 0, 10, 10, 16);

        public TinCanAlarm(Settings s) {
            super(s);
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(RATTLED, false));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> b) {
            b.add(FACING, RATTLED);
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().rotateYClockwise());
        }

        @Override
        public VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return st.get(FACING).getAxis() == Direction.Axis.Z ? EW : NS;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState st, BlockView w, BlockPos pos, ShapeContext ctx) {
            return VoxelShapes.empty();
        }

        @Override
        public void onEntityCollision(BlockState st, World w, BlockPos pos, Entity e) {
            if (w.isClient || st.get(RATTLED) || !(e instanceof LivingEntity) || e instanceof PlayerEntity) return;
            w.setBlockState(pos, st.with(RATTLED, true));
            w.scheduleBlockTick(pos, this, 100);
            w.playSound(null, pos, SoundEvents.BLOCK_CHAIN_FALL, SoundCategory.BLOCKS, 2f, 1.6f);
            w.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.4f, 2f);
            for (ServerPlayerEntity p : ((ServerWorld) w).getPlayers())
                if (p.squaredDistanceTo(Vec3d.ofCenter(pos)) < 72 * 72)
                    p.sendMessage(Text.translatable("msg.outbreak.alarm", pos.getX(), pos.getZ()).formatted(Formatting.GOLD), true);
        }

        @Override
        public void scheduledTick(BlockState st, ServerWorld w, BlockPos pos, Random r) {
            w.setBlockState(pos, st.with(RATTLED, false));
        }
    }
}
