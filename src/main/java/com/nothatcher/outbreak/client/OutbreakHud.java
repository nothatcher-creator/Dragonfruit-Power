package com.nothatcher.outbreak.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nothatcher.outbreak.OutbreakEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Project Zomboid's HUD, Outbreak style:
 *  - a digital watch, top right: the time (24h), the temperature and the date (counting from 07/09, as in Zomboid);
 *  - moodles underneath: hungry, injured, queasy (infected), panic (zombies close), wet; four levels each, named for
 *    a few seconds when they appear or change (no hovering on a phone);
 *  - a minimap, bottom right, in Zomboid's map colours, turned so up is the way the camera faces.
 */
public final class OutbreakHud {
    private static final int LCD = 0xFF54D6E4, LCD_DIM = 0x1A54D6E4;
    private static final int[] LEVEL_BG = {0xFFC9B36B, 0xFFD3893B, 0xFFC0482E, 0xFF8C1C18};

    private OutbreakHud() {}

    public static void render(DrawContext c, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.options.hudHidden || mc.options.debugEnabled) return;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        // keep clear of the game's potion icons in the top-right corner
        boolean good = false, bad = false;
        for (StatusEffectInstance e : p.getStatusEffects()) {
            if (!e.shouldShowIcon()) continue;
            if (e.getEffectType().isBeneficial()) good = true;
            else bad = true;
        }
        int top = 3 + (good ? 26 : 0) + (bad ? 26 : 0);
        int bottom = watch(c, mc, w - 66, top);
        moodles(c, mc, p, w, bottom + 4);
        Minimap.render(c, mc, w, h);
    }

    // ---------------------------------------------------------------- watch

    private static int watch(DrawContext c, MinecraftClient mc, int x, int y) {
        ClientWorld world = mc.world;
        long t = world.getTimeOfDay();
        int hours = (int) ((t / 1000 + 6) % 24), minutes = (int) ((t % 1000) * 60 / 1000);
        c.fill(x, y, x + 62, y + 32, 0xD00C0F10);
        c.fill(x, y, x + 62, y + 1, 0xFF3A4246);
        c.fill(x, y + 31, x + 62, y + 32, 0xFF050607);
        int dx = x + 5;
        segDigit(c, dx, y + 3, hours / 10);
        segDigit(c, dx + 12, y + 3, hours % 10);
        c.fill(dx + 24, y + 7, dx + 26, y + 9, LCD);                  // colon
        c.fill(dx + 24, y + 12, dx + 26, y + 14, LCD);
        segDigit(c, dx + 29, y + 3, minutes / 10);
        segDigit(c, dx + 41, y + 3, minutes % 10);
        // the temperature and the date, small
        float f = temperatureF(mc);
        long day = t / 24000;
        int[] md = date(day);
        String line = String.format("%.1f°F %02d/%02d", f, md[0], md[1]);
        c.getMatrices().push();
        c.getMatrices().translate(x + 4, y + 22, 0);
        c.getMatrices().scale(0.75f, 0.75f, 1f);
        c.drawText(mc.textRenderer, line, 0, 0, LCD, false);
        c.getMatrices().pop();
        return y + 32;
    }

    /** a seven-segment digit, 10 wide and 17 tall */
    private static void segDigit(DrawContext c, int x, int y, int d) {
        // segments: a top, b top-right, c bottom-right, d bottom, e bottom-left, f top-left, g middle
        int[] mask = {0b1111110, 0b0110000, 0b1101101, 0b1111001, 0b0110011, 0b1011011, 0b1011111, 0b1110000, 0b1111111, 0b1111011};
        int m = mask[d];
        seg(c, (m & 0b1000000) != 0, x + 2, y, x + 8, y + 2);
        seg(c, (m & 0b0100000) != 0, x + 8, y + 2, x + 10, y + 8);
        seg(c, (m & 0b0010000) != 0, x + 8, y + 9, x + 10, y + 15);
        seg(c, (m & 0b0001000) != 0, x + 2, y + 15, x + 8, y + 17);
        seg(c, (m & 0b0000100) != 0, x, y + 9, x + 2, y + 15);
        seg(c, (m & 0b0000010) != 0, x, y + 2, x + 2, y + 8);
        seg(c, (m & 0b0000001) != 0, x + 2, y + 7, x + 8, y + 9);
    }

    private static void seg(DrawContext c, boolean on, int x0, int y0, int x1, int y1) {
        c.fill(x0, y0, x1, y1, on ? LCD : LCD_DIM);
    }

    /** the biome's warmth, a day/night swing, and rain */
    private static float temperatureF(MinecraftClient mc) {
        BlockPos pos = mc.player.getBlockPos();
        float biome = mc.world.getBiome(pos).value().getTemperature();
        float hours = ((mc.world.getTimeOfDay() / 1000f) + 6f) % 24f;
        float c = biome * 25f - 2f + 6f * (float) Math.sin((hours - 9f) / 24f * Math.PI * 2);
        if (mc.world.isRaining()) c -= 3f;
        if (pos.getY() > 120) c -= (pos.getY() - 120) / 20f;
        return c * 1.8f + 32f;
    }

    /** month and day, counting from July 9th */
    private static int[] date(long day) {
        int[] len = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int month = 7, dom = 9 + (int) (day % 365);
        while (dom > len[month - 1]) {
            dom -= len[month - 1];
            month = month % 12 + 1;
        }
        return new int[]{month, dom};
    }

    // ---------------------------------------------------------------- moodles

    private record Moodle(String id, int level) {}

    private static final Map<String, Integer> lastLevel = new HashMap<>();
    private static final Map<String, Long> shownAt = new HashMap<>();
    private static float wetness;

    /** called every client tick: things that build up over time */
    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        if (p.isTouchingWaterOrRain()) wetness = Math.min(2400, wetness + (p.isSubmergedInWater() ? 8 : 1.5f));
        else wetness = Math.max(0, wetness - (p.isOnFire() ? 10 : 1));
        Minimap.tick(mc);
    }

    private static List<Moodle> current(MinecraftClient mc, ClientPlayerEntity p) {
        List<Moodle> out = new ArrayList<>();
        int food = p.getHungerManager().getFoodLevel();
        if (food <= 14) out.add(new Moodle("hungry", food <= 3 ? 4 : food <= 6 ? 3 : food <= 10 ? 2 : 1));
        float hp = p.getHealth() / p.getMaxHealth();
        if (hp <= 0.75f) out.add(new Moodle("injured", hp <= 0.15f ? 4 : hp <= 0.3f ? 3 : hp <= 0.5f ? 2 : 1));
        StatusEffectInstance inf = p.getStatusEffect(OutbreakEffects.INFECTION);
        if (inf != null) {
            int left = inf.getDuration();
            out.add(new Moodle("queasy", left > 16000 ? 1 : left > 8000 ? 2 : left > 3000 ? 3 : 4));
        }
        int near = mc.world.getEntitiesByClass(ZombieEntity.class, p.getBoundingBox().expand(10), z -> z.isAlive()).size();
        near = CharacterClient.panicScale(near);
        if (near > 0) out.add(new Moodle("panic", near >= 10 ? 4 : near >= 6 ? 3 : near >= 3 ? 2 : 1));
        if (wetness > 200) out.add(new Moodle("wet", wetness > 2200 ? 4 : wetness > 1500 ? 3 : wetness > 800 ? 2 : 1));
        return out;
    }

    private static void moodles(DrawContext c, MinecraftClient mc, ClientPlayerEntity p, int w, int y) {
        long now = net.minecraft.util.Util.getMeasuringTimeMs();
        List<Moodle> list = current(mc, p);
        // remember changes, to name the moodle for a moment
        Map<String, Integer> seen = new HashMap<>();
        for (Moodle m : list) {
            seen.put(m.id, m.level);
            Integer was = lastLevel.get(m.id);
            if (was == null || was != m.level) shownAt.put(m.id, now);
        }
        lastLevel.clear();
        lastLevel.putAll(seen);
        int x = w - 22;
        for (Moodle m : list) {
            int bg = LEVEL_BG[m.level - 1];
            disc(c, x + 9, y + 9, 9.5f, 0xC0000000);
            disc(c, x + 9, y + 9, 8.5f, bg);
            RenderSystem.enableBlend();
            c.drawTexture(new Identifier("outbreak", "textures/gui/moodle/" + m.id + ".png"), x + 1, y + 1, 16, 16, 0, 0, 32, 32, 32, 32);
            Long at = shownAt.get(m.id);
            if (at != null && now - at < 4000) {
                Text name = Text.translatable("moodle.outbreak." + m.id + "." + m.level);
                int tw = mc.textRenderer.getWidth(name);
                c.fill(x - tw - 8, y + 4, x - 2, y + 15, 0xB0000000);
                c.drawTextWithShadow(mc.textRenderer, name, x - tw - 5, y + 6, 0xFFF0E6D2);
            }
            y += 21;
        }
    }

    private static void disc(DrawContext c, float cx, float cy, float r, int color) {
        int ri = (int) Math.ceil(r);
        for (int dy = -ri; dy <= ri; dy++) {
            int half = (int) Math.sqrt(Math.max(0, r * r - (dy + 0.5f) * (dy + 0.5f)));
            if (half > 0) c.fill((int) cx - half, (int) cy + dy, (int) cx + half, (int) cy + dy + 1, color);
        }
    }

    // ---------------------------------------------------------------- minimap

    /** a 128x128 map (one pixel a block) around you, rebuilt a few rows per tick */
    static final class Minimap {
        private static final int N = 128, SIZE = 84;
        private static NativeImageBackedTexture tex;
        private static Identifier id;
        private static int cx = Integer.MIN_VALUE, cz, row, groundY;

        static void tick(MinecraftClient mc) {
            ClientPlayerEntity p = mc.player;
            if (p == null || mc.world == null) return;
            if (tex == null) {
                tex = new NativeImageBackedTexture(N, N, true);
                id = mc.getTextureManager().registerDynamicTexture("outbreak_minimap", tex);
            }
            int px = p.getBlockX(), pz = p.getBlockZ();
            if (cx == Integer.MIN_VALUE || Math.abs(px - cx) > 6 || Math.abs(pz - cz) > 6) {
                cx = px;
                cz = pz;
                groundY = p.getBlockY();
                row = 0;
            }
            NativeImage img = tex.getImage();
            if (img == null) return;
            for (int k = 0; k < 16 && row < N; k++, row++) {
                for (int u = 0; u < N; u++) img.setColor(u, row, abgr(color(mc.world, cx - N / 2 + u, cz - N / 2 + row)));
            }
            tex.upload();
            if (row >= N) row = 0;
        }

        static int color(ClientWorld w, int x, int z) {
            if (!w.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return 0x1E2022;
            int top = w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
            int solid = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos at = new BlockPos(x, top, z);
            BlockState s = w.getBlockState(at);
            if (solid >= groundY + 2) {
                if (SkillsClient.inSafehouse(new BlockPos(x, groundY, z))) return ((x + z) & 7) == 0 ? 0x5E9E5E : 0x6EB46E;   // your safehouse
                return ((x + z) & 15) == 0 ? 0x6E6EC8 : 0x7878D2;   // a building
            }
            if (!s.getFluidState().isEmpty()) return 0x5A82BE;
            if (s.isIn(BlockTags.LEAVES)) return 0x6F8C52;
            Identifier bid = Registries.BLOCK.getId(s.getBlock());
            if (bid.getPath().startsWith("asphalt") || bid.getPath().contains("line") || bid.getPath().contains("crosswalk")) return 0x9A9A98;
            MapColor mcol = w.getBlockState(new BlockPos(x, solid, z)).getMapColor(w, new BlockPos(x, solid, z));
            if (mcol == MapColor.PALE_GREEN || mcol == MapColor.DARK_GREEN || mcol == MapColor.GREEN) return 0xC8C79A;
            if (mcol == MapColor.STONE_GRAY || mcol == MapColor.GRAY || mcol == MapColor.BLACK || mcol == MapColor.DEEPSLATE_GRAY) return 0xB4B2AC;
            return 0xD8CCA6;                                                // ground: Zomboid beige
        }

        private static int abgr(int rgb) {
            return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
        }

        static void render(DrawContext c, MinecraftClient mc, int w, int h) {
            if (id == null) return;
            int x1 = w - 4, y1 = h - 4, x0 = x1 - SIZE, y0 = y1 - SIZE;
            c.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF15171A);
            c.enableScissor(x0, y0, x1, y1);
            float camYaw = mc.gameRenderer.getCamera().getYaw();
            ClientPlayerEntity p = mc.player;
            // where you are within the map (it's re-centred every few blocks)
            float ox = (float) (p.getX() - cx), oz = (float) (p.getZ() - cz);
            float scale = SIZE / 72f;                                     // about 72 blocks across
            c.getMatrices().push();
            c.getMatrices().translate((x0 + x1) / 2f, (y0 + y1) / 2f, 0);
            c.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-180f - camYaw));
            c.getMatrices().scale(scale, scale, 1);
            c.getMatrices().translate(-N / 2f - ox, -N / 2f - oz, 0);
            c.drawTexture(id, 0, 0, 0, 0, N, N, N, N);
            c.getMatrices().pop();
            c.disableScissor();
            int mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
            c.fill(mx - 2, my - 2, mx + 2, my + 2, 0xFF000000);
            c.fill(mx - 1, my - 1, mx + 1, my + 1, 0xFFE02020);
        }
    }

    static int clamp(int v) {
        return MathHelper.clamp(v, 0, 255);
    }
}
