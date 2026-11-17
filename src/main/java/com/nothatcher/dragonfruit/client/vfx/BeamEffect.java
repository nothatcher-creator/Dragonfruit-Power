package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Glowing beam: wide soft glow, coloured body, white-hot core. Fades and thins out. */
public class BeamEffect extends VfxEffect {
    private final Vec3d from, to;
    private final int color;
    private final float width;
    private final int style;

    public BeamEffect(Vec3d from, Vec3d to, int color, float width, int life, int style) {
        super(life);
        this.from = from;
        this.to = to;
        this.color = color;
        this.width = width;
        this.style = style;
    }

    @Override
    protected void onTick(ClientWorld world) {
        if (age != 0) return;
        double len = from.distanceTo(to);
        Vec3d d = to.subtract(from).multiply(1.0 / Math.max(1.0E-4, len));
        for (double l = 0; l < len; l += 1.2) {
            Vec3d p = from.add(d.multiply(l));
            switch (style) {
                case Vfx.S_LIGHT -> {
                    Particles.add(ParticleTypes.END_ROD, p, Particles.randDir(world.random).multiply(0.03));
                    if (world.random.nextInt(3) == 0) Particles.add(ParticleTypes.ELECTRIC_SPARK, p, Particles.randDir(world.random).multiply(0.2));
                }
                case Vfx.S_FROST -> Particles.add(ParticleTypes.SNOWFLAKE, p, Particles.randDir(world.random).multiply(0.03));
                case Vfx.S_FLAME -> Particles.add(ParticleTypes.FLAME, p, Vec3d.ZERO);
                default -> { }
            }
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        double p = progress(td);
        double fade = Math.pow(1 - p, 1.4);
        double pulse = age == 0 ? 1.5 : 1.0;
        double w = width * pulse * (0.35 + 0.65 * fade);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Gfx.ribbon(vc, m, from, to, w * 2.4, Gfx.argb(color, (float) (0.22 * fade)));
        Gfx.ribbon(vc, m, from, to, w, Gfx.argb(color, (float) (0.75 * fade)));
        Gfx.ribbon(vc, m, from, to, w * 0.35, Gfx.argb(0xFFFFFF, (float) (0.95 * fade)));
    }
}
