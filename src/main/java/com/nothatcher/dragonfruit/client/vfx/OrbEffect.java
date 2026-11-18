package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Glowing orb. "charging" orbs grow in the hand and suck particles inward (Light Cannon, Quake Bubble);
 * otherwise it's an expanding fireball-style glow that fades.
 */
public class OrbEffect extends VfxEffect {
    private final Vec3d center;
    private final float radius;
    private final int color;
    private final int attachId, attachMode;
    private final boolean charging;
    private final int style;

    public OrbEffect(Vec3d center, float radius, int color, int life, int attachId, int attachMode, boolean charging, int style) {
        super(life);
        this.center = center;
        this.radius = radius;
        this.color = color;
        this.attachId = attachId;
        this.attachMode = attachMode;
        this.charging = charging;
        this.style = style;
    }

    boolean isHandChargeOf(int entityId) {
        return charging && attachMode == Vfx.ATTACH_HAND && attachId == entityId;
    }

    private Vec3d pos(float td) {
        if (attachMode == Vfx.ATTACH_NONE || mc().world == null) return center;
        Entity e = mc().world.getEntityById(attachId);
        if (e == null) return center;
        if (attachMode == Vfx.ATTACH_CENTER) return e.getLerpedPos(td).add(0, e.getHeight() * 0.5, 0);
        return Poses.handFront(e, td);
    }

    private double size(double p) {
        if (charging) return radius * (0.15 + 0.85 * Math.pow(p, 0.7));
        return radius * (0.6 + 0.6 * Gfx.easeOutCubic(p));
    }

    @Override
    protected void onTick(ClientWorld world) {
        Vec3d c = pos(1f);
        double s = size((double) age / life);
        ParticleEffect fx = switch (style) {
            case Vfx.S_FLAME -> ParticleTypes.FLAME;
            case Vfx.S_FROST -> ParticleTypes.SNOWFLAKE;
            case Vfx.S_LIGHT -> ParticleTypes.END_ROD;
            case Vfx.S_QUAKE -> ParticleTypes.CLOUD;
            case Vfx.S_THUNDER -> ParticleTypes.ELECTRIC_SPARK;
            case Vfx.S_GRAVITY -> ParticleTypes.REVERSE_PORTAL;
            default -> null;
        };
        if (fx == null) return;
        if (charging) {
            for (int i = 0; i < 4; i++) {
                Vec3d d = Particles.randDir(world.random);
                Vec3d from = c.add(d.multiply(s * 4 + 1));
                Particles.add(fx, from, d.multiply(-0.18));
            }
            if (style == Vfx.S_QUAKE && age % 4 == 0) Particles.add(ParticleTypes.ELECTRIC_SPARK, c, Particles.randDir(world.random).multiply(0.3));
        } else if (age < 3) {
            for (int i = 0; i < 10; i++) Particles.add(fx, c, Particles.randDir(world.random).multiply(0.3));
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double p = progress(td);
        Vec3d c = pos(td);
        double flicker = 1 + 0.06 * Math.sin((age + td) * 1.9);
        double s = size(p) * flicker;
        double alpha = charging ? 0.55 + 0.35 * p : Math.pow(1 - p, 1.3);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Gfx.sphere(vc, m, c, s * 1.7, Gfx.argb(color, (float) (0.16 * alpha)), 8, 12);
        Gfx.sphere(vc, m, c, s, Gfx.argb(color, (float) (0.45 * alpha)), 8, 12);
        Gfx.sphere(vc, m, c, s * 0.45, Gfx.argb(0xFFFFFF, (float) (0.85 * alpha)), 6, 10);
    }
}
