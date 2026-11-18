package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Jagged lightning with forks. Re-jags every 2 ticks and flickers out like real lightning. */
public class BoltEffect extends VfxEffect {
    private record Seg(Vec3d a, Vec3d b, double width) {}

    private final Vec3d from, to;
    private final int color;
    private final float width;
    private final Random random;
    private List<Seg> segs = new ArrayList<>();
    private boolean visibleThisTick = true;

    public BoltEffect(Vec3d from, Vec3d to, int color, float width, int life, long seed) {
        super(life);
        this.from = from;
        this.to = to;
        this.color = color;
        this.width = width;
        this.random = new Random(seed);
        rebuild();
    }

    private void rebuild() {
        segs = new ArrayList<>();
        Vec3d axis = to.subtract(from);
        double len = axis.length();
        if (len < 0.01) return;
        Vec3d[] b = Gfx.basis(axis);
        int n = Math.max(6, (int) (len / 1.6));
        double amp = Math.min(2.2, len * 0.05);
        Vec3d prev = from;
        for (int i = 1; i <= n; i++) {
            double s = i / (double) n;
            Vec3d base = from.add(axis.multiply(s));
            Vec3d p = i == n ? to : base.add(b[0].multiply((random.nextDouble() - 0.5) * 2 * amp))
                    .add(b[1].multiply((random.nextDouble() - 0.5) * 2 * amp));
            segs.add(new Seg(prev, p, width * (1.0 - s * 0.35)));
            // forks
            if (i < n - 1 && random.nextInt(4) == 0) {
                Vec3d fp = p;
                Vec3d dir = axis.normalize().add(b[0].multiply((random.nextDouble() - 0.5) * 1.6))
                        .add(b[1].multiply((random.nextDouble() - 0.5) * 1.6)).normalize();
                double flen = len * (0.04 + random.nextDouble() * 0.06);
                for (int k = 0; k < 4; k++) {
                    Vec3d nxt = fp.add(dir.multiply(flen)).add(b[0].multiply((random.nextDouble() - 0.5) * amp * 0.6));
                    segs.add(new Seg(fp, nxt, width * 0.4 * (1 - k * 0.2)));
                    fp = nxt;
                }
            }
            prev = p;
        }
    }

    @Override
    protected void onTick(ClientWorld world) {
        if (age % 2 == 0) rebuild();
        visibleThisTick = age < 3 || random.nextInt(3) != 0;
        if (age == 0) {
            for (int i = 0; i < 25; i++) Particles.add(ParticleTypes.ELECTRIC_SPARK, to, Particles.randDir(world.random).multiply(0.6));
            for (int i = 0; i < 10; i++) Particles.add(ParticleTypes.END_ROD, to, Particles.randDir(world.random).multiply(0.15));
        }
        if (random.nextInt(2) == 0 && !segs.isEmpty()) {
            Seg s = segs.get(random.nextInt(segs.size()));
            Particles.add(ParticleTypes.ELECTRIC_SPARK, s.b(), Particles.randDir(world.random).multiply(0.2));
        }
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, float td) {
        if (!visibleThisTick) return;
        double fade = 1 - Math.pow(progress(td), 2);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        for (Seg s : segs) {
            Gfx.ribbon(vc, m, s.a(), s.b(), s.width() * 5, Gfx.argb(color, (float) (0.16 * fade)));
            Gfx.ribbon(vc, m, s.a(), s.b(), s.width() * 1.8, Gfx.argb(color, (float) (0.6 * fade)));
            Gfx.ribbon(vc, m, s.a(), s.b(), s.width() * 0.6, Gfx.argb(0xFFFFFF, (float) (0.95 * fade)));
        }
    }
}
