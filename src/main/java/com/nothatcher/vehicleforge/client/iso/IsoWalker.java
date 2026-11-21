package com.nothatcher.vehicleforge.client.iso;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * "Walk here" for the isometric view: finds a way on foot (A* over the blocks: steps up one, drops of up to three,
 * diagonals where no corner is cut, through doors and gates it can open) and walks it, then does whatever was
 * asked at the end (break, use, attack). Any movement key cancels it.
 */
public final class IsoWalker {
    private static final int MAX_NODES = 9000;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private static List<BlockPos> path;
    private static int idx;
    private static Predicate<BlockPos> goal;
    private static Runnable onArrive;
    private static double best;
    private static int sinceProgress;
    private static final Set<Long> opened = new HashSet<>();

    private IsoWalker() {}

    public static boolean active() {
        return path != null;
    }

    public static void cancel() {
        path = null;
        onArrive = null;
        goal = null;
    }

    // ---- what counts as walkable ----

    private static VoxelShape shape(World w, BlockPos p) {
        return w.getBlockState(p).getCollisionShape(w, p);
    }

    /** doors and gates you can open by hand count as open */
    private static boolean openable(BlockState s) {
        return (s.getBlock() instanceof DoorBlock && !s.isOf(Blocks.IRON_DOOR)) || s.getBlock() instanceof FenceGateBlock;
    }

    /** you can stand with your feet here: nothing solid, or only something low (carpet, slab, snow) */
    private static boolean low(World w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        if (!s.getFluidState().isEmpty()) return false;
        if (openable(s)) return true;
        VoxelShape sh = s.getCollisionShape(w, p);
        return sh.isEmpty() || sh.getMax(Direction.Axis.Y) <= 0.5;
    }

    /** your head fits here */
    private static boolean clear(World w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        if (!s.getFluidState().isEmpty()) return false;
        return openable(s) || s.getCollisionShape(w, p).isEmpty();
    }

    static boolean standable(World w, BlockPos p) {
        if (!low(w, p) || !clear(w, p.up())) return false;
        VoxelShape here = shape(w, p);
        if (!here.isEmpty() && !openable(w.getBlockState(p))) return true;          // standing on a slab or carpet in this block
        VoxelShape below = shape(w, p.down());
        return !below.isEmpty() && below.getMax(Direction.Axis.Y) > 0.8;
    }

    // ---- searching ----

    private record Node(long pos, double f) {}

    private static List<BlockPos> search(World w, BlockPos start, Predicate<BlockPos> goalTest, Vec3d toward) {
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        Map<Long, Double> g = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        long s0 = start.asLong();
        g.put(s0, 0.0);
        open.add(new Node(s0, h(start, toward)));
        int expanded = 0;
        BlockPos.Mutable n = new BlockPos.Mutable();
        while (!open.isEmpty() && expanded++ < MAX_NODES) {
            Node cur = open.poll();
            BlockPos p = BlockPos.fromLong(cur.pos);
            double gp = g.get(cur.pos);
            if (cur.f > gp + h(p, toward) + 1e-6) continue;                         // stale entry
            if (goalTest.test(p)) return unwind(parent, cur.pos);
            if (p.getManhattanDistance(start) > 96) continue;
            for (int[] d : DIRS) {
                boolean diag = d[0] != 0 && d[1] != 0;
                for (int dy = 1; dy >= -3; dy--) {
                    n.set(p.getX() + d[0], p.getY() + dy, p.getZ() + d[1]);
                    if (!canStep(w, p, n, dy, diag)) continue;
                    long key = n.asLong();
                    double cost = gp + (diag ? 1.414 : 1.0) + (dy > 0 ? 0.6 : dy < 0 ? -dy * 0.3 : 0);
                    Double old = g.get(key);
                    if (old != null && old <= cost) break;
                    g.put(key, cost);
                    parent.put(key, cur.pos);
                    open.add(new Node(key, cost + h(n, toward)));
                    break;                                                          // one landing per direction
                }
            }
        }
        return null;
    }

    private static boolean canStep(World w, BlockPos from, BlockPos to, int dy, boolean diag) {
        if (dy > 0) {
            if (diag || !clear(w, from.up(2))) return false;                      // jumping needs headroom
            return standable(w, to);
        }
        if (dy == 0) {
            if (!standable(w, to)) return false;
            if (!diag) return true;
            // no cutting corners
            BlockPos a = new BlockPos(to.getX(), from.getY(), from.getZ()), b = new BlockPos(from.getX(), from.getY(), to.getZ());
            return low(w, a) && clear(w, a.up()) && low(w, b) && clear(w, b.up());
        }
        if (diag) return false;
        // dropping: the space above the landing must be open all the way up to where you step off
        BlockPos top = new BlockPos(to.getX(), from.getY(), to.getZ());
        if (!clear(w, top) || !clear(w, top.up())) return false;
        for (int j = 1; j < -dy; j++) if (!clear(w, top.down(j))) return false;
        return standable(w, to);
    }

    private static double h(BlockPos p, Vec3d toward) {
        double dx = p.getX() + 0.5 - toward.x, dy = p.getY() - toward.y, dz = p.getZ() + 0.5 - toward.z;
        return Math.sqrt(dx * dx + dy * dy * 0.5 + dz * dz);
    }

