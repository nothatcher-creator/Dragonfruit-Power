package com.nothatcher.outbreak;

import com.nothatcher.outbreak.BarricadeBlock.Part;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Project Zomboid-style barricades. With a claw hammer, a plank and two nails you can board up a door, a window
 * or any one-block gap in a wall, up to four planks deep; or cover it with sheet metal (four nails), which is much
 * tougher. Zombies that are after you, or after a noise behind it, beat on a barricade until the boards give way.
 * Prying boards off again (claw hammer or crowbar) gives the planks back.
 */
public final class Barricades {
    public static final int MAX_BOARDS = 4;
    public static final float BOARD_HP = 60f, METAL_HP = 400f;
    public static final int NAILS_PER_BOARD = 2, NAILS_PER_METAL = 4;

    private Barricades() {}

    // ------------------------------------------------------------------ what can be barricaded

    public static boolean isPlank(ItemStack s) {
        return s.isIn(ItemTags.PLANKS);
    }

    public static boolean isGlass(BlockView w, BlockPos pos, BlockState st) {
        if (st.getSoundGroup() != BlockSoundGroup.GLASS || st.getLuminance() != 0) return false;
        float h = st.getHardness(w, pos);
        return h >= 0 && h < 1;
    }

    private static boolean wall(BlockView w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        return s.isFullCube(w, p) || s.getBlock() instanceof BarricadeBlock || isGlass(w, p, s);
    }

    private static boolean passable(BlockView w, BlockPos p) {
        return w.getBlockState(p).getCollisionShape(w, p).isEmpty();
    }

    /** the axis you'd walk through a gap along: walls on both sides across it, open in front and behind */
    private static Direction.Axis gapAxis(BlockView w, BlockPos pos) {
        if (wall(w, pos.east()) && wall(w, pos.west()) && passable(w, pos.north()) && passable(w, pos.south())) return Direction.Axis.Z;
        if (wall(w, pos.north()) && wall(w, pos.south()) && passable(w, pos.east()) && passable(w, pos.west())) return Direction.Axis.X;
        return null;
    }

    /** an empty one-block gap in a wall: a smashed window, or a doorway with no door */
    public static boolean isOpening(BlockView w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        return (st.isAir() || st.isReplaceable()) && passable(w, pos) && gapAxis(w, pos) != null;
    }

    private static boolean barricadable(World w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        if (st.getBlock() instanceof DoorBlock) {
            BlockPos lower = st.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            return w.getBlockState(lower).getBlock() instanceof DoorBlock && w.getBlockState(lower.up()).getBlock() instanceof DoorBlock;
        }
        return isGlass(w, pos, st) || isOpening(w, pos);
    }

    public static boolean isBarricade(World w, BlockPos pos) {
        return w.getBlockState(pos).getBlock() instanceof BarricadeBlock;
    }

    /** the block that holds a barricade's boards: the lower half for a door */
    public static BlockPos master(World w, BlockPos pos) {
        BlockState st = w.getBlockState(pos);
        return st.getBlock() instanceof BarricadeBlock && st.get(BarricadeBlock.PART) == Part.UPPER ? pos.down() : pos;
    }

    /** can another plank go on here? */
    public static boolean canBoard(World w, BlockPos pos) {
        BlockState st = w.getBlockState(master(w, pos));
        if (st.getBlock() instanceof BarricadeBlock) return !st.get(BarricadeBlock.METAL) && st.get(BarricadeBlock.BOARDS) < MAX_BOARDS;
        return barricadable(w, pos);
    }

    /** sheet metal only goes over a bare opening */
    public static boolean canMetal(World w, BlockPos pos) {
        return !isBarricade(w, pos) && barricadable(w, pos);
    }

    /**
     * What you're looking at, if it can be barricaded or already is: the first door, window or barricade along
     * your line of sight, or an empty gap in a wall on the way to it.
     */
    public static BlockPos target(World w, PlayerEntity p, double reach) {
        Vec3d eye = p.getEyePos(), dir = p.getRotationVec(1f);
        BlockPos last = null;
        for (double t = 0; t <= reach; t += 0.1) {
            BlockPos bp = BlockPos.ofFloored(eye.add(dir.multiply(t)));
            if (bp.equals(last)) continue;
            last = bp;
            BlockState st = w.getBlockState(bp);
            if (st.getBlock() instanceof BarricadeBlock || st.getBlock() instanceof DoorBlock || isGlass(w, bp, st)) return bp;
            if (!passable(w, bp)) return null;
            if (t > 0.8 && isOpening(w, bp)) return bp;
        }
        return null;
    }

