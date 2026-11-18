package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A flying crescent of sword energy: glowing coloured edge with a white-hot core, tapering at the tips. */
public class SlashEffect extends VfxEffect {
    private final Vec3d start, dir;
    private final float speed, width, roll;
    private final int color;
    private Vec3d prev, cur;
    private boolean stopped;
    private final DustParticleEffect dust;

    public SlashEffect(Vec3d start, Vec3d dir, float speed, int life, float width, float roll, int color) {
        super(life);
        this.start = start;
        this.dir = dir.normalize();
        this.speed = speed;
        this.width = width;
        this.roll = roll;
        this.color = color;
        this.prev = start;
        this.cur = start;
        this.dust = new DustParticleEffect(new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f), 1.2f);
    }

    @Override
    protected void onTick(ClientWorld world) {
        prev = cur;
        if (stopped) return;
        Vec3d next = cur.add(dir.multiply(speed));
        if (mc().player != null) {
            BlockHitResult bh = world.raycast(new RaycastContext(cur, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc().player));
            if (bh.getType() != HitResult.Type.MISS) {
                next = bh.getPos();
                stopped = true;
                for (int i = 0; i < 12; i++) Particles.add(ParticleTypes.CRIT, next, Particles.randDir(world.random).multiply(0.5));
            }
        }
        cur = next;
        Vec3d[] b = frame();
        for (int s = -1; s <= 1; s += 2) {
            Vec3d tip = cur.add(b[0].multiply(s * width * 0.5)).subtract(dir.multiply(width * 0.2));
            Particles.add(dust, tip, Vec3d.ZERO);
        }
    }

    /** [side axis, up axis] of the crescent plane, rolled. */
    private Vec3d[] frame() {
        Vec3d[] b = Gfx.basis(dir);
        Vec3d side = Poses.rightOf(dir);
        Vec3d up = side.crossProduct(dir).normalize();
        double r = Math.toRadians(roll);
        Vec3d s2 = side.multiply(Math.cos(r)).add(up.multiply(Math.sin(r)));
        Vec3d u2 = s2.crossProduct(dir).normalize();
        return new Vec3d[]{s2, u2, b[0]};
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double p = progress(td);
        double fade = stopped ? Math.max(0, 1 - (age + td - life * 0.5) / 3.0) : 1 - Math.pow(p, 3);
        if (fade <= 0.01) return;
        Vec3d c = prev.lerp(cur, td);
        Vec3d[] b = frame();
        Vec3d side = b[0];
        double grow = Math.min(1, (age + td) / 3.0);
        double R = width * 0.5 * (0.6 + 0.4 * grow);
        int n = 14;
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        for (int layer = 0; layer < 2; layer++) {
            double thickMul = layer == 0 ? 1.0 : 0.35;
            int col = layer == 0 ? Gfx.argb(color, (float) (0.75 * fade)) : Gfx.argb(0xFFFFFF, (float) (0.95 * fade));
            Vec3d prevIn = null, prevOut = null;
            for (int i = 0; i <= n; i++) {
                double th = Math.toRadians(-75 + 150.0 * i / n);
                Vec3d radial = side.multiply(Math.sin(th)).add(dir.multiply(Math.cos(th)));
                Vec3d pt = c.add(side.multiply(Math.sin(th) * R)).add(dir.multiply((Math.cos(th) - 0.6) * R * 0.55));
                double w = 0.35 * R * Math.pow(Math.cos(th), 1.4) * thickMul + 0.01;
                Vec3d in = pt.subtract(radial.multiply(w));
                Vec3d out = pt.add(radial.multiply(w * 0.35));
                if (prevIn != null) Gfx.quad(vc, m, prevIn, prevOut, out, in, col);
                prevIn = in;
                prevOut = out;
            }
        }
        // soft afterglow trail
        Gfx.ribbon(vc, m, c.subtract(dir.multiply(R * 0.3)), c.subtract(dir.multiply(R * 1.6)), R * 1.2, Gfx.argb(color, (float) (0.12 * fade)));
    }
}
