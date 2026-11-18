package com.nothatcher.dragonfruit;

/** Willpower abilities every player has, fruit or not. They keep working in water and while sealed by seastone. */
public final class Willpower {
    public static final FruitType.Ability[] ABILITIES = {
            new FruitType.Ability("sense", 400),
            new FruitType.Ability("armor", 500),
            new FruitType.Ability("dominance", 900)
    };
    public static final int COLOR = 0xD8D8E8;

    private Willpower() {}
}
