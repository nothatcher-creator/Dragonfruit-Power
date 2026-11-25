package com.nothatcher.vehicleforge.traffic;

import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.block.CityBlocks;
import com.nothatcher.vehicleforge.block.FacingShapeBlock;
import com.nothatcher.vehicleforge.block.TrafficLightBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The driver of a city traffic car. The city is a grid of 81-block cells with a road band (29 wide) along the low
 * edge of each; traffic keeps right, two lanes each way, and follows the lane markings the generator paints:
 * southbound and westbound lanes sit at 7.5 / 11.5 across the band, northbound and eastbound at 21.5 / 17.
 * At each junction it reads the real traffic lights (or stop signs), picks a way on where the road continues,
 * keeps its distance from whatever's ahead, and buses pull in at the bus stops.
 */
public final class TrafficAI {
    static final int CELL = 81, BAND = 29, GROUND = 63, BASE = 64;
    private static final double[] LOW = {7.5, 11.5}, HIGH = {21.5, 17.0};   // [outer lane, inner lane]

    private TrafficAI() {}

    public static final class Brain {
        public Direction dir;
        public int lane;
        public final boolean bus;
        /** turning through a junction: where to, and the corner to swing round */
        Direction next;
        double apexX, apexZ, exitX, exitZ;
        boolean pastApex;
        /** the junction (along-index) this car has already decided on / stopped at */
        int decidedAt = Integer.MIN_VALUE, stoppedAt = Integer.MIN_VALUE, busDoneAt = Integer.MIN_VALUE;
        int stopTimer, blockedTicks, stuckTicks, offRoadTicks, honkCooldown, panicTicks;
        float cruise, panicSteer;
        public boolean dead;

        public Brain(Direction dir, int lane, boolean bus, float cruise) {
            this.dir = dir;
            this.lane = lane;
            this.bus = bus;
            this.cruise = cruise;
        }
    }

    // ------------------------------------------------------------------ geometry

    static double laneCoord(Direction d, int lane, double cross) {
        double o = Math.floorDiv((int) Math.floor(cross), CELL) * (double) CELL;
        return o + ((d == Direction.SOUTH || d == Direction.WEST) ? LOW[lane] : HIGH[lane]);
    }

    static double along(Direction d, double x, double z) {
        return d.getAxis() == Direction.Axis.Z ? z : x;
    }

    static double cross(Direction d, double x, double z) {
        return d.getAxis() == Direction.Axis.Z ? x : z;
    }