    private static List<BlockPos> unwind(Map<Long, Long> parent, long end) {
        List<BlockPos> out = new ArrayList<>();
        Long k = end;
        while (k != null) {
            out.add(BlockPos.fromLong(k));
            k = parent.get(k);
        }
        Collections.reverse(out);
        return out;
    }

    // ---- starting and walking ----

    private static BlockPos feet(World w, ClientPlayerEntity p) {
        BlockPos b = BlockPos.ofFloored(p.getX(), p.getY() + 0.01, p.getZ());
        if (standable(w, b)) return b;
        if (standable(w, b.up())) return b.up();
        if (standable(w, b.down())) return b.down();
        return b;
    }

    /**
     * Walk until {@code goalTest} holds for the block your feet are in, then run {@code then}.
     * @param toward roughly where that is (guides the search)
     */
    public static boolean start(MinecraftClient mc, Predicate<BlockPos> goalTest, Vec3d toward, Runnable then) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return false;
        BlockPos from = feet(mc.world, p);
        if (goalTest.test(from)) {
            cancel();
            if (then != null) then.run();
            return true;
        }
        List<BlockPos> found = search(mc.world, from, goalTest, toward);
        if (found == null || found.size() < 2) {
            p.sendMessage(Text.translatable("msg.vehicleforge.iso.no_path").formatted(Formatting.GRAY), true);
            return false;
        }
        path = found;
        idx = 1;
        goal = goalTest;
        onArrive = then;
        best = Double.MAX_VALUE;
        sinceProgress = 0;
        opened.clear();
        return true;
    }

    /** each tick while walking: drive the keys toward the next step */
    static void steer(MinecraftClient mc, ClientPlayerEntity p, Input in) {
        World w = mc.world;
        if (w == null || path == null) {
            cancel();
            return;
        }
        BlockPos at = feet(w, p);
        if (goal != null && goal.test(at) && p.isOnGround()) {
            arrive(in);
            return;
        }
        BlockPos next = path.get(idx);
        Vec3d c = Vec3d.ofBottomCenter(next);
        double dx = c.x - p.getX(), dz = c.z - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.35 && Math.abs(p.getY() - next.getY()) < 1.1) {
            idx++;
            best = Double.MAX_VALUE;
            if (idx >= path.size()) {
                arrive(in);
                return;
            }
            next = path.get(idx);
            c = Vec3d.ofBottomCenter(next);
            dx = c.x - p.getX();
            dz = c.z - p.getZ();
            dist = Math.sqrt(dx * dx + dz * dz);
        }
        // open a closed door or gate in the way
        for (BlockPos q : new BlockPos[]{next, next.up()}) {
            BlockState s = w.getBlockState(q);
            if (openable(s) && s.contains(DoorBlock.OPEN) && !s.get(DoorBlock.OPEN) && opened.add(q.asLong()) && dist < 2.2) {
                Direction side = Direction.getFacing(p.getX() - (q.getX() + 0.5), 0, p.getZ() - (q.getZ() + 0.5));
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(q).add(Vec3d.of(side.getVector()).multiply(0.5)), side, q, false));
                p.swingHand(Hand.MAIN_HAND);
            } else if (s.getBlock() instanceof FenceGateBlock && !s.get(FenceGateBlock.OPEN) && opened.add(q.asLong()) && dist < 2.2) {
                Direction side = Direction.getFacing(p.getX() - (q.getX() + 0.5), 0, p.getZ() - (q.getZ() + 0.5));
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(q), side, q, false));
                p.swingHand(Hand.MAIN_HAND);
            }
        }
        // stuck?
        if (dist < best - 0.05) {
            best = dist;
            sinceProgress = 0;
        } else if (++sinceProgress > 60) {
            p.sendMessage(Text.translatable("msg.vehicleforge.iso.stuck").formatted(Formatting.GRAY), true);
            cancel();
            in.movementForward = 0;
            return;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        p.setYaw(yaw);
        p.setBodyYaw(yaw);
        p.setHeadYaw(yaw);
        in.movementForward = dist < 0.6 ? 0.5f : 1f;
        in.movementSideways = 0;
        in.jumping = (next.getY() > p.getY() + 0.4 && dist < 1.6) || (p.horizontalCollision && p.isOnGround());
    }

    private static void arrive(Input in) {
        Runnable then = onArrive;
        cancel();
        in.movementForward = 0;
        in.movementSideways = 0;
        in.jumping = false;
        if (then != null) then.run();
    }

    /** a spot to stand on near where you clicked (on top of the block, or in front of the face you clicked) */
    public static BlockPos spotNear(World w, BlockHitResult hit) {
        BlockPos base = hit.getSide() == Direction.UP ? hit.getBlockPos().up() : hit.getBlockPos().offset(hit.getSide());
        for (int dy = 0; dy >= -4; dy--) if (standable(w, base.up(dy))) return base.up(dy);
        if (standable(w, base.up())) return base.up();
        for (int r = 1; r <= 2; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    for (int dy = 1; dy >= -2; dy--) {
                        BlockPos q = base.add(dx, dy, dz);
                        if (standable(w, q)) return q;
                    }
        return null;
    }
}
