package com.nothatcher.outbreak;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.EnumSet;

/** Walk towards the last noise heard (gunfire, engines) when there's nobody to chase. */
public class NoiseGoal extends Goal {
    private final PathAwareEntity mob;
    private int repath;

    public NoiseGoal(PathAwareEntity mob) {
        this.mob = mob;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        Noise.Target t = Noise.get(mob);
        return t != null && mob.getTarget() == null && mob.squaredDistanceTo(t.pos()) > 9;
    }

    @Override
    public boolean shouldContinue() {
        Noise.Target t = Noise.get(mob);
        if (t == null || mob.getTarget() != null) return false;
        if (mob.squaredDistanceTo(t.pos()) < 9) {
            Noise.clear(mob);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void tick() {
        Noise.Target t = Noise.get(mob);
        if (t == null) return;
        if (--repath <= 0) {
            repath = 40;
            mob.getNavigation().startMovingTo(t.pos().x, t.pos().y, t.pos().z, 1.0);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
