package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Body aura: Second Gear steam, Flame Body fire, etc. Lighter in your own first-person view. */
public class AuraEffect extends VfxEffect {
    private static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(1f, 0.45f, 0.55f), 1.1f);
    private final int entityId;
    private final int kind;

    public AuraEffect(int entityId, int kind, int duration) {
        super(duration);
        this.entityId = entityId;
        this.kind = kind;
    }

    @Override
    protected void onTick(ClientWorld world) {
        Entity e = world.getEntityById(entityId);
        if (e == null) return;
        Random r = world.random;
        boolean fp = Poses.isFirstPersonOwner(e);
        int n = fp ? 1 : 3;
        for (int i = 0; i < n; i++) {
            double h = fp ? r.nextDouble() * e.getHeight() * 0.5 : r.nextDouble() * e.getHeight();
            Vec3d p = e.getPos().add((r.nextDouble() - 0.5) * e.getWidth() * 1.4, h, (r.nextDouble() - 0.5) * e.getWidth() * 1.4);
            switch (kind) {
                case Vfx.A_STEAM -> {
                    Particles.add(ParticleTypes.CLOUD, p, new Vec3d(0, 0.06 + r.nextDouble() * 0.08, 0));
                    if (r.nextInt(3) == 0) Particles.add(PINK, p, Vec3d.ZERO);
                }
                case Vfx.A_FLAME -> {
                    Particles.add(ParticleTypes.FLAME, p, new Vec3d(0, 0.04 + r.nextDouble() * 0.06, 0));
                    if (r.nextInt(4) == 0) Particles.add(ParticleTypes.SMALL_FLAME, p, Vec3d.ZERO);
                    if (r.nextInt(25) == 0) Particles.add(ParticleTypes.LAVA, p, Vec3d.ZERO);
                }
                case Vfx.A_FROST -> Particles.add(ParticleTypes.SNOWFLAKE, p, Vec3d.ZERO);
                case Vfx.A_LIGHT -> Particles.add(ParticleTypes.END_ROD, p, Vec3d.ZERO);
                default -> { }
            }
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (kind != Vfx.A_FLAME || mc().world == null) return;
        Entity e = mc().world.getEntityById(entityId);
        if (e == null || Poses.isFirstPersonOwner(e)) return;
        double t = age + td;
        Vec3d c = e.getLerpedPos(td).add(0, e.getHeight() * 0.5, 0);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        double s = e.getHeight() * 0.62 * (1 + 0.05 * Math.sin(t * 1.7));
        Gfx.sphere(vc, m, c, s, Gfx.argb(0xFF6A00, 0.13f), 8, 12);
    }
}
