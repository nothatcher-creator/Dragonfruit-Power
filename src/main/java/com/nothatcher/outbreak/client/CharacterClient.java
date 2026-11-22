package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.CharacterDefs;
import com.nothatcher.outbreak.Characters;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** What this client knows about everyone's characters: looks (for drawing them), and your own sheet (for the HUD). */
public final class CharacterClient {
    public record Info(int look, String name, String occupation, Set<String> traits) {}

    private static final Map<UUID, Info> INFO = new HashMap<>();
    private static final Identifier[] LOOKS = new Identifier[CharacterDefs.LOOKS];
    private static boolean openPending;
    /** while the creation screen is open: the look being tried on (-2 = not previewing) */
    public static int previewLook = -2;

    static {
        for (int i = 0; i < LOOKS.length; i++) LOOKS[i] = new Identifier("outbreak", "textures/entity/survivor/survivor_" + i + ".png");
    }

    private CharacterClient() {}

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Characters.OPEN, (client, handler, buf, sender) -> client.execute(() -> openPending = true));
        ClientPlayNetworking.registerGlobalReceiver(Characters.SYNC, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            Map<UUID, Info> got = new HashMap<>();
            for (int i = 0; i < n; i++) {
                UUID id = buf.readUuid();
                int look = buf.readVarInt();
                String name = buf.readString(), occ = buf.readString();
                int k = buf.readVarInt();
                Set<String> traits = new LinkedHashSet<>();
                for (int j = 0; j < k; j++) traits.add(buf.readString());
                got.put(id, new Info(look, name, occ, traits));
            }
            client.execute(() -> INFO.putAll(got));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            INFO.clear();
            openPending = false;
            previewLook = -2;
        });
    }

    /** opens the creation screen once the world has finished loading in */
    static void tick(MinecraftClient mc) {
        if (!openPending || mc.player == null || mc.world == null) return;
        if (mc.currentScreen instanceof CharacterScreen) {
            openPending = false;
            return;
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
        openPending = false;
        mc.setScreen(new CharacterScreen());
    }

    public static Info mine() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player == null ? null : INFO.get(mc.player.getUuid());
    }

    public static boolean hasTrait(String t) {
        Info i = mine();
        return i != null && i.traits().contains(t);
    }

    /** the survivor look to draw a player with, or null for their own skin */
    public static Identifier skin(UUID player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int look;
        if (previewLook != -2 && mc.player != null && mc.player.getUuid().equals(player)) look = previewLook;
        else {
            Info i = INFO.get(player);
            look = i == null ? -1 : i.look();
        }
        return look >= 0 && look < LOOKS.length ? LOOKS[look] : null;
    }

    /** brave survivors panic less, cowards more */
    public static int panicScale(int near) {
        Info i = mine();
        if (i == null) return near;
        if (i.traits().contains("brave") || i.occupation().equals("veteran")) return near / 2;
        if (i.traits().contains("cowardly")) return near * 2;
        return near;
    }
}
