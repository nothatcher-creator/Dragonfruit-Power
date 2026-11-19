package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.GunItem;
import com.nothatcher.outbreak.Outbreak;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class OutbreakClient implements ClientModInitializer {
    private static final Identifier SCOPE = new Identifier("textures/misc/spyglass_scope.png");
    public static KeyBinding RELOAD;

    @Override
    public void onInitializeClient() {
        RELOAD = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.outbreak.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.outbreak"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (RELOAD.wasPressed()) {
                if (mc.player != null && mc.player.getMainHandStack().getItem() instanceof GunItem)
                    ClientPlayNetworking.send(Outbreak.RELOAD, PacketByteBufs.empty());
            }
        });
        HudRenderCallback.EVENT.register(OutbreakClient::hud);
    }

    public static boolean scoped(AbstractClientPlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return p == mc.player && p.isSneaking() && mc.options.getPerspective().isFirstPerson()
                && p.getMainHandStack().getItem() instanceof GunItem g && g.scope;
    }

    private static void hud(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        ItemStack st = mc.player.getMainHandStack();
        if (!(st.getItem() instanceof GunItem gun)) return;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
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
            ctx.drawText(mc.textRenderer, none, w / 2 - mc.textRenderer.getWidth(none) / 2, h / 2 + 12, 0xFF5050, true);
        }
    }

    private static int count(PlayerEntity p, GunItem gun) {
        PlayerInventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(gun.ammoItem())) n += inv.getStack(i).getCount();
        return n;
    }
}
