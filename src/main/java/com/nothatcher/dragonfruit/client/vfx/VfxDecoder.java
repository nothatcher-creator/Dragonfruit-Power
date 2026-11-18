package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.client.CameraFx;
import com.nothatcher.dragonfruit.net.Vfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

/**
 * Reads a VFX packet on the network thread and returns work to run on the render thread.
 * Field order must match net/Vfx.java.
 */
public final class VfxDecoder {
    private VfxDecoder() {}

    private static Vec3d vec(PacketByteBuf b) {
        return new Vec3d(b.readDouble(), b.readDouble(), b.readDouble());
    }

    public static Runnable decode(PacketByteBuf b) {
        int type = b.readByte();
        switch (type) {
            case Vfx.ARM -> {
                int owner = b.readVarInt();
                int side = b.readByte();
                Vec3d target = vec(b);
                int ext = b.readByte(), hold = b.readByte(), ret = b.readByte();
                float thick = b.readFloat();
                return () -> VfxManager.add(new ArmEffect(owner, side, target, ext, hold, ret, thick, 1f, 0, 0));
            }
            case Vfx.GATLING -> {
                int owner = b.readVarInt();
                int duration = b.readVarInt();
                long seed = b.readLong();
                float range = b.readFloat();
                return () -> VfxManager.add(new GatlingEffect(owner, duration, seed, range));
            }
            case Vfx.BEAM -> {
                Vec3d from = vec(b), to = vec(b);
                int color = b.readInt();
                float width = b.readFloat();
                int life = b.readVarInt();
                int style = b.readByte();
                return () -> VfxManager.add(new BeamEffect(from, to, color, width, life, style));
            }
            case Vfx.RING -> {
                Vec3d c = vec(b), n = vec(b);
                float r = b.readFloat();
                int color = b.readInt();
                int life = b.readVarInt();
                int style = b.readByte();
                return () -> VfxManager.add(new RingEffect(c, n, r, color, life, style));
            }
            case Vfx.ORB -> {
                Vec3d c = vec(b);
                float r = b.readFloat();
                int color = b.readInt();
                int life = b.readVarInt();
                int attach = b.readVarInt();
                int mode = b.readByte();
                boolean charging = b.readBoolean();
                int style = b.readByte();
                return () -> VfxManager.add(new OrbEffect(c, r, color, life, attach, mode, charging, style));
            }
            case Vfx.SPIKES -> {
                Vec3d c = vec(b);
                float r = b.readFloat();
                int count = b.readVarInt();
                int life = b.readVarInt();
                long seed = b.readLong();
                return () -> VfxManager.add(new SpikesEffect(c, r, count, life, seed));
            }
            case Vfx.ICE_COFFIN -> {
                int id = b.readVarInt();
                int life = b.readVarInt();
                return () -> VfxManager.add(new IceCoffinEffect(id, life));
            }
            case Vfx.AIR_CRACK -> {
                Vec3d p = vec(b), n = vec(b);
                float size = b.readFloat();
                int life = b.readVarInt();
                long seed = b.readLong();
                return () -> VfxManager.add(new AirCrackEffect(p, n, size, life, seed));
            }
            case Vfx.PROJECTILE -> {
                int owner = b.readVarInt();
                int kind = b.readByte();
                Vec3d start = vec(b), dir = vec(b);
                float speed = b.readFloat();
                int life = b.readVarInt();
                float size = b.readFloat();
                return () -> VfxManager.add(new ProjectileEffect(owner, kind, start, dir, speed, life, size));
            }
            case Vfx.FIREFLIES -> {
                int owner = b.readVarInt();
                int n = b.readVarInt();
                int[] ids = new int[Math.min(n, 64)];
                for (int i = 0; i < n; i++) {
                    int id = b.readVarInt();
                    if (i < ids.length) ids[i] = id;
                }
                int orbit = b.readVarInt(), fly = b.readVarInt();
                return () -> VfxManager.add(new FireflyEffect(owner, ids, orbit, fly));
            }
            case Vfx.SHAKE -> {
                Vec3d c = vec(b);
                float intensity = b.readFloat();
                int duration = b.readVarInt();
                float radius = b.readFloat();
                return () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null) return;
                    double d = mc.player.getPos().distanceTo(c);
                    double falloff = 1 - d / Math.max(1, radius);
                    if (falloff > 0) CameraFx.shake((float) (intensity * falloff), duration);
                };
            }
            case Vfx.FLASH -> {
                Vec3d c = vec(b);
                float intensity = b.readFloat();
                float radius = b.readFloat();
                return () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null) return;
                    double d = mc.player.getEyePos().distanceTo(c);
                    double falloff = 1 - d / Math.max(1, radius);
                    if (falloff > 0) CameraFx.flash((float) (intensity * falloff));
                };
            }
            case Vfx.AURA -> {
                int id = b.readVarInt();
                int kind = b.readByte();
                int duration = b.readVarInt();
                return () -> VfxManager.add(new AuraEffect(id, kind, duration));
            }
            case Vfx.BURST -> {
                Vec3d p = vec(b), d = vec(b);
                int kind = b.readByte();
                float scale = b.readFloat();
                return () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.world != null) Particles.burst(mc.world, p, d, kind, scale);
                };
            }
            case Vfx.FOV -> {
                float amount = b.readFloat();
                int duration = b.readVarInt();
                return () -> CameraFx.kickFov(amount, duration);
            }
            case Vfx.BOLT -> {
                Vec3d from = vec(b), to = vec(b);
                int color = b.readInt();
                float width = b.readFloat();
                int life = b.readVarInt();
                long seed = b.readLong();
                return () -> VfxManager.add(new BoltEffect(from, to, color, width, life, seed));
            }
            case Vfx.METEOR -> {
                Vec3d start = vec(b), end = vec(b);
                int fall = b.readVarInt();
                float radius = b.readFloat();
                return () -> VfxManager.add(new MeteorEffect(start, end, fall, radius));
            }
            case Vfx.IMPACT -> {
                Vec3d p = vec(b);
                float size = b.readFloat();
                int color = b.readInt();
                int life = b.readVarInt();
                return () -> VfxManager.add(new ImpactEffect(p, size, color, life));
            }
            case Vfx.PRESS -> {
                Vec3d c = vec(b);
                float r = b.readFloat();
                int color = b.readInt();
                int life = b.readVarInt();
                return () -> VfxManager.add(new PressEffect(c, r, color, life));
            }
            case Vfx.SLASH -> {
                Vec3d start = vec(b), dir = vec(b);
                float speed = b.readFloat();
                int life = b.readVarInt();
                float width = b.readFloat();
                float roll = b.readFloat();
                int color = b.readInt();
                return () -> VfxManager.add(new SlashEffect(start, dir, speed, life, width, roll, color));
            }
            case Vfx.SPEEDLINES -> {
                float intensity = b.readFloat();
                int duration = b.readVarInt();
                return () -> CameraFx.speedLines(intensity, duration);
            }
            default -> {
                return () -> { };
            }
        }
    }
}
