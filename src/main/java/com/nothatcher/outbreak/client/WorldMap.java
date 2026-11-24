package com.nothatcher.outbreak.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The world map remembers every chunk you've seen, in the minimap's Zomboid colours, and keeps it per world
 * (in .minecraft/outbreak/maps). Chunks are drawn from textures of 16x16 chunks each.
 */
public final class WorldMap {
    static final Map<Long, int[]> CHUNKS = new HashMap<>();
    static final Map<Long, Tile> TILES = new HashMap<>();
    private static String worldId;
    private static long lastSave;

    static final class Tile {
        NativeImageBackedTexture tex;
        Identifier id;
        boolean dirty = true;
    }

    private WorldMap() {}

    static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> mc.execute(() -> {
            save();
            for (Tile t : TILES.values()) if (t.tex != null) t.tex.close();
            TILES.clear();
            CHUNKS.clear();
            worldId = null;
        }));
    }

    private static String id(MinecraftClient mc) {
        String raw = mc.getServer() != null ? "sp_" + mc.getServer().getSaveProperties().getLevelName()
                : "mp_" + (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "server");
        String dim = mc.world == null ? "" : "_" + mc.world.getRegistryKey().getValue().getPath();
        return (raw + dim).replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static File file() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, "outbreak/maps");
        dir.mkdirs();
        return new File(dir, worldId + ".map");
    }

    static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        String now = id(mc);
        if (!now.equals(worldId)) {
            if (worldId != null) save();
            CHUNKS.clear();
            for (Tile t : TILES.values()) if (t.tex != null) t.tex.close();
            TILES.clear();
            worldId = now;
            load();
        }
        if (mc.player.age % 5 != 0) return;
        ChunkPos c = mc.player.getChunkPos();
        int done = 0;
        for (int r = 0; r <= 6 && done < 2; r++) {
            for (int dx = -r; dx <= r && done < 2; dx++) {
                for (int dz = -r; dz <= r && done < 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = c.x + dx, cz = c.z + dz;
                    long key = ChunkPos.toLong(cx, cz);
                    boolean here = r <= 1 && mc.player.age % 100 == 0;              // keep where you are up to date
                    if ((CHUNKS.containsKey(key) && !here) || !mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                    int[] px = new int[256];
                    for (int i = 0; i < 256; i++) px[i] = OutbreakHud.Minimap.color(mc.world, cx * 16 + (i & 15), cz * 16 + (i >> 4));
                    CHUNKS.put(key, px);
                    Tile t = TILES.get(ChunkPos.toLong(cx >> 4, cz >> 4));
                    if (t != null) t.dirty = true;
                    done++;
                }
            }
        }
        if (System.currentTimeMillis() - lastSave > 120_000) save();
    }

    /** the texture for a 16x16-chunk region, rebuilt when something in it changed */
    static Identifier tile(int rx, int rz) {
        long key = ChunkPos.toLong(rx, rz);
        Tile t = TILES.computeIfAbsent(key, k -> new Tile());
        MinecraftClient mc = MinecraftClient.getInstance();
        if (t.tex == null) {
            t.tex = new NativeImageBackedTexture(256, 256, true);
            t.id = mc.getTextureManager().registerDynamicTexture("outbreak_map_" + rx + "_" + rz, t.tex);
        }
        if (t.dirty) {
            NativeImage img = t.tex.getImage();
            if (img == null) return t.id;
            for (int cx = 0; cx < 16; cx++) {
                for (int cz = 0; cz < 16; cz++) {
                    int[] px = CHUNKS.get(ChunkPos.toLong(rx * 16 + cx, rz * 16 + cz));
                    for (int i = 0; i < 256; i++) {
                        int col = px == null ? 0 : 0xFF000000 | abgr(px[i]);
                        img.setColor(cx * 16 + (i & 15), cz * 16 + (i >> 4), col);
                    }
                }
            }
            t.tex.upload();
            t.dirty = false;
        }
        return t.id;
    }

    private static int abgr(int rgb) {
        return ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }

    static boolean anyIn(int rx, int rz) {
        for (int cx = 0; cx < 16; cx++) for (int cz = 0; cz < 16; cz++) if (CHUNKS.containsKey(ChunkPos.toLong(rx * 16 + cx, rz * 16 + cz))) return true;
        return false;
    }

    static void save() {
        if (worldId == null || CHUNKS.isEmpty()) return;
        lastSave = System.currentTimeMillis();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file())))) {
            out.writeInt(1);
            out.writeInt(CHUNKS.size());
            for (Map.Entry<Long, int[]> e : CHUNKS.entrySet()) {
                out.writeLong(e.getKey());
                for (int c : e.getValue()) {
                    out.writeByte(c >> 16);
                    out.writeByte(c >> 8);
                    out.writeByte(c);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        lastSave = System.currentTimeMillis();
        File f = file();
        if (!f.exists()) return;
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(f)))) {
            if (in.readInt() != 1) return;
            int n = in.readInt();
            for (int k = 0; k < n; k++) {
                long key = in.readLong();
                int[] px = new int[256];
                for (int i = 0; i < 256; i++) px[i] = (in.readUnsignedByte() << 16) | (in.readUnsignedByte() << 8) | in.readUnsignedByte();
                CHUNKS.put(key, px);
            }
        } catch (Exception ignored) {
        }
    }
}
