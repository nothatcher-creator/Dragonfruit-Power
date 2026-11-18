package com.nothatcher.vehicleforge.client.model;

/**
 * Materials = cells of the 128x128 palette texture (textures/entity/vehicle_palette.png, 8x8 cells of 16px).
 * tint: multiplied by the vehicle's paint colour (1 = full paint, 0.6 = darker trim, 0 = no tint).
 */
public enum Mat {
    PAINT(0, 1f, Layer.SOLID),
    PAINT_DARK(1, 0.6f, Layer.SOLID),
    CHROME(2), BLACK(3), TIRE(4), RIM(5),
    GLASS(6, 0f, Layer.GLASS),
    SEAT(7), DASH(8), GAUGE(9),
    HEADLIGHT(10, 0f, Layer.GLOW),
    TAILLIGHT(11, 0f, Layer.GLOW),
    SIREN_RED(12, 0f, Layer.GLOW),
    SIREN_BLUE(13, 0f, Layer.GLOW),
    WOOD(14), CANVAS(15), CAMO(16), METAL(17), BRASS(18), CARPET(19), WHITE(20), RED(21), YELLOW(22),
    ROTOR(23),
    FLAME(24, 0f, Layer.GLOW),
    GRILLE(25), PLATE(26), DECK(27), NAVY(28), ORANGE(29), WALL(30),
    SCREEN(31, 0f, Layer.GLOW),
    TREAD(32), SMOKESTACK(33), COAL(34), STRIPE(35, 0.8f, Layer.SOLID), RUBBER(36), CANOPY(37, 0f, Layer.GLASS),
    INDICATOR(38, 0f, Layer.GLOW), REVERSE(39, 0f, Layer.GLOW),
    FEATHER(40), SKIN(41), RUG(42), ALIEN(43, 0f, Layer.GLOW), BALLOON(44), WICKER(45), TIME_GLOW(46, 0f, Layer.GLOW), STEEL(47);

    public enum Layer { SOLID, GLASS, GLOW }

    public final int cell;
    public final float tint;
    public final Layer layer;

    Mat(int cell) {
        this(cell, 0f, Layer.SOLID);
    }

    Mat(int cell, float tint, Layer layer) {
        this.cell = cell;
        this.tint = tint;
        this.layer = layer;
    }

    public float u0() {
        return ((cell % 8) * 16 + 1) / 128f;
    }

    public float u1() {
        return ((cell % 8) * 16 + 15) / 128f;
    }

    public float v0() {
        return ((cell / 8) * 16 + 1) / 128f;
    }

    public float v1() {
        return ((cell / 8) * 16 + 15) / 128f;
    }
}
