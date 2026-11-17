package com.nothatcher.dragonfruit.client.vfx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

/** A client-side visual that lives for {@code life} ticks. */
public abstract class VfxEffect {
    protected int age;
    protected final int life;

    protected VfxEffect(int life) {
        this.life = Math.max(1, life);
    }

    /** @return false when finished */
    public boolean tick(ClientWorld world) {
        onTick(world);
        age++;
        return age < life;
    }

    protected void onTick(ClientWorld world) {}

    public void render(MatrixStack matrices, VertexConsumerProvider consumers, float tickDelta) {}

    /** 0..1 over the effect's life (smooth with tickDelta). */
    protected double progress(float tickDelta) {
        return Gfx.clamp01((age + tickDelta) / life);
    }

    protected static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }
}
