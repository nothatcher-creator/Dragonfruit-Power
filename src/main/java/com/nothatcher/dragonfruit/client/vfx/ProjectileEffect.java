package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

/** Fire Fist / Ice Spear. Same straight-line maths as the server, so it stops where the hit happens. */
public class ProjectileEffect extends VfxEffect {
    private final int ownerId, kind;
    private final Vec3d start, dir;
    private final float speed, size;
    private Vec3d prev, cur;
    private boolean dead;

    public ProjectileEffect(int ownerId, int kind, Vec3d start, Vec3d dir, float speed, int life, float size) {
        super(life + 1);
        this.ownerId = ownerId;
        this.kind = kind;
        this.start = start;
        this.dir = dir.normalize();
        this.speed = speed;
        this.size = size;
        this.prev = start;
        this.cur = start;
    }

    @Override
    public boolean tick(ClientWorld world) {
        if (dead) return false;
        return super.tick(world);
    }

    @Override
    protected void onTick(ClientWorld world) {
        prev = cur;
        Vec3d next = cur.add(dir.multiply(speed));
        if (mc().player != null) {
            BlockHitResult bh = world.raycast(new RaycastContext(cur, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc().player));
            if (bh.getType() != HitResult.Type.MISS) {
                next = bh.getPos();
                dead = true;
            }
        }
        if (!world.getEntitiesByClass(LivingEntity.class, new Box(cur, next).expand(size),
                e -> e.getId() != ownerId && e.isAlive() && !e.isSpectator()).isEmpty()) {
            dead = true;
        }
        cur = next;
        trail(world);
    }

    private void trail(ClientWorld w) {
        if (kind == Vfx.P_FIRE_FIST) {
            for (int i = 0; i < 22; i++) {
                Vec3d p = cur.add(Particles.randDir(w.random).multiply(size * w.random.nextDouble()));
                Particles.add(ParticleTypes.FLAME, p, dir.multiply(-0.15).add(Particles.randDir(w.random).multiply(0.04)));
            }
            for (int i = 0; i < 4; i++) Particles.add(ParticleTypes.LARGE_SMOKE, cur.subtract(dir.multiply(size)), dir.multiply(-0.05));
            if (w.random.nextInt(2) == 0) Particles.add(ParticleTypes.LAVA, cur, Vec3d.ZERO);
        } else if (kind == Vfx.P_MAGMA) {
            for (int i = 0; i < 14; i++) {
                Vec3d p = cur.add(Particles.randDir(w.random).multiply(size * w.random.nextDouble()));
                Particles.add(ParticleTypes.FLAME, p, dir.multiply(-0.1));
            }
            for (int i = 0; i < 3; i++) Particles.add(ParticleTypes.LAVA, cur, Vec3d.ZERO);
            for (int i = 0; i < 5; i++) Particles.add(ParticleTypes.LARGE_SMOKE, cur.subtract(dir.multiply(size)), dir.multiply(-0.05));
            Particles.add(ParticleTypes.DRIPPING_LAVA, cur.add(Particles.randDir(w.random).multiply(size * 0.8)), Vec3d.ZERO);
        } else {
            for (int i = 0; i < 3; i++) Particles.add(ParticleTypes.SNOWFLAKE, cur, Particles.randDir(w.random).multiply(0.03));
            Particles.add(ParticleTypes.ITEM_SNOWBALL, cur.subtract(dir.multiply(0.6)), Vec3d.ZERO);
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (dead) return;
        Vec3d p = prev.lerp(cur, td);
        Matrix4f m = ms.peek().getPositionMatrix();
        if (kind == Vfx.P_FIRE_FIST) {
            double pulse = 1 + 0.08 * Math.sin((age + td) * 2.3);
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
            Gfx.sphere(vc, m, p, size * 1.5 * pulse, Gfx.argb(0xFF4A00, 0.22f), 8, 12);
            Gfx.sphere(vc, m, p, size * pulse, Gfx.argb(0xFF8A1A, 0.55f), 8, 12);
            Gfx.sphere(vc, m, p, size * 0.55, Gfx.argb(0xFFE07A, 0.8f), 6, 10);
            Gfx.ribbon(vc, m, p, p.subtract(dir.multiply(size * 3.5)), size * 1.6, Gfx.argb(0xFF6A00, 0.35f));
        } else if (kind == Vfx.P_MAGMA) {
            double t = age + td;
            VertexConsumer solid = vcp.getBuffer(RenderLayer.getDebugQuads());
            Gfx.shadedSphere(solid, m, p, size * 0.8, 0x7A1A08, 1f, 8, 12, new Vec3d(Math.cos(t * 0.3), 1, Math.sin(t * 0.3)));
            VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
            Gfx.sphere(glow, m, p, size * 0.95, Gfx.argb(0xFF4A00, 0.5f), 8, 12);
            Gfx.sphere(glow, m, p, size * 1.45, Gfx.argb(0xFF2200, 0.18f), 8, 12);
            Gfx.ribbon(glow, m, p, p.subtract(dir.multiply(size * 4)), size * 1.5, Gfx.argb(0xFF3A00, 0.3f));
        } else {
            Vec3d base = p.subtract(dir.multiply(0.9));
            VertexConsumer solid = vcp.getBuffer(RenderLayer.getDebugQuads());
            Gfx.crystal(solid, m, base, dir, 1.9, 0.17, Gfx.argb(0xBDF0FF, 0.75f));
            VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
            Gfx.crystal(glow, m, base, dir, 2.0, 0.24, Gfx.argb(0x7FD8FF, 0.25f));
            Gfx.ribbon(glow, m, base, base.subtract(dir.multiply(1.6)), 0.25, Gfx.argb(0xCFF6FF, 0.25f));
        }
    }
}
