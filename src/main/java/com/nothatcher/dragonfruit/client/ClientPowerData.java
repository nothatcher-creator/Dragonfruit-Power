package com.nothatcher.dragonfruit.client;

import com.nothatcher.dragonfruit.FruitType;

/** What the server told us about our own power. */
public final class ClientPowerData {
    public static FruitType fruit = FruitType.NONE;
    public static int selected;
    private static int[] cooldownEnd = new int[0];
    private static int buffEnd;
    private static int clock;
    /** ticks since the selected ability last changed (for the HUD toast) */
    public static int selectAge = 999;

    /** every other player's fruit, for body effects */
    private static final java.util.Map<java.util.UUID, FruitType> OTHERS = new java.util.HashMap<>();

    private ClientPowerData() {}

    private static final java.util.Set<java.util.UUID> IN_FORM = new java.util.HashSet<>();

    public static void setForm(java.util.UUID id, boolean on) {
        if (on) IN_FORM.add(id);
        else IN_FORM.remove(id);
    }

    public static boolean inForm(net.minecraft.entity.player.PlayerEntity p) {
        return IN_FORM.contains(p.getUuid()) && fruitOf(p).form() != FruitType.Form.NONE;
    }

    /** The beast this player currently is, or NONE. */
    public static FruitType beastOf(net.minecraft.entity.player.PlayerEntity p) {
        FruitType f = fruitOf(p);
        return inForm(p) && f.form() == FruitType.Form.BEAST ? f : FruitType.NONE;
    }

    public static void setOther(java.util.UUID id, FruitType f) {
        if (f == FruitType.NONE) OTHERS.remove(id);
        else OTHERS.put(id, f);
    }

    public static FruitType fruitOf(net.minecraft.entity.player.PlayerEntity p) {
        if (p == net.minecraft.client.MinecraftClient.getInstance().player) return fruit;
        return OTHERS.getOrDefault(p.getUuid(), FruitType.NONE);
    }

    public static void apply(int fruitOrdinal, int[] remaining, int buffRemaining) {
        FruitType newFruit = FruitType.byOrdinal(fruitOrdinal);
        if (newFruit != fruit) {
            selected = 0;
            selectAge = 0;
        }
        fruit = newFruit;
        cooldownEnd = new int[remaining.length];
        for (int i = 0; i < remaining.length; i++) cooldownEnd[i] = clock + remaining[i];
        buffEnd = clock + buffRemaining;
        if (selected >= fruit.full.length) selected = 0;
    }

    public static void tick() {
        clock++;
        selectAge++;
    }

    public static void cycle(int dir) {
        int n = fruit.full.length;
        if (n == 0) return;
        selected = Math.floorMod(selected + dir, n);
        selectAge = 0;
    }

    public static int cooldownLeft(int i) {
        if (i < 0 || i >= cooldownEnd.length) return 0;
        return Math.max(0, cooldownEnd[i] - clock);
    }

    public static float cooldownFraction(int i, float td) {
        if (i < 0 || i >= fruit.full.length) return 0;
        int total = fruit.full[i].cooldown();
        if (total <= 0) return 0;
        return Math.max(0f, Math.min(1f, (cooldownLeft(i) - td) / total));
    }

    public static boolean buffActive() {
        return buffEnd > clock;
    }

    public static int buffLeft() {
        return Math.max(0, buffEnd - clock);
    }

    public static void reset() {
        OTHERS.clear();
        IN_FORM.clear();
        fruit = FruitType.NONE;
        selected = 0;
        cooldownEnd = new int[0];
        buffEnd = 0;
    }
}
