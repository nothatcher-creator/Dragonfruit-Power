package com.nothatcher.vehicleforge.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * Big, slow, billowing smoke: large puffs that rise, drift with the wind, swell and thin out over ~15 seconds.
 * Dark for fires, white for steam.
 */
public class SmokeParticle extends SpriteBillboardParticle {
    private final float grow;

    SmokeParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites, boolean dark) {
        super(world, x, y, z);
        setSprite(sprites);
        scale(dark ? 3.2f + random.nextFloat() * 1.6f : 2.2f + random.nextFloat());
        setBoundingBoxSpacing(0.25f, 0.25f);
        this.maxAge = dark ? 260 + random.nextInt(120) : 70 + random.nextInt(40);
        this.gravityStrength = 3.0e-6f;
        this.velocityX = vx;
        this.velocityY = vy + random.nextFloat() / 500f;
        this.velocityZ = vz;
        this.grow = dark ? 1.004f : 1.012f;
        float g = dark ? 0.12f + random.nextFloat() * 0.1f : 0.88f + random.nextFloat() * 0.1f;
        setColor(g, g, g * (dark ? 0.98f : 1f));
        setAlpha(dark ? 0.92f : 0.6f);
    }

    @Override
    public void tick() {
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        if (age++ >= maxAge || alpha <= 0) {
            markDead();
            return;
        }
        // wander, with a steady breeze carrying the column east
        velocityX += random.nextFloat() / 5000f * (random.nextBoolean() ? 1 : -1) + 0.00012;
        velocityZ += random.nextFloat() / 5000f * (random.nextBoolean() ? 1 : -1);
        velocityY -= gravityStrength;
        move(velocityX, velocityY, velocityZ);
        scale *= grow;
        if (age >= maxAge - 60 && alpha > 0.01f) alpha -= 0.015f;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;
        private final boolean dark;

        public Factory(SpriteProvider sprites, boolean dark) {
            this.sprites = sprites;
            this.dark = dark;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            return new SmokeParticle(world, x, y, z, vx, vy, vz, sprites, dark);
        }
    }
}
