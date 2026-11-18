package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.DragonfruitMod;
import com.nothatcher.dragonfruit.FruitType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Skins per fruit:
 * 1. Elemental form active -> the built-in elemental body skin.
 * 2. Otherwise, if you put a 64x64 skin PNG at config/dragonfruit/skins/&lt;fruit&gt;.png (e.g. rubber.png),
 *    every player with that fruit is shown with that skin on your screen.
 */
public final class CustomSkins {
    private static final Map<FruitType, Identifier> LOADED = new EnumMap<>(FruitType.class);
    private static final Set<FruitType> MISSING = EnumSet.noneOf(FruitType.class);
    private static final Map<FruitType, Identifier> ELEMENT = new EnumMap<>(FruitType.class);

    static {
        for (FruitType f : FruitType.values()) {
            if (f.form() == FruitType.Form.ELEMENT) ELEMENT.put(f, DragonfruitMod.id("textures/entity/form/" + f.id + ".png"));
        }
    }

    private CustomSkins() {}

    public static Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve("dragonfruit").resolve("skins");
    }

    /** Creates the folder and a short how-to the first time the game starts. */
    public static void ensureFolder() {
        try {
            Path dir = folder();
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.txt");
            if (!Files.exists(readme)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Dragonfruit Powers - custom skins per fruit\n\n");
                sb.append("Put a normal 64x64 Minecraft skin PNG here named after a fruit, for example:\n");
                for (FruitType f : FruitType.values()) if (f != FruitType.NONE) sb.append("  ").append(f.id).append(".png\n");
                sb.append("\nAnyone with that fruit will be shown with that skin on your screen.\n");
                sb.append("Changes are picked up when you rejoin a world.\n");
                Files.writeString(readme, sb.toString());
            }
        } catch (IOException e) {
            DragonfruitMod.LOG.warn("Could not create skins folder", e);
        }
    }

    public static Identifier skinFor(AbstractClientPlayerEntity player) {
        FruitType f = ClientPowerData.fruitOf(player);
        if (f == FruitType.NONE) return null;
        if (ClientPowerData.inForm(player)) {
            Identifier element = ELEMENT.get(f);
            if (element != null) return element;
        }
        return custom(f);
    }

    private static Identifier custom(FruitType f) {
        Identifier have = LOADED.get(f);
        if (have != null || MISSING.contains(f)) return have;
        Path file = folder().resolve(f.id + ".png");
        if (!Files.exists(file)) {
            MISSING.add(f);
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage img = NativeImage.read(in);
            if (img.getWidth() != 64 || img.getHeight() != 64) {
                DragonfruitMod.LOG.warn("Skin {} must be 64x64 (is {}x{})", file, img.getWidth(), img.getHeight());
                img.close();
                MISSING.add(f);
                return null;
            }
            Identifier id = DragonfruitMod.id("custom_skin/" + f.id);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
            LOADED.put(f, id);
            return id;
        } catch (Exception e) {
            DragonfruitMod.LOG.warn("Could not load skin {}", file, e);
            MISSING.add(f);
            return null;
        }
    }

    public static void clear() {
        LOADED.clear();
        MISSING.clear();
    }
}
