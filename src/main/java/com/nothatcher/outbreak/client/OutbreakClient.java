package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.GunItem;
import com.nothatcher.outbreak.Outbreak;
import com.nothatcher.outbreak.OutbreakBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class OutbreakClient implements ClientModInitializer {
    private static final Identifier SCOPE = new Identifier("textures/misc/spyglass_scope.png");
    public static KeyBinding RELOAD, SHOVE, ACTIONS, FOG;
    public static boolean fogOn = true;
    /** entities further than this from the camera are lost in the fog, so they aren't drawn (0 = fog off, draw all) */
    public static volatile float cullDist = 0f;
    private static boolean ctrlWasDown;
    // timed action progress, from the server
    private static long progressStart, progressEnd;
    private static Text progressLabel = Text.empty();

    @Override
    public void onInitializeClient() {
        // the built-in "Zomboid Look" pack (on by default; Resource Packs can switch it off) and its colour grade
        net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("outbreak").ifPresent(mod ->
                net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                        new net.minecraft.util.Identifier("outbreak", "zomboid_look"), mod,
                        net.minecraft.text.Text.translatable("pack.outbreak.zomboid_look"),
                        net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED));
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                    @Override
                    public net.minecraft.util.Identifier getFabricId() {
                        return new net.minecraft.util.Identifier("outbreak", "zomboid_grade");
                    }

                    @Override
                    public void reload(net.minecraft.resource.ResourceManager manager) {
                        ZomboidGrade.reload();
                    }
                });
        RELOAD = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.outbreak.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.outbreak"));
        SHOVE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.outbreak.shove", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.outbreak"));
        ACTIONS = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.outbreak.actions", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.outbreak"));
        FOG = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.outbreak.fog", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.outbreak"));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(net.minecraft.entity.EntityType.ZOMBIE, CivilianZombieRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.nothatcher.outbreak.OutbreakItems.CORPSE, CorpseRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.nothatcher.outbreak.OutbreakItems.THROWN,
                net.minecraft.client.render.entity.FlyingItemEntityRenderer::new);
        for (Block b : OutbreakBlocks.ALL) BlockRenderLayerMap.INSTANCE.putBlock(b, RenderLayer.getCutout());
        for (Block b : OutbreakBlocks.BIG_DECALS) BlockRenderLayerMap.INSTANCE.putBlock(b, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(OutbreakBlocks.BARRICADE, RenderLayer.getCutout());
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((st, world, pos, tint) -> 0x3F76E4, OutbreakBlocks.RAIN_BARREL);
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(OutbreakBlocks.BARRICADE_BE, ctx -> new BarricadeRenderer());

        ClientTickEvents.END_CLIENT_TICK.register(OutbreakHud::tick);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (RELOAD.wasPressed()) {
                if (mc.player != null && mc.player.getMainHandStack().getItem() instanceof GunItem)
                    ClientPlayNetworking.send(Outbreak.RELOAD, PacketByteBufs.empty());
            }
            while (SHOVE.wasPressed()) {
                if (mc.player != null && !mc.player.hasVehicle()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    ClientPlayNetworking.send(Outbreak.SHOVE, PacketByteBufs.empty());
                }
            }
            while (FOG.wasPressed()) {
                fogOn = !fogOn;
                if (mc.player != null) mc.player.sendMessage(Text.translatable(fogOn ? "msg.outbreak.fog_on" : "msg.outbreak.fog_off"), true);
            }
            while (ACTIONS.wasPressed()) {
                if (mc.currentScreen == null) ActionMenuScreen.open(mc);
            }
            // Ctrl opens the menu too, as long as you're standing still (Ctrl + walking is still sprint)
            long win = mc.getWindow().getHandle();
            boolean ctrl = InputUtil.isKeyPressed(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(win, GLFW.GLFW_KEY_RIGHT_CONTROL);
            if (ctrl && !ctrlWasDown && mc.currentScreen == null && mc.player != null
                    && mc.player.input.movementForward == 0 && mc.player.input.movementSideways == 0) {
                ActionMenuScreen.open(mc);
            }
            ctrlWasDown = ctrl;
        });
        ClientPlayNetworking.registerGlobalReceiver(Outbreak.PROGRESS, (client, handler, buf, sender) -> {
            int ticks = buf.readVarInt();
            String key = buf.readString(), arg = buf.readString();
            client.execute(() -> {
                if (client.world == null) return;
                long now = client.world.getTime();
                progressStart = now;
                progressEnd = ticks <= 0 ? 0 : now + ticks;
                progressLabel = arg.isEmpty() ? Text.translatable(key) : Text.translatable(key, Text.translatable(arg));
            });
        });
        CharacterClient.register();
        SkillsClient.register();
        ClientTickEvents.END_CLIENT_TICK.register(CharacterClient::tick);
        HudRenderCallback.EVENT.register(OutbreakClient::hud);
        HudRenderCallback.EVENT.register(OutbreakHud::render);
    }

    public static boolean scoped(AbstractClientPlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return p == mc.player && p.isSneaking() && mc.options.getPerspective().isFirstPerson()
                && p.getMainHandStack().getItem() instanceof GunItem g && g.scope;
    }

    private static void hud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        // timed action
        if (progressEnd > 0) {
            float now = mc.world.getTime() + tickDelta;
            float f = Math.min(1f, (now - progressStart) / Math.max(1f, progressEnd - progressStart));
            if (now > progressEnd + 10) progressEnd = 0;
            int bw = 100, x = w / 2 - bw / 2, y = h / 2 + 14;
            ctx.fill(x - 1, y - 1, x + bw + 1, y + 6, 0xC0000000);
            ctx.fill(x, y, x + (int) (bw * f), y + 5, 0xFF7FBF4F);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, progressLabel, w / 2, y + 8, 0xFFE8E8E8);
        }
        ItemStack st = mc.player.getMainHandStack();
        if (!(st.getItem() instanceof GunItem gun)) return;
        if (scoped(mc.player)) {
            int size = Math.min(w, h);
            int x = (w - size) / 2, y = (h - size) / 2;
            ctx.drawTexture(SCOPE, x, y, -90, 0f, 0f, size, size, size, size);
            ctx.fill(0, 0, w, y, 0xFF000000);
            ctx.fill(0, y + size, w, h, 0xFF000000);
            ctx.fill(0, y, x, y + size, 0xFF000000);
            ctx.fill(x + size, y, w, y + size, 0xFF000000);
            ctx.fill(w / 2, y + size / 6, w / 2 + 1, y + size * 5 / 6, 0xCC000000);
            ctx.fill(x + size / 6, h / 2, x + size * 5 / 6, h / 2 + 1, 0xCC000000);
        }
        int mag = GunItem.ammo(st);
        int spare = count(mc.player, gun);
        String text = mag + " / " + gun.mag + "   |  " + spare;
        int tw = mc.textRenderer.getWidth(text);
        int x = w - tw - 8, y = h - 22;
        ctx.fill(x - 4, y - 3, x + tw + 4, y + 11, 0x88000000);
        ctx.drawText(mc.textRenderer, text, x, y, mag == 0 ? 0xFF5050 : 0xF0E0B0, true);
        if (mag == 0 && spare == 0) {
            String none = "NO AMMO";
            ctx.drawText(mc.textRenderer, none, w / 2 - mc.textRenderer.getWidth(none) / 2, h / 2 + 30, 0xFF5050, true);
        }
    }

    private static int count(PlayerEntity p, GunItem gun) {
        PlayerInventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(gun.ammoItem())) n += inv.getStack(i).getCount();
        return n;
    }
}
