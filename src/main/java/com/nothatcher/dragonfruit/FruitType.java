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
            new Ability("gear_third", 220),
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
            new Ability("quake_bubble", 300)),

    THUNDER("thunder", 0x8FD8FF,
            new Ability("lightning_strike", 50),
            new Ability("thunder_dash", 60),
            new Ability("thunder_storm", 240)),

    MAGMA("magma", 0xE0461A,
            new Ability("magma_fist", 70),
            new Ability("eruption", 180),
            new Ability("meteor_rain", 280)),

    GRAVITY("gravity", 0x9A6BFF,
            new Ability("gravity_crush", 120),
            new Ability("gravity_lift", 140),
            new Ability("meteor_call", 400)),

    // animal fruits: Transform turns you into the beast
    WOLF("wolf", 0x9AA3AD,
            new Ability("pounce", 40),
            new Ability("howl", 300),
            new Ability("frenzy", 160)),

    DRAGON("dragon", 0x2FAF5A,
            new Ability("dragon_breath", 160),
            new Ability("wing_gust", 100),
            new Ability("dive_bomb", 200)),

    REINDEER("reindeer", 0xA0703A,
            new Ability("antler_charge", 80),
            new Ability("healing_burst", 400),
            new Ability("horn_guard", 400));

    /** What the Transform key does for this fruit. */
    public enum Form { NONE, ELEMENT, BEAST }

    public Form form() {
        return switch (this) {
            case FLAME, ICE, MAGMA, THUNDER, LIGHT -> Form.ELEMENT;
            case WOLF, DRAGON, REINDEER -> Form.BEAST;
            default -> Form.NONE;
        };
    }

    public record Ability(String id, int cooldown) {}

    public final String id;
    public final int color;
    /** this fruit's own abilities */
    public final Ability[] abilities;
    /** fruit abilities followed by the Willpower abilities everyone has (this is what the ability bar shows) */
    public final Ability[] full;

    FruitType(String id, int color, Ability... abilities) {
        this.id = id;
        this.color = color;
        this.abilities = abilities;
        this.full = new Ability[abilities.length + Willpower.ABILITIES.length];
        System.arraycopy(abilities, 0, full, 0, abilities.length);
        System.arraycopy(Willpower.ABILITIES, 0, full, abilities.length, Willpower.ABILITIES.length);
    }

    public boolean isWillpower(int index) {
        return index >= abilities.length;
    }

    public Text displayName() {
        return Text.translatable("fruit.dragonfruit." + id);
    }

    public Text abilityName(int index) {
        if (index < 0 || index >= full.length) return Text.empty();
        if (isWillpower(index)) return Text.translatable("ability.dragonfruit.willpower." + full[index].id());
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
