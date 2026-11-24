package com.nothatcher.vehicleforge.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/** A single fly: a speck that darts about in short jerky hops, circling the spot it was born at. */
public class FlyParticle extends SpriteBillboardParticle {
    private final double ox, oy, oz;

    FlyParticle(ClientWorld w, double x, double y, double z, SpriteProvider sprites) {
        super(w, x, y, z);
        setSprite(sprites);
        ox = x;
        oy = y;
        oz = z;
        scale(0.35f + random.nextFloat() * 0.2f);
        maxAge = 30 + random.nextInt(40);
        gravityStrength = 0;
        collidesWithWorld = false;
        velocityX = (random.nextDouble() - 0.5) * 0.08;
        velocityY = (random.nextDouble() - 0.5) * 0.04;
        velocityZ = (random.nextDouble() - 0.5) * 0.08;
        setColor(0.08f, 0.08f, 0.09f);
    }

    @Override
    public void tick() {
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
        if (age++ >= maxAge) {
            markDead();
            return;
        }
        if (random.nextInt(4) == 0) {                                       // dart off in a new direction
            velocityX = (random.nextDouble() - 0.5) * 0.12;
            velocityY = (random.nextDouble() - 0.5) * 0.06;
            velocityZ = (random.nextDouble() - 0.5) * 0.12;
        }
        velocityX += (ox - x) * 0.04;                                       // but keep circling home
        velocityY += (oy - y) * 0.04;
        velocityZ += (oz - z) * 0.04;
        move(velocityX, velocityY, velocityZ);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld w, double x, double y, double z, double vx, double vy, double vz) {
            return new FlyParticle(w, x, y, z, sprites);
        }
    }
}
