package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.FruitType;
import com.nothatcher.dragonfruit.client.vfx.Particles;
import com.nothatcher.dragonfruit.client.vfx.VfxDecoder;
import com.nothatcher.dragonfruit.client.vfx.VfxManager;
import com.nothatcher.dragonfruit.net.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class DragonfruitClient implements ClientModInitializer {
    public static KeyBinding useKey;
    public static KeyBinding nextKey;
    public static KeyBinding prevKey;

    private static double lastVelY;
    private static boolean wasOnGround = true;

    @Override
    public void onInitializeClient() {
        useKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.use", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.dragonfruit"));
        nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.dragonfruit"));
        prevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.prev", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.dragonfruit"));

        ClientPlayNetworking.registerGlobalReceiver(Net.STATE, (client, handler, buf, responseSender) -> {
            int fruit = buf.readVarInt();
            int n = buf.readVarInt();
            int[] cds = new int[n];
            for (int i = 0; i < n; i++) cds[i] = buf.readVarInt();
            int buff = buf.readVarInt();
            client.execute(() -> ClientPowerData.apply(fruit, cds, buff));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.VFX, (client, handler, buf, responseSender) -> {
            Runnable work = VfxDecoder.decode(buf);
            client.execute(work);
        });

        ClientTickEvents.END_CLIENT_TICK.register(DragonfruitClient::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(VfxManager::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientPowerData.reset();
            VfxManager.clear();
            CameraFx.reset();
        }));
    }

    public static String useKeyName() {
        return useKey == null ? "R" : useKey.getBoundKeyLocalizedText().getString().toUpperCase();
    }

    private static void tick(MinecraftClient client) {
        CameraFx.tick();
        ClientPowerData.tick();
        VfxManager.tick(client.world);
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        while (nextKey.wasPressed()) ClientPowerData.cycle(1);
        while (prevKey.wasPressed()) ClientPowerData.cycle(-1);
        while (useKey.wasPressed()) {
            if (ClientPowerData.fruit == FruitType.NONE) {
                player.sendMessage(Text.translatable("msg.dragonfruit.no_fruit").formatted(Formatting.GRAY), true);
                continue;
            }
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(ClientPowerData.selected);
            ClientPlayNetworking.send(Net.USE_ABILITY, buf);
        }

        FruitType fruit = ClientPowerData.fruit;
        if (fruit == FruitType.NONE || player.isSpectator()) {
            wasOnGround = player.isOnGround();
            lastVelY = player.getVelocity().y;
            return;
        }

        // Fruit users sink like a stone.
        if (player.isTouchingWater() && !player.isCreative() && !player.hasVehicle()) {
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x * 0.6, Math.min(v.y, -0.09), v.z * 0.6);
        }

        // Rubber body: bounce instead of splatting.
        boolean onGround = player.isOnGround();
        if (fruit == FruitType.RUBBER && onGround && !wasOnGround && lastVelY < -0.65 && !player.isSneaking()
                && !player.isTouchingWater()) {
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x, Math.min(1.5, -lastVelY * 0.72), v.z);
            client.world.playSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_SLIME_JUMP,
                    SoundCategory.PLAYERS, 1f, 0.7f, false);
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                Particles.add(ParticleTypes.POOF, player.getPos(), new Vec3d(Math.cos(a) * 0.2, 0.02, Math.sin(a) * 0.2));
            }
            CameraFx.kickFov(-0.08f);
        }
        wasOnGround = onGround;
        lastVelY = player.getVelocity().y;
    }
}