    static float yawOf(Direction d) {
        return switch (d) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            default -> -90f;
        };
    }

    static boolean isRoad(BlockState s) {
        Block b = s.getBlock();
        return b == CityBlocks.ASPHALT || b == CityBlocks.LINE_WHITE || b == CityBlocks.LINE_YELLOW || b == CityBlocks.LINE_DOUBLE_YELLOW
                || b == CityBlocks.CROSSWALK || b == CityBlocks.MANHOLE || b == CityBlocks.STORM_DRAIN || b == CityBlocks.POTHOLE;
    }

    static boolean roadAt(World w, double x, double z) {
        BlockPos p = BlockPos.ofFloored(x, GROUND, z);
        return w.isChunkLoaded(p) && isRoad(w.getBlockState(p));
    }

    /** distance from the car's front to its stop line ahead, and the junction index it belongs to; NaN if committed */
    private static double[] stopLine(Direction d, double af) {
        int s = d.getDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        int v = Math.floorMod((int) Math.floor(af), CELL);
        if (v < BAND) return null;                                   // already in the junction
        if (s > 0) {
            int k = Math.floorDiv((int) Math.floor(af), CELL) + 1;
            return new double[]{(k * CELL - 4) - af, k};
        }
        int k = Math.floorDiv((int) Math.floor(af), CELL);
        return new double[]{af - (k * CELL + 33), k};
    }

    // ------------------------------------------------------------------ junction control

    private enum Control { NONE, STOP, GREEN, YELLOW, RED }

    private static Control control(World w, Direction d, int k, double crossCoord) {
        int o = Math.floorDiv((int) Math.floor(crossCoord), CELL) * CELL;
        int jx = d.getAxis() == Direction.Axis.Z ? o : k * CELL, jz = d.getAxis() == Direction.Axis.Z ? k * CELL : o;
        boolean sign = false;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int cx : new int[]{2, 26}) {
            for (int cz : new int[]{2, 26}) {
                for (int y = BASE + 4; y <= BASE + 5; y++) {
                    p.set(jx + cx, y, jz + cz);
                    if (!w.isChunkLoaded(p)) continue;
                    BlockState st = w.getBlockState(p);
                    if (st.getBlock() instanceof TrafficLightBlock && st.get(FacingShapeBlock.FACING).getAxis() == d.getAxis()) {
                        if (!st.get(TrafficLightBlock.POWERED)) return Control.STOP;          // a dead light is a four-way stop
                        return switch (st.get(TrafficLightBlock.PHASE)) {
                            case 0 -> Control.GREEN;
                            case 1 -> Control.YELLOW;
                            default -> Control.RED;
                        };
                    }
                }
                p.set(jx + cx, BASE, jz + cz);
                if (w.isChunkLoaded(p) && w.getBlockState(p).isOf(CityBlocks.STOP_SIGN)) sign = true;
            }
        }
        return sign ? Control.STOP : Control.NONE;
    }

    // ------------------------------------------------------------------ driving

    /** this tick's {pedal, steer} */
    public static float[] drive(VehicleEntity v) {
        Brain b = v.brain;
        World w = v.getWorld();
        double x = v.getX(), z = v.getZ();
        float speed = v.speed;
        if (b.honkCooldown > 0) b.honkCooldown--;

        if (b.panicTicks > 0) {                                      // swerving off in a panic, then abandoned
            b.panicTicks--;
            if (b.panicTicks == 0) {
                v.setAi(false);
                if (v.getRandom().nextInt(3) == 0) v.honk();
                return new float[]{-1, 0, 1};
            }
            return new float[]{1, b.panicSteer, 0};
        }

        Vec3d f = v.forward();
        double front = Math.max(1.2, v.type.frontZ);
        double fx = x + f.x * front, fz = z + f.z * front;
        float target = b.cruise;
        double tx, tz;                                               // steering target

        if (b.next != null) {
            // swinging through a junction: round the corner, then out along the new lane
            if (!b.pastApex && Math.hypot(b.apexX - x, b.apexZ - z) < (isRight(b.dir, b.next) ? 4.0 : 7.5)) b.pastApex = true;
            tx = b.pastApex ? b.exitX : b.apexX;
            tz = b.pastApex ? b.exitZ : b.apexZ;
            target = Math.min(target, isRight(b.dir, b.next) ? 0.22f : 0.3f);
            double past = (along(b.next, x, z) - along(b.next, b.exitX, b.exitZ)) * sign(b.next);
            if (b.pastApex && past > -1.5) {
                b.dir = b.next;
                b.next = null;
            }
        } else {
            double look = 5 + Math.abs(speed) * 10;
            double ax = along(b.dir, x, z) + sign(b.dir) * look;
            double lc = laneCoord(b.dir, b.lane, cross(b.dir, x, z));
            tx = b.dir.getAxis() == Direction.Axis.Z ? lc : ax;
            tz = b.dir.getAxis() == Direction.Axis.Z ? ax : lc;

            double af = along(b.dir, fx, fz);
            double[] sl = stopLine(b.dir, af);
            if (sl != null) {
                int k = (int) sl[1];
                double dist = sl[0];
                if (dist < 40) {
                    Control c = control(w, b.dir, k, cross(b.dir, x, z));
                    boolean mustStop = switch (c) {
                        case RED -> dist > -0.5;
                        case YELLOW -> dist > 5;
                        case STOP -> b.stoppedAt != k && dist > -0.5;
                        default -> false;
                    };
                    if (c == Control.NONE) target = Math.min(target, 0.32f);
                    if (mustStop) {
                        target = Math.min(target, (float) Math.max(0, Math.sqrt(Math.max(0, dist - 0.3) * 0.05)));
                        if (dist < 1.6 && Math.abs(speed) < 0.03f) {
                            target = 0;
                            if (c == Control.STOP && ++b.stopTimer > 24) {
                                b.stoppedAt = k;
                                b.stopTimer = 0;
                            }
                        }
                    }
                    if (dist < 2.5 && !mustStop && b.decidedAt != k) plan(w, v, b, k);
                }
                // buses pull in at the stop halfway down the block (south- and westbound kerb side)
                if (b.bus && (b.dir == Direction.SOUTH || b.dir == Direction.WEST)) {
                    int cell = Math.floorDiv((int) Math.floor(af), CELL);
                    double stopAt = cell * CELL + 52 + (b.dir == Direction.SOUTH ? 3 : -3);
                    double dd = (stopAt - af) * sign(b.dir);
                    if (b.busDoneAt != cell && dd > -1 && dd < 25) {
                        target = Math.min(target, (float) Math.sqrt(Math.max(0, dd) * 0.04));
                        if (dd < 1 && Math.abs(speed) < 0.03f && ++b.stopTimer > 100) {
                            b.busDoneAt = cell;
                            b.stopTimer = 0;
                        }
                    }
                }
            } else if (b.decidedAt == Integer.MIN_VALUE || stopLineIndexHere(b.dir, af) != b.decidedAt) {
                plan(w, v, b, stopLineIndexHere(b.dir, af));            // spawned or slipped into a junction undecided
            }
        }

        // keep a gap to anything ahead; pull round a parked car or wreck sitting in the lane
        Entity[] who = new Entity[1];
        double gap = gapAhead(v, f, who);
        if (b.next == null && gap < 8 && who[0] instanceof VehicleEntity o && !o.aiDriven() && b.blockedTicks > 30) {
            b.lane = 1 - b.lane;
            b.blockedTicks = 0;
        }
        if (gap < 20) {
            target = Math.min(target, (float) Math.max(0, (gap - 2.2) * 0.07));
            if (gap < 3.2) target = 0;
        }
        if (target == 0 && gap < 6 && Math.abs(speed) < 0.02f) {
            if (++b.blockedTicks > 60 && b.honkCooldown == 0) {
                v.honk();
                b.honkCooldown = 80 + v.getRandom().nextInt(80);
            }
        } else b.blockedTicks = 0;

        // stuck or off the road for long: give up (the manager takes it away out of sight)
        if (Math.abs(speed) < 0.01f) b.stuckTicks++;
        else b.stuckTicks = 0;
        if (!roadAt(w, x, z)) b.offRoadTicks++;
        else b.offRoadTicks = 0;
        if (b.stuckTicks > 900 || b.offRoadTicks > 60) b.dead = true;
        // the road ends ahead (dead end, broken bridge): stop
        if (b.next == null && !roadAt(w, fx + f.x * 3, fz + f.z * 3)) target = 0;

        float pedal = 0, hand = 0;
        if (speed < target - 0.025f) pedal = 1;
        else if (speed > target + 0.03f) {
            if (speed > 0.06f) pedal = -1;
            else hand = 1;
        }
        if (target < 0.01f && speed < 0.08f) {                     // hold it on the handbrake rather than creep into reverse
            pedal = 0;
            hand = 1;
        }
        double desired = Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        float err = MathHelper.wrapDegrees((float) desired - v.getYaw());
        float steer = MathHelper.clamp(-err / 14f, -1f, 1f);
        return new float[]{pedal, steer, hand};
    }

    private static int stopLineIndexHere(Direction d, double af) {
        return Math.floorDiv((int) Math.floor(af), CELL);
    }

    private static int sign(Direction d) {
        return d.getDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
    }

    static boolean isRight(Direction from, Direction to) {
        return from.rotateYClockwise() == to;
    }

    /** choose straight on, right or left at junction k, wherever the road actually continues */
    private static void plan(World w, VehicleEntity v, Brain b, int k) {
        b.decidedAt = k;
        double x = v.getX(), z = v.getZ();
        int o = Math.floorDiv((int) Math.floor(cross(b.dir, x, z)), CELL) * CELL;
        int jx = b.dir.getAxis() == Direction.Axis.Z ? o : k * CELL, jz = b.dir.getAxis() == Direction.Axis.Z ? k * CELL : o;
        List<Direction> opts = new ArrayList<>();
        Direction straight = b.dir, right = b.dir.rotateYClockwise(), left = b.dir.rotateYCounterclockwise();
        int r = v.getRandom().nextInt(100);
        Direction[] order = r < 60 ? new Direction[]{straight, right, left} : r < 80 ? new Direction[]{right, straight, left} : new Direction[]{left, straight, right};
        for (Direction d : order) {
            double[] e = exit(d, d == right ? 0 : d == left ? 1 : b.lane, jx, jz);
            Vec3d dv = Vec3d.of(d.getVector());
            if (roadAt(w, e[0] + dv.x * 8, e[1] + dv.z * 8) && roadAt(w, e[0] + dv.x * 20, e[1] + dv.z * 20)) opts.add(d);
        }
        if (opts.isEmpty()) {
            b.dead = true;
            return;
        }
        Direction d = opts.get(0);
        if (d == straight) return;                                   // just carry on in lane
        int lane = d == right ? 0 : 1;
        double[] e = exit(d, lane, jx, jz);
        b.next = d;
        b.lane = lane;
        b.exitX = e[0];
        b.exitZ = e[1];
        b.pastApex = false;
        // the corner: where the incoming lane line meets the outgoing one
        double inLane = laneCoord(b.dir, d == right ? 0 : 1, cross(b.dir, x, z));
        double outLane = b.dir.getAxis() == Direction.Axis.Z ? e[1] : e[0];
        b.apexX = b.dir.getAxis() == Direction.Axis.Z ? inLane : outLane;
        b.apexZ = b.dir.getAxis() == Direction.Axis.Z ? outLane : inLane;
    }

    /** a point just outside junction (jx, jz) on the given lane of the road heading d */
    private static double[] exit(Direction d, int lane, int jx, int jz) {
        double[] l = (d == Direction.SOUTH || d == Direction.WEST) ? LOW : HIGH;
        return switch (d) {
            case SOUTH -> new double[]{jx + l[lane], jz + BAND + 3};
            case NORTH -> new double[]{jx + l[lane], jz - 3};
            case EAST -> new double[]{jx + BAND + 3, jz + l[lane]};
            default -> new double[]{jx - 3, jz + l[lane]};
        };
    }

    /** distance from the front bumper to the nearest car, person or animal in the lane ahead */
    private static double gapAhead(VehicleEntity v, Vec3d f, Entity[] who) {
        double reach = 6 + Math.abs(v.speed) * 16;
        double front = Math.max(1.2, v.type.frontZ);
        Vec3d a = v.getPos().add(f.multiply(front)), c = a.add(f.multiply(reach));
        double hw = Math.min(1.4, v.type.width / 2 + 0.2);
        Vec3d side = new Vec3d(-f.z, 0, f.x).multiply(hw);
        Box box = new Box(a.add(side), c.subtract(side)).union(new Box(a.subtract(side), c.add(side))).stretch(0, 2.5, 0).expand(0, 0.5, 0);
        double best = 99;
        for (Entity e : v.getWorld().getOtherEntities(v, box, e -> e.isAlive() && !(e instanceof Monster) && e.getRootVehicle() != v
                && (e instanceof VehicleEntity || e instanceof LivingEntity))) {
            Vec3d d = e.getPos().subtract(a);
            double ahead = d.x * f.x + d.z * f.z;
            double lat = Math.abs(d.x * f.z - d.z * f.x);
            double half = e instanceof VehicleEntity o ? Math.max(1.0, Math.abs(o.type.rearZ)) * 0.9 : e.getWidth() / 2;
            if (lat > hw + (e instanceof VehicleEntity o2 ? o2.type.width / 2 : e.getWidth() / 2)) continue;
            double g = Math.max(0, ahead - half);
            if (g < best) {
                best = g;
                who[0] = e;
            }
        }
        return best;
    }

    /** somebody hit a traffic car: the driver leans on the horn, and bails out if it's badly damaged */
    public static void onHit(VehicleEntity v, DamageSource src) {
        if (src.getAttacker() instanceof PlayerEntity && v.brain != null && v.brain.honkCooldown == 0) {
            v.honk();
            v.brain.honkCooldown = 40;
        }
        if (v.getHealth() < v.type.health * 0.5f) v.setAi(false);
    }

    /** the outbreak reaches the driver: swerve, floor it, and leave the car where it ends up */
    public static void panic(VehicleEntity v) {
        if (v.brain == null) return;
        v.brain.panicTicks = 20 + v.getRandom().nextInt(30);
        v.brain.panicSteer = (v.getRandom().nextFloat() - 0.5f) * 1.6f;
    }
}