    /** the side of the opening the player is standing on: the boards go on that side */
    public static Direction side(World w, BlockPos pos, PlayerEntity p) {
        BlockState st = w.getBlockState(pos);
        Direction.Axis a;
        if (st.getBlock() instanceof DoorBlock) a = st.get(DoorBlock.FACING).getAxis();
        else if (st.getBlock() instanceof BarricadeBlock) return st.get(BarricadeBlock.FACING);
        else {
            a = gapAxisLoose(w, pos);
            if (a == null) a = p.getHorizontalFacing().getAxis();
        }
        double d = a == Direction.Axis.X ? p.getX() - (pos.getX() + 0.5) : p.getZ() - (pos.getZ() + 0.5);
        return Direction.from(a, d >= 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    /** like gapAxis, but for a window pane or glass block, which is itself solid */
    private static Direction.Axis gapAxisLoose(BlockView w, BlockPos pos) {
        boolean ew = wall(w, pos.east()) && wall(w, pos.west()), ns = wall(w, pos.north()) && wall(w, pos.south());
        if (ew && !ns) return Direction.Axis.Z;
        if (ns && !ew) return Direction.Axis.X;
        return null;
    }

    // ------------------------------------------------------------------ building and breaking

    private static final int QUIET = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    /** turn a door, window or gap into a barricade with nothing on it yet */
    private static BlockPos cover(ServerWorld w, BlockPos pos, Direction side, boolean metal) {
        BlockState st = w.getBlockState(pos);
        BlockState base = OutbreakBlocks.BARRICADE.getDefaultState().with(BarricadeBlock.FACING, side)
                .with(BarricadeBlock.BOARDS, 0).with(BarricadeBlock.METAL, metal);
        if (st.getBlock() instanceof DoorBlock) {
            BlockPos lower = st.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            BlockState dl = w.getBlockState(lower), du = w.getBlockState(lower.up());
            if (dl.contains(Properties.OPEN)) dl = dl.with(Properties.OPEN, false);
            if (du.contains(Properties.OPEN)) du = du.with(Properties.OPEN, false);
            w.setBlockState(lower, base.with(BarricadeBlock.PART, Part.LOWER), QUIET);
            w.setBlockState(lower.up(), base.with(BarricadeBlock.PART, Part.UPPER), QUIET);
            if (w.getBlockEntity(lower) instanceof BarricadeBlockEntity be) be.original = dl;
            if (w.getBlockEntity(lower.up()) instanceof BarricadeBlockEntity be) {
                be.original = du;
                be.sync();
            }
            return lower;
        }
        w.setBlockState(pos, base, Block.NOTIFY_ALL);
        if (w.getBlockEntity(pos) instanceof BarricadeBlockEntity be) be.original = st.isReplaceable() && !isGlass(w, pos, st) ? Blocks.AIR.getDefaultState() : st;
        return pos;
    }

    private static void setBoards(ServerWorld w, BlockPos master, int n) {
        BlockState st = w.getBlockState(master);
        w.setBlockState(master, st.with(BarricadeBlock.BOARDS, n), QUIET);
        if (st.get(BarricadeBlock.PART) == Part.LOWER) {
            BlockState up = w.getBlockState(master.up());
            if (up.getBlock() instanceof BarricadeBlock) w.setBlockState(master.up(), up.with(BarricadeBlock.BOARDS, n), QUIET);
        }
    }

    /** nail on one more plank (the first one barricades the opening) */
    public static boolean addBoard(ServerWorld w, BlockPos pos, Direction side, String plankId) {
        if (!canBoard(w, pos)) return false;
        BlockPos m = isBarricade(w, pos) ? master(w, pos) : cover(w, pos, side, false);
        if (!(w.getBlockEntity(m) instanceof BarricadeBlockEntity be)) return false;
        setBoards(w, m, w.getBlockState(m).get(BarricadeBlock.BOARDS) + 1);
        be.planks.add(plankId);
        be.hp = BOARD_HP;
        be.sync();
        return true;
    }

    public static boolean addMetal(ServerWorld w, BlockPos pos, Direction side) {
        if (!canMetal(w, pos)) return false;
        BlockPos m = cover(w, pos, side, true);
        if (!(w.getBlockEntity(m) instanceof BarricadeBlockEntity be)) return false;
        be.hp = METAL_HP;
        be.sync();
        return true;
    }

    /** take the barricade away and put back what it covered; zombies breaking through a window shatter the glass */
    private static void restore(ServerWorld w, BlockPos master, boolean forced) {
        BlockState st = w.getBlockState(master);
        if (!(st.getBlock() instanceof BarricadeBlock) || !(w.getBlockEntity(master) instanceof BarricadeBlockEntity be)) return;
        BlockState orig = be.original;
        if (st.get(BarricadeBlock.PART) == Part.LOWER) {
            BlockState up = w.getBlockState(master.up()).getBlock() instanceof BarricadeBlock
                    && w.getBlockEntity(master.up()) instanceof BarricadeBlockEntity ub ? ub.original : Blocks.AIR.getDefaultState();
            if (orig.getBlock() instanceof DoorBlock && up.getBlock() == orig.getBlock()) {
                w.setBlockState(master, orig, QUIET);
                w.setBlockState(master.up(), up, QUIET);
            } else {
                w.setBlockState(master, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                w.setBlockState(master.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
            return;
        }
        if (forced && isGlass(w, master, orig)) {
            w.setBlockState(master, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            w.playSound(null, master, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1f, 0.9f);
            return;
        }
        w.setBlockState(master, orig, Block.NOTIFY_ALL);
    }

    /** pry off the outermost plank (or the sheet metal); returns what comes off */
    public static ItemStack removeTop(ServerWorld w, BlockPos pos) {
        BlockPos m = master(w, pos);
        BlockState st = w.getBlockState(m);
        if (!(st.getBlock() instanceof BarricadeBlock) || !(w.getBlockEntity(m) instanceof BarricadeBlockEntity be)) return ItemStack.EMPTY;
        if (st.get(BarricadeBlock.METAL)) {
            restore(w, m, false);
            return new ItemStack(OutbreakItems.SHEET_METAL);
        }
        String id = be.planks.isEmpty() ? "minecraft:oak_planks" : be.planks.remove(be.planks.size() - 1);
        ItemStack back = new ItemStack(Registries.ITEM.get(new Identifier(id)));
        if (back.isEmpty()) back = new ItemStack(net.minecraft.item.Items.OAK_PLANKS);
        int n = st.get(BarricadeBlock.BOARDS) - 1;
        if (n <= 0) restore(w, m, false);
        else {
            setBoards(w, m, n);
            be.hp = BOARD_HP;
            be.sync();
        }
        return back;
    }

    /** a zombie's blow; returns true if that broke through */
    public static boolean damage(ServerWorld w, BlockPos pos, float amount) {
        BlockPos m = master(w, pos);
        BlockState st = w.getBlockState(m);
        if (!(st.getBlock() instanceof BarricadeBlock) || !(w.getBlockEntity(m) instanceof BarricadeBlockEntity be)) return false;
        boolean metal = st.get(BarricadeBlock.METAL);
        Vec3d c = Vec3d.ofCenter(pos);
        w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, metal ? Blocks.IRON_BLOCK.getDefaultState() : Blocks.OAK_PLANKS.getDefaultState()),
                c.x, c.y, c.z, 8, 0.3, 0.4, 0.3, 0.1);
        be.hp -= amount;
        if (be.hp > 0) {
            be.sync();
            return false;
        }
        w.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.9f, metal ? 0.6f : 1.1f);
        if (metal) {
            restore(w, m, true);
            return true;
        }
        int n = st.get(BarricadeBlock.BOARDS) - 1;
        if (!be.planks.isEmpty()) be.planks.remove(be.planks.size() - 1);
        if (n <= 0) {
            restore(w, m, true);
            return true;
        }
        setBoards(w, m, n);
        be.hp = BOARD_HP;
        be.sync();
        return false;
    }

    /** "Barricade: 3 planks (72%)" */
    public static Text describe(World w, BlockPos pos) {
        BlockPos m = master(w, pos);
        BlockState st = w.getBlockState(m);
        if (!(st.getBlock() instanceof BarricadeBlock)) return Text.empty();
        float hp = w.getBlockEntity(m) instanceof BarricadeBlockEntity be ? be.hp : BOARD_HP;
        boolean metal = st.get(BarricadeBlock.METAL);
        int pct = Math.max(1, Math.round(100f * hp / (metal ? METAL_HP : BOARD_HP)));
        return metal ? Text.translatable("msg.outbreak.barricade_metal", pct)
                : Text.translatable("msg.outbreak.barricade_boards", st.get(BarricadeBlock.BOARDS), pct);
    }
}
