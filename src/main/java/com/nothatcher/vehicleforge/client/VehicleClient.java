package com.nothatcher.vehicleforge.client;

import com.nothatcher.vehicleforge.InputState;
import com.nothatcher.vehicleforge.ModRegistry;
import com.nothatcher.vehicleforge.Net;
import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleType;
import com.nothatcher.vehicleforge.VehicleType.Category;
import com.nothatcher.vehicleforge.client.render.VehicleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.lwjgl.glfw.GLFW;

public class VehicleClient implements ClientModInitializer {
    public static KeyBinding primaryKey, secondaryKey, downKey;

    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(com.nothatcher.vehicleforge.block.CityExtras.DARK_SMOKE,
                sprites -> new SmokeParticle.Factory(sprites, true));
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(com.nothatcher.vehicleforge.block.CityExtras.STEAM,
                sprites -> new SmokeParticle.Factory(sprites, false));
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(com.nothatcher.vehicleforge.block.CityExtras.POWER_BE, ctx -> new TurbineRenderer());
        EntityRendererRegistry.register(com.nothatcher.vehicleforge.block.CityExtras.SEAT, net.minecraft.client.render.entity.EmptyEntityRenderer::new);
        for (net.minecraft.block.Block b : com.nothatcher.vehicleforge.block.CityBlocks.CUTOUT)
            net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(b, net.minecraft.client.render.RenderLayer.getCutoutMipped());
        for (net.minecraft.block.Block b : com.nothatcher.vehicleforge.block.CityBlocks.TRANSLUCENT)
            net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(b, net.minecraft.client.render.RenderLayer.getTranslucent());
        for (EntityType<VehicleEntity> t : ModRegistry.ENTITY_TYPES.values()) {
            EntityRendererRegistry.register(t, VehicleRenderer::new);
        }
        primaryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.primary", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.vehicleforge"));
        secondaryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.secondary", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.vehicleforge"));
        downKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vehicleforge.down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.vehicleforge"));

        VehicleEntity.clientTickHook = VehicleClient::vehicleTick;
        VehicleEntity.crashHook = (v, impact) -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeFloat(impact);
            ClientPlayNetworking.send(Net.CRASH, buf);
        };
        ClientTickEvents.END_CLIENT_TICK.register(VehicleClient::tick);
        HudRenderCallback.EVENT.register(VehicleClient::hud);
    }

    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        boolean driving = p != null && p.getVehicle() instanceof VehicleEntity v && v.getControllingPassenger() == p;
        if (driving) {
            InputState.forward = p.input.pressingForward;
            InputState.back = p.input.pressingBack;
            InputState.left = p.input.pressingLeft;
            InputState.right = p.input.pressingRight;
            InputState.up = p.input.jumping;
            InputState.down = downKey.isPressed();
            InputState.vehicleId = p.getVehicle().getId();
        } else {
            InputState.vehicleId = -1;
            InputState.forward = InputState.back = InputState.left = InputState.right = InputState.up = InputState.down = false;
        }
        while (primaryKey.wasPressed()) {
            if (!driving) continue;
            VehicleEntity v = (VehicleEntity) p.getVehicle();
            if (v.type.weapon == VehicleType.Weapon.BOOST && !v.tryLocalBoost()) continue;
            send(0);
        }
        while (secondaryKey.wasPressed()) if (driving) send(1);
    }

    private static void send(int action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(action);
        ClientPlayNetworking.send(Net.ACTION, buf);
    }

    /** Engine sounds and exhaust / smoke / flame / spray particles. */
    private static void vehicleTick(VehicleEntity v) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        VehicleType t = v.type;
        if (!"none".equals(t.engineSound) && v.engineRunning()
                && (!(v.engineSound instanceof EngineSound es) || es.isDone())) {
            SoundEvent s = ModRegistry.sound(t.engineSound);
            if (s != null) {
                EngineSound sound = new EngineSound(v, s);
                v.engineSound = sound;
                mc.getSoundManager().play(sound);
            }
        }
        Random r = mc.world.random;
        boolean running = v.engineRunning();
        float speed = v.displaySpeed();
        effects(mc, v, r, speed);
        switch (t.category) {
            case CAR, TRUCK, MILITARY, RV -> {
                if (running && r.nextInt(speed > 0.3f ? 2 : 5) == 0) {
                    Vec3d at = v.toWorld(0.5, 0.35, t.rearZ - 0.1);
                    mc.world.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0.02, 0);
                }
            }
            case BIKE -> {
                if (running && !t.engineSound.equals("none") && r.nextInt(4) == 0) {
                    Vec3d at = v.toWorld(0.2, 0.4, -1.1);
                    mc.world.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0.01, 0);
                }
            }
            case TRAIN -> {
                if (t.smoke != null && (running || speed > 0.05f)) {
                    Vec3d at = v.toWorld(t.smoke.x, t.smoke.y, t.smoke.z);
                    mc.world.addParticle(speed > 0.4f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            true, at.x, at.y, at.z, 0, 0.12 + speed * 0.1, 0);
                }
            }
            case ROCKET -> {
                if (v.thrusting) {
                    for (int i = 0; i < 6; i++) {
                        Vec3d at = v.toWorld((r.nextDouble() - 0.5) * 1.2, -0.5 - r.nextDouble() * 2, (r.nextDouble() - 0.5) * 1.2);
                        mc.world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, -0.4, 0);
                    }
                    Vec3d at = v.toWorld(0, -3, 0);
                    mc.world.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, at.x, at.y, at.z, (r.nextDouble() - 0.5) * 0.3, 0.05, (r.nextDouble() - 0.5) * 0.3);
                    mc.world.addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, (r.nextDouble() - 0.5) * 0.6, -0.1, (r.nextDouble() - 0.5) * 0.6);
                }
            }
            case PLANE -> {
                if (v.thrusting && t.id.equals("jet")) {
                    Vec3d at = v.toWorld(0, 1.0, -6.0);
                    mc.world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, 0, 0);
                    mc.world.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0, 0);
                }
            }
            case HELICOPTER -> {
                if (running && r.nextInt(2) == 0) {
                    double ground = groundBelow(v, 6);
                    if (ground >= 0) {
                        double a = r.nextDouble() * Math.PI * 2;
                        mc.world.addParticle(ParticleTypes.CLOUD, v.getX() + Math.cos(a) * 3, v.getY() - ground + 0.2, v.getZ() + Math.sin(a) * 3,
                                Math.cos(a) * 0.3, 0.02, Math.sin(a) * 0.3);
                    }
                }
            }
            case BOAT -> {
                if (speed > 0.1f) {
                    for (int sd = -1; sd <= 1; sd += 2) {
                        Vec3d bow = v.toWorld(sd * 0.6, 0.1, t.width * 1.4);
                        mc.world.addParticle(ParticleTypes.SPLASH, bow.x, bow.y, bow.z, 0, 0.15, 0);
                    }
                    Vec3d wake = v.toWorld(0, 0.05, -t.width * 1.5);
                    mc.world.addParticle(ParticleTypes.BUBBLE_POP, wake.x, wake.y, wake.z, 0, 0, 0);
                    mc.world.addParticle(ParticleTypes.CLOUD, wake.x, wake.y, wake.z, 0, 0.01, 0);
                }
            }
            default -> { }
        }
    }

    private static final java.util.Map<Integer, Integer> SKID_COOLDOWN = new java.util.HashMap<>();

    /** Tyre smoke and skid sounds when sliding, engine smoke and fire when damaged, bubbles under water. */
    private static void effects(MinecraftClient mc, VehicleEntity v, Random r, float speed) {
        VehicleType t = v.type;
        if (t.wheeled() && v.isOnGround()) {
            boolean skidding = v.slipVis > 0.09f || (v.accelVis < -0.03f && speed > 0.35f);
            if (skidding) {
                for (int sd = -1; sd <= 1; sd += 2) {
                    if (t.category == Category.BIKE && sd > 0) continue;
                    Vec3d at = v.toWorld(t.category == Category.BIKE ? 0 : sd * t.width * 0.4, 0.15, t.axleR());
                    mc.world.addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, (r.nextDouble() - 0.5) * 0.05, 0.03, (r.nextDouble() - 0.5) * 0.05);
                    if (r.nextInt(3) == 0) mc.world.addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 0, 0.02, 0);
                }
                int key = v.getId();
                int until = SKID_COOLDOWN.getOrDefault(key, 0);
                if (v.age >= until || v.age < until - 40) {
                    SoundEvent skid = ModRegistry.sound("skid");
                    if (skid != null) mc.world.playSound(v.getX(), v.getY(), v.getZ(), skid, net.minecraft.sound.SoundCategory.NEUTRAL,
                            0.7f + Math.min(0.8f, speed), 0.85f + r.nextFloat() * 0.3f, false);
                    SKID_COOLDOWN.put(key, v.age + 14);
                }
            }
        }
        float hp = v.getHealth() / t.health;
        if (hp < 0.5f) {
            Vec3d at = t.flies() ? v.toWorld(0, t.height * 0.7, 0) : v.toWorld(0, t.height * 0.55, t.frontZ * 0.6);
            if (r.nextInt(hp < 0.25f ? 1 : 3) == 0) {
                mc.world.addParticle(hp < 0.25f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, true, at.x, at.y, at.z,
                        (r.nextDouble() - 0.5) * 0.04, 0.06, (r.nextDouble() - 0.5) * 0.04);
            }
            if (hp < 0.2f && r.nextInt(3) == 0) mc.world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, 0.03, 0);
        }
        // specials
        if (v.abilityOn()) {
            switch (t.weapon) {
                case TIME_JUMP -> {
                    Vec3d at = v.toWorld((r.nextDouble() - 0.5) * 1.8, 0.4 + r.nextDouble() * 0.6, t.rearZ + r.nextDouble());
                    mc.world.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0.02, 0);
                    mc.world.addParticle(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 0, 0, 0);
                }
                case BOOST -> {
                    Vec3d at = v.toWorld(0, 0.6, t.rearZ - 0.3);
                    for (int i = 0; i < 3; i++) mc.world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z,
                            -v.forward().x * 0.3 + (r.nextDouble() - 0.5) * 0.1, 0.02, -v.forward().z * 0.3 + (r.nextDouble() - 0.5) * 0.1);
                    mc.world.addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 0, 0.03, 0);
                }
                case MOWER -> {
                    if (speed > 0.02f && r.nextInt(2) == 0) {
                        Vec3d at = v.toWorld(-0.8, 0.2, 0);
                        mc.world.addParticle(ParticleTypes.COMPOSTER, at.x, at.y, at.z, 0, 0.05, 0);
                    }
                }
                default -> { }
            }
        }
        if (t.category == Category.BALLOON && v.thrusting) {
            Vec3d at = v.toWorld(0, 3.4, 0);
            mc.world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, (r.nextDouble() - 0.5) * 0.05, 0.15, (r.nextDouble() - 0.5) * 0.05);
        }
        if (t.category == Category.BALL && speed > 0.1f && r.nextInt(3) == 0) {
            mc.world.addParticle(ParticleTypes.POOF, v.getX(), v.getY() + 0.1, v.getZ(), 0, 0.01, 0);
        }
        if (t.category == Category.ANIMAL && speed > 0.4f && v.isOnGround() && r.nextInt(2) == 0) {
            mc.world.addParticle(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK,
                    v.getWorld().getBlockState(v.getBlockPos().down())), v.getX(), v.getY() + 0.1, v.getZ(), 0, 0.1, 0);
        }
        if (t.hover && v.engineRunning()) {
            Vec3d at = v.toWorld((r.nextDouble() - 0.5) * t.width, 0.05, (r.nextDouble() - 0.5) * 3);
            mc.world.addParticle(v.isTouchingWater() ? ParticleTypes.SPLASH : ParticleTypes.CLOUD, at.x, at.y, at.z, 0, 0.02, 0);
        }
        if (t.category == Category.SUBMARINE && v.isSubmergedInWater() && speed > 0.05f) {
            Vec3d at = v.toWorld(0, 1.3, t.rearZ);
            for (int i = 0; i < 3; i++) mc.world.addParticle(ParticleTypes.BUBBLE, at.x, at.y, at.z,
                    (r.nextDouble() - 0.5) * 0.2, 0.05, (r.nextDouble() - 0.5) * 0.2);
        }
    }

    /** distance to the ground below (within max) or -1 */
    private static double groundBelow(VehicleEntity v, int max) {
        for (int i = 0; i <= max; i++) {
            if (!v.getWorld().getBlockState(v.getBlockPos().down(i)).isAir()) return i;
        }
        return -1;
    }

    /** Speedometer, altitude, throttle and the controls for whatever you're driving. */
    private static void hud(DrawContext ctx, float td) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || !(mc.player.getVehicle() instanceof VehicleEntity v)) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        int kmh = Math.round(v.displaySpeed() * 20 * 3.6f);
        int x = w - 112, y = h - 70;
        ctx.fill(x - 4, y - 4, w - 4, h - 8, 0x99000000);
        ctx.drawTextWithShadow(tr, Text.literal(kmh + " km/h").styled(s -> s.withBold(true)), x, y, 0xFFFFFF);
        int line = y + 11;
        if (v.type.flies()) {
            ctx.drawTextWithShadow(tr, Text.translatable("hud.vehicleforge.altitude", (int) v.getY()), x, line, 0x9FE0FF);
            line += 10;
        }
        if (v.type.category == Category.PLANE) {
            ctx.drawTextWithShadow(tr, Text.translatable("hud.vehicleforge.throttle", Math.round(v.throttle * 100)), x, line, 0xFFD27A);
            line += 10;
        }
        if (v.type.wheeled() && v.type.gears > 0) {
            String g = v.gear < 0 ? "R" : !v.engineRunning() ? "N" : "D" + v.gear;
            ctx.drawTextWithShadow(tr, Text.literal(g), x + 74, y, 0xFFD27A);
            // tachometer
            float rpm = MathHelper.lerp(td, v.prevRpm, v.rpm);
            ctx.fill(x, line + 1, x + 100, line + 4, 0xFF333333);
            int rw = (int) (100 * MathHelper.clamp(rpm, 0, 1));
            ctx.fill(x, line + 1, x + rw, line + 4, rpm > 0.88f ? 0xFFFF5040 : 0xFFFFB040);
            line += 6;
        }
        float hp = v.getHealth() / v.type.health;
        ctx.drawTextWithShadow(tr, Text.literal("\u2764"), x - 1, line - 1, 0xFFAAAA);
        ctx.fill(x + 9, line + 1, x + 100, line + 4, 0xFF333333);
        ctx.fill(x + 9, line + 1, x + 9 + (int) (91 * hp), line + 4, hp > 0.5f ? 0xFF55DD55 : hp > 0.25f ? 0xFFDDDD55 : 0xFFDD5555);
        if (v.type.usesFuel()) {
            line += 7;
            float fuel = v.getFuel();
            ctx.drawTextWithShadow(tr, Text.literal("\u26FD"), x - 1, line - 1, 0xFFD27A);
            ctx.fill(x + 9, line + 1, x + 100, line + 4, 0xFF333333);
            ctx.fill(x + 9, line + 1, x + 9 + (int) (91 * fuel), line + 4, fuel > 0.25f ? 0xFFE0A030 : 0xFFDD3333);
        }
        if (v.getControllingPassenger() == mc.player) {
            String hint = Text.translatable("hud.vehicleforge.controls." + controlsKey(v.type),
                    primaryKey.getBoundKeyLocalizedText(), secondaryKey.getBoundKeyLocalizedText(), downKey.getBoundKeyLocalizedText()).getString();
            ctx.drawTextWithShadow(tr, Text.literal(hint), (w - tr.getWidth(hint)) / 2, 6, 0xE0E0E0);
        } else if (v.type.interiorRoom) {
            String hint = Text.translatable("hud.vehicleforge.rv").getString();
            ctx.drawTextWithShadow(tr, Text.literal(hint), (w - tr.getWidth(hint)) / 2, 6, 0xE0E0E0);
        }
    }

    private static String controlsKey(VehicleType t) {
        switch (t.weapon) {
            case TIME_JUMP: return "time";
            case SHRINK: return "shrink";
            case TRACTOR_BEAM: return "ufo";
            case BOOST: return "boost";
            case MOWER: return "mower";
            case FORKS: return "forklift";
            case COMPACTOR: return "garbage";
            case DIG: return "dig";
            case BLADE: return "blade";
            case PAVE: return "pave";
            case CRANE: return "crane";
            case TOW: return "tow";
            default: break;
        }
        if (t.category == Category.ANIMAL) return "animal";
        if (t.category == Category.BALLOON) return "balloon";
        if (t.siren) return t.weapon == VehicleType.Weapon.WATER_CANNON ? "siren" : "siren_horn";
        if (t.category == Category.SUBMARINE) return "sub";
        if (t.weapon != VehicleType.Weapon.NONE) return t.flies() ? "air_weapon" : "weapon";
        return switch (t.category) {
            case HELICOPTER -> "heli";
            case PLANE -> "plane";
            case ROCKET -> "rocket";
            case RV -> "rv_drive";
            default -> t.wheeled() ? "drive" : "basic";
        };
    }
}
