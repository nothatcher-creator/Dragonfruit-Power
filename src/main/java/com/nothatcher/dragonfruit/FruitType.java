package com.nothatcher.dragonfruit;

import net.minecraft.text.Text;

/**
 * Every Dragonfruit power set. To add a new fruit:
 *  1. add an entry here (id, colour, abilities with cooldowns in ticks),
 *  2. add its ability code in power/Abilities.java (the switch in run()),
 *  3. add a texture + model + lang lines (see README).
 */
public enum FruitType {
    NONE("none", 0xFFFFFF),

    RUBBER("rubber", 0xE04FB4,
            new Ability("pistol", 18),
            new Ability("rocket", 50),
            new Ability("gatling", 150),
            new Ability("second_gear", 600)),

    FLAME("flame", 0xFF7A1A,
            new Ability("fire_fist", 70),
            new Ability("flame_pillar", 140),
            new Ability("firefly_swarm", 120),
            new Ability("flame_body", 400)),

    ICE("ice", 0x8FE3FF,
            new Ability("ice_spears", 60),
            new Ability("ice_age", 200),
            new Ability("ice_coffin", 160)),

    LIGHT("light", 0xFFE55C,
            new Ability("light_flash", 40),
            new Ability("light_barrage", 160),
            new Ability("light_cannon", 240)),

    QUAKE("quake", 0xF4F1E4,
            new Ability("quake_punch", 60),
            new Ability("ground_quake", 160),
            new Ability("quake_bubble", 300));

    public record Ability(String id, int cooldown) {}

    public final String id;
    public final int color;
    public final Ability[] abilities;

    FruitType(String id, int color, Ability... abilities) {
        this.id = id;
        this.color = color;
        this.abilities = abilities;
    }

    public Text displayName() {
        return Text.translatable("fruit.dragonfruit." + id);
    }

    public Text abilityName(int index) {
        if (index < 0 || index >= abilities.length) return Text.empty();
        return Text.translatable("ability.dragonfruit." + id + "." + abilities[index].id());
    }

    public static FruitType byId(String id) {
        for (FruitType t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return NONE;
    }

    public static FruitType byOrdinal(int ordinal) {
        FruitType[] v = values();
        return ordinal >= 0 && ordinal < v.length ? v[ordinal] : NONE;
    }
}
