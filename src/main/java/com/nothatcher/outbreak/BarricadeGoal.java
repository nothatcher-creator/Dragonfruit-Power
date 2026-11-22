package com.nothatcher.outbreak;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * A zombie that's after you (or after a noise) and finds a barricade between it and you stops and beats on it,
 * about once a second, until it gives way. The banging is itself a noise, so others come to help.
 */
public class BarricadeGoal extends Goal {
    private final ZombieEntity mob;
    private BlockPos target;
    private int scan, cooldown, patience;

    public BarricadeGoal(ZombieEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    /** where the zombie is trying to get to */
    private Vec3d goal() {
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive()) return t.getPos();
        Noise.Target n = Noise.get(mob);
        return n == null ? null : n.pos();
    }

    private BlockPos find(Vec3d goal) {
        Vec3d me = mob.getPos();
        double toGoal = me.squaredDistanceTo(goal);
        if (toGoal < 2.25) return null;
        Vec3d dir = new Vec3d(goal.x - me.x, 0, goal.z - me.z).normalize();
        BlockPos base = mob.getBlockPos(), best = null;
        double bestD = 4;
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos bp = base.add(dx, dy, dz);
                    if (!Barricades.isBarricade(mob.getWorld(), bp)) continue;
                    Vec3d c = Vec3d.ofCenter(bp);
                    Vec3d to = new Vec3d(c.x - me.x, 0, c.z - me.z);
                    double d = me.squaredDistanceTo(c);
                    // it must be on the way: towards the goal, and nearer than it (or the noise came from the barricade itself)
                    boolean atIt = c.squaredDistanceTo(goal) < 6.25;
                    if (!atIt && (to.lengthSquared() < 1e-4 || to.normalize().dotProduct(dir) < 0.3 || d > toGoal)) continue;
                    if (d < bestD) {
                        bestD = d;
                        best = bp;
                    }
                }
            }
        }
        return best;
    }

    @Override
    public boolean canStart() {
        if (--scan > 0) return false;
        scan = 10;
        Vec3d g = goal();
        if (g == null) return false;
        target = find(g);
        return target != null;
    }

    @Override
    public boolean shouldContinue() {
        if (target == null || !Barricades.isBarricade(mob.getWorld(), target)) return false;
        if (mob.squaredDistanceTo(Vec3d.ofCenter(target)) > 6.25) return false;
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive() && mob.squaredDistanceTo(t) < 2.25) return false;      // it reached you some other way
        if (goal() != null) patience = 400;
        return --patience > 0;                                                             // keeps at it a while after losing track of you
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        cooldown = 8;
        patience = 400;
    }

    @Override
    public void stop() {
        target = null;
        scan = 20;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) return;
        Vec3d c = Vec3d.ofCenter(target);
        mob.getNavigation().stop();
        mob.getLookControl().lookAt(c.x, c.y, c.z);
        if (--cooldown > 0) return;
        cooldown = 18 + mob.getRandom().nextInt(12);
        if (!(mob.getWorld() instanceof ServerWorld w)) return;
        mob.swingHand(Hand.MAIN_HAND);
        boolean metal = w.getBlockState(Barricades.master(w, target)).get(BarricadeBlock.METAL);
        w.playSound(null, target, metal ? SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundCategory.HOSTILE, 0.8f, 0.9f + mob.getRandom().nextFloat() * 0.2f);
        if (mob.getRandom().nextInt(3) == 0) Noise.emit(w, c, 14);
        if (Barricades.damage(w, target, 3f + mob.getRandom().nextFloat() * 3f)) target = null;
    }
}
