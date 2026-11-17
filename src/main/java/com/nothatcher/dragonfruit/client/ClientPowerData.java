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

    private ClientPowerData() {}

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
        if (selected >= fruit.abilities.length) selected = 0;
    }

    public static void tick() {
        clock++;
        selectAge++;
    }

    public static void cycle(int dir) {
        int n = fruit.abilities.length;
        if (n == 0) return;
        selected = Math.floorMod(selected + dir, n);
        selectAge = 0;
    }

    public static int cooldownLeft(int i) {
        if (i < 0 || i >= cooldownEnd.length) return 0;
        return Math.max(0, cooldownEnd[i] - clock);
    }

    public static float cooldownFraction(int i, float td) {
        if (i < 0 || i >= fruit.abilities.length) return 0;
        int total = fruit.abilities[i].cooldown();
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
        fruit = FruitType.NONE;
        selected = 0;
        cooldownEnd = new int[0];
        buffEnd = 0;
    }
}
