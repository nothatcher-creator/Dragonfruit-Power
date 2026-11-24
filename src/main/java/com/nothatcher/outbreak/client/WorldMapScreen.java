package com.nothatcher.outbreak.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** The full-screen map: drag to pan, scroll or the +/- buttons to zoom. You're the red arrow, your safehouse is green. */
public class WorldMapScreen extends Screen {
    private double cx, cz;            // world coordinates at the centre of the screen
    private double zoom = 1;          // screen pixels per block
    private boolean followed;

    public WorldMapScreen() {
        super(Text.translatable("map.outbreak.title"));
    }

    @Override
    protected void init() {
        if (!followed && client != null && client.player != null) {
            cx = client.player.getX();
            cz = client.player.getZ();
            followed = true;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> zoom = Math.min(8, zoom * 1.5)).dimensions(width - 28, 30, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> zoom = Math.max(0.125, zoom / 1.5)).dimensions(width - 28, 54, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("map.outbreak.centre"), b -> {
            if (client != null && client.player != null) {
                cx = client.player.getX();
                cz = client.player.getZ();
            }
        }).dimensions(width - 68, 78, 60, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close()).dimensions(width / 2 - 40, height - 26, 80, 20).build());
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, 0xFF1E2022);
        // which 256x256-block regions are on screen
        double halfW = width / 2.0 / zoom, halfH = height / 2.0 / zoom;
        int rx0 = MathHelper.floor((cx - halfW) / 256), rx1 = MathHelper.floor((cx + halfW) / 256);
        int rz0 = MathHelper.floor((cz - halfH) / 256), rz1 = MathHelper.floor((cz + halfH) / 256);
        c.getMatrices().push();
        c.getMatrices().translate(width / 2.0, height / 2.0, 0);
        c.getMatrices().scale((float) zoom, (float) zoom, 1);
        c.getMatrices().translate(-cx, -cz, 0);
        for (int rx = rx0; rx <= rx1; rx++)
            for (int rz = rz0; rz <= rz1; rz++)
                if (WorldMap.anyIn(rx, rz)) c.drawTexture(WorldMap.tile(rx, rz), rx * 256, rz * 256, 0, 0, 256, 256, 256, 256);
        c.getMatrices().pop();
        // your safehouse
        if (SkillsClient.safehouse != null) {
            var b = SkillsClient.safehouse;
            int x0 = sx(b.getMinX()), z0 = sz(b.getMinZ()), x1 = sx(b.getMaxX() + 1), z1 = sz(b.getMaxZ() + 1);
            c.drawBorder(x0, z0, Math.max(2, x1 - x0), Math.max(2, z1 - z0), 0xFF40D040);
            c.drawTextWithShadow(textRenderer, Text.translatable("map.outbreak.safehouse"), x0, z0 - 10, 0x60E060);
        }
        // you, pointing the way you face
        if (client != null && client.player != null) {
            int px = sx(client.player.getX()), pz = sz(client.player.getZ());
            float yaw = (float) Math.toRadians(client.player.getYaw());
            int tx = px + (int) (-Math.sin(yaw) * 7), tz = pz + (int) (Math.cos(yaw) * 7);
            for (int i = 0; i <= 6; i++) {
                int lx = px + (tx - px) * i / 6, lz = pz + (tz - pz) * i / 6;
                c.fill(lx - 1, lz - 1, lx + 1, lz + 1, 0xFFE02020);
            }
            c.fill(px - 3, pz - 3, px + 3, pz + 3, 0xFF000000);
            c.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFFE02020);
        }
        c.fill(0, 0, width, 22, 0xC0141618);
        c.drawCenteredTextWithShadow(textRenderer, title, width / 2, 7, 0xE8D6CC);
        String coords = (int) cx + ", " + (int) cz;
        c.drawTextWithShadow(textRenderer, coords, 6, 7, 0xA0A0A0);
        super.render(c, mouseX, mouseY, delta);
    }

    private int sx(double x) {
        return (int) Math.round(width / 2.0 + (x - cx) * zoom);
    }

    private int sz(double z) {
        return (int) Math.round(height / 2.0 + (z - cz) * zoom);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (super.mouseDragged(mx, my, button, dx, dy)) return true;
        cx -= dx / zoom;
        cz -= dy / zoom;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        zoom = MathHelper.clamp(zoom * (amount > 0 ? 1.25 : 0.8), 0.125, 8);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
