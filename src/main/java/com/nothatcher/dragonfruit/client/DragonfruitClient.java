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
    public static KeyBinding camKey;
    public static KeyBinding cannonKey;
    public static KeyBinding formKey;

    private static double lastVelY;
    private static boolean wasOnGround = true;

    @Override
    public void onInitializeClient() {
        CustomSkins.ensureFolder();
        useKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.use", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.dragonfruit"));
        nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.next", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.dragonfruit"));
        prevKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.prev", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.dragonfruit"));
        formKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.transform", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.dragonfruit"));
        cannonKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.cannons", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.dragonfruit"));
        camKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dragonfruit.camera", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.dragonfruit"));

        ClientPlayNetworking.registerGlobalReceiver(Net.STATE, (client, handler, buf, responseSender) -> {
            int fruit = buf.readVarInt();
            int n = buf.readVarInt();
            int[] cds = new int[n];
            for (int i = 0; i < n; i++) cds[i] = buf.readVarInt();
            int buff = buf.readVarInt();
            client.execute(() -> ClientPowerData.apply(fruit, cds, buff));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.FRUITS, (client, handler, buf, responseSender) -> {
            int n = buf.readVarInt();
            java.util.UUID[] ids = new java.util.UUID[n];
            int[] fruits = new int[n];
            for (int i = 0; i < n; i++) {
                ids[i] = buf.readUuid();
                fruits[i] = buf.readVarInt();
            }
            client.execute(() -> {
                for (int i = 0; i < n; i++) ClientPowerData.setOther(ids[i], FruitType.byOrdinal(fruits[i]));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.FORMS, (client, handler, buf, responseSender) -> {
            int n = buf.readVarInt();
            java.util.UUID[] ids = new java.util.UUID[n];
            boolean[] on = new boolean[n];
            for (int i = 0; i < n; i++) {
                ids[i] = buf.readUuid();
                on[i] = buf.readBoolean();
            }
            client.execute(() -> {
                for (int i = 0; i < n; i++) ClientPowerData.setForm(ids[i], on[i]);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.VFX, (client, handler, buf, responseSender) -> {
            Runnable work = VfxDecoder.decode(buf);
            client.execute(work);
        });

        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.nothatcher.dragonfruit.content.ModContent.SHIP, com.nothatcher.dragonfruit.client.render.ShipRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
                com.nothatcher.dragonfruit.content.ModContent.SEA_KING, com.nothatcher.dragonfruit.client.render.SeaKingRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, helper, context) -> {
                    if (entityRenderer instanceof net.minecraft.client.render.entity.PlayerEntityRenderer playerRenderer) {
                        helper.register(new com.nothatcher.dragonfruit.client.render.FruitAuraFeature(playerRenderer));
                    }
                });

        ClientTickEvents.END_CLIENT_TICK.register(DragonfruitClient::tick);
        HudRenderCallback.EVENT.register(Hud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(VfxManager::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientPowerData.reset();
            VfxManager.clear();
            CameraFx.reset();
            PowerCam.reset();
            CustomSkins.clear();
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

        while (camKey.wasPressed()) PowerCam.cycle();
        while (nextKey.wasPressed()) ClientPowerData.cycle(1);
        while (prevKey.wasPressed()) ClientPowerData.cycle(-1);
        while (formKey.wasPressed()) ClientPlayNetworking.send(Net.TOGGLE_FORM, PacketByteBufs.create());
        while (cannonKey.wasPressed()) {
            if (player.getVehicle() instanceof com.nothatcher.dragonfruit.content.ShipEntity) {
                ClientPlayNetworking.send(Net.FIRE_CANNONS, PacketByteBufs.create());
            }
        }
        while (useKey.wasPressed()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(ClientPowerData.selected);
            ClientPlayNetworking.send(Net.USE_ABILITY, buf);
        }

        FormFx.tick(client);

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
