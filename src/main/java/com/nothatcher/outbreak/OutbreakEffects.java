package com.nothatcher.outbreak;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class OutbreakEffects {
    public static final InfectionEffect INFECTION = new InfectionEffect();

    private OutbreakEffects() {}

    public static void register() {
        Registry.register(Registries.STATUS_EFFECT, Outbreak.id("infection"), INFECTION);
    }

}
