package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleType.Category;
import com.nothatcher.vehicleforge.VehicleType.Horn;
import com.nothatcher.vehicleforge.VehicleType.Weapon;

import java.util.LinkedHashMap;
import java.util.Map;

/** The full vehicle line-up. Seats: x = left, y = seat height, z = forward. Seat 0 is the driver. */
public final class VehicleTypes {
    public static final Map<String, VehicleType> ALL = new LinkedHashMap<>();

    private static VehicleType add(VehicleType t) {
        ALL.put(t.id, t);
        return t;
    }

    // ------------------------------------------------------------------ road
    public static final VehicleType CLASSIC = add(new VehicleType("classic_cruiser", Category.CAR, 2.0f, 1.5f)
            .seat(0.42, 0.35, 0.1).seat(-0.42, 0.35, 0.1).seat(0.42, 0.35, -1.0).seat(-0.42, 0.35, -1.0)
            .speed(0.85f, 0.022f, 3.6f).paint(0x5FB8B0).camera(6.5f));
    public static final VehicleType SEDAN = add(new VehicleType("sedan", Category.CAR, 1.9f, 1.5f)
            .seat(0.4, 0.3, 0.15).seat(-0.4, 0.3, 0.15).seat(0.4, 0.3, -0.9).seat(-0.4, 0.3, -0.9)
            .speed(1.0f, 0.03f, 4f).paint(0x2E4E8F).camera(6.5f));
    public static final VehicleType SPORTS = add(new VehicleType("sports_car", Category.CAR, 1.9f, 1.2f)
            .seat(0.38, 0.15, -0.2).seat(-0.38, 0.15, -0.2)
            .speed(1.5f, 0.05f, 4.6f).paint(0xE0301E).camera(6f));
    public static final VehicleType PICKUP = add(new VehicleType("pickup_truck", Category.TRUCK, 2.1f, 1.9f)
            .seat(0.42, 0.55, 0.6).seat(-0.42, 0.55, 0.6).seat(0.5, 0.35, -1.3).seat(-0.5, 0.35, -1.3)
            .speed(0.95f, 0.028f, 3.6f).paint(0x3C6E3A).camera(7f).horn(Horn.TRUCK).sound("engine_truck"));
    public static final VehicleType VAN = add(new VehicleType("van", Category.TRUCK, 2.0f, 2.2f)
            .seat(0.42, 0.6, 1.2).seat(-0.42, 0.6, 1.2).seat(0.42, 0.55, -0.2).seat(-0.42, 0.55, -0.2).seat(0.42, 0.55, -1.2).seat(-0.42, 0.55, -1.2)
            .speed(0.85f, 0.024f, 3.4f).paint(0xF2F2F2).camera(7f).horn(Horn.TRUCK).sound("engine_truck"));
    public static final VehicleType SEMI = add(new VehicleType("semi_truck", Category.TRUCK, 2.4f, 3.0f)
            .seat(0.5, 1.3, 3.05).seat(-0.5, 1.3, 3.05)
            .speed(0.8f, 0.016f, 2.6f).health(140f).paint(0xB0122A).camera(11f).horn(Horn.TRUCK).sound("engine_truck").step(1.1f));
    public static final VehicleType FIRETRUCK = add(new VehicleType("fire_truck", Category.TRUCK, 2.4f, 2.8f)
            .seat(0.5, 1.1, 2.55).seat(-0.5, 1.1, 2.55)
            .speed(0.8f, 0.02f, 2.8f).health(140f).paint(0xC81E1E).camera(10f).horn(Horn.TRUCK).sound("engine_truck")
            .siren().weapon(Weapon.WATER_CANNON));
    public static final VehicleType BICYCLE = add(new VehicleType("bicycle", Category.BIKE, 0.7f, 1.2f)
            .seat(0, 0.58, -0.18).speed(0.45f, 0.02f, 6f).health(20f).paint(0x2A9DF4).camera(4.5f).horn(Horn.BIKE_BELL).sound("none"));
    public static final VehicleType MOTORBIKE = add(new VehicleType("motorbike", Category.BIKE, 0.8f, 1.2f)
            .seat(0, 0.55, -0.3).seat(0, 0.6, -0.68).speed(1.4f, 0.05f, 5.5f).health(35f).paint(0x161616).camera(5f).sound("engine_bike"));
    public static final VehicleType DIRTBIKE = add(new VehicleType("dirt_bike", Category.BIKE, 0.8f, 1.3f)
            .seat(0, 0.62, -0.3).speed(1.15f, 0.05f, 6f).health(30f).paint(0xF07A10).camera(5f).sound("engine_bike").step(1.25f));

    // -------------------------------------------------------------- military
    public static final VehicleType JEEP = add(new VehicleType("army_jeep", Category.MILITARY, 2.2f, 2.0f)
            .seat(0.45, 0.5, 0.3).seat(-0.45, 0.5, 0.3).seat(0, 1.07, -1.25).seat(0.45, 0.5, -0.6)
            .speed(1.0f, 0.03f, 3.8f).health(120f).paint(0x5B6B3A).camera(7.5f).horn(Horn.TRUCK).sound("engine_truck")
            .weapon(Weapon.MACHINE_GUN).step(1.25f));
    public static final VehicleType TANK = add(new VehicleType("tank", Category.MILITARY, 3.2f, 2.4f)
            .seat(0, 1.4, -0.3).seat(0.5, 1.4, -1.3)
            .speed(0.5f, 0.012f, 2.4f).health(400f).paint(0x4F5D33).camera(12f).horn(Horn.TRUCK).sound("engine_tank")
            .weapon(Weapon.TANK_CANNON).step(1.3f));
    public static final VehicleType ARTILLERY = add(new VehicleType("rocket_artillery", Category.MILITARY, 2.6f, 3.0f)
            .seat(0.5, 1.1, 2.75).seat(-0.5, 1.1, 2.75)
            .speed(0.7f, 0.016f, 2.6f).health(180f).paint(0x55643A).camera(12f).horn(Horn.TRUCK).sound("engine_truck")
            .weapon(Weapon.ROCKET_SALVO).step(1.1f));

    // ---------------------------------------------------------- air & space
    public static final VehicleType HELICOPTER = add(new VehicleType("helicopter", Category.HELICOPTER, 2.6f, 2.6f)
            .seat(0.45, 0.55, 1.0).seat(-0.45, 0.55, 1.0).seat(0.45, 0.55, -0.1).seat(-0.45, 0.55, -0.1)
            .speed(1.3f, 0.04f, 4f).health(120f).paint(0x1E2A3A).camera(12f).sound("engine_heli").weapon(Weapon.AIR_ROCKETS));
    public static final VehicleType PROP_PLANE = add(new VehicleType("prop_plane", Category.PLANE, 2.6f, 2.2f)
            .seat(0, 0.55, 0.3).seat(0, 0.55, -0.7)
            .speed(1.6f, 0.02f, 3.5f).health(90f).paint(0xE8C21A).camera(13f).sound("engine_prop").weapon(Weapon.MACHINE_GUN));
    public static final VehicleType JET = add(new VehicleType("jet", Category.PLANE, 2.8f, 2.3f)
            .seat(0, 0.75, 1.6)
            .speed(2.6f, 0.03f, 3.2f).health(140f).paint(0x7E8A96).camera(16f).sound("engine_jet").weapon(Weapon.AIR_ROCKETS));
    public static final VehicleType ROCKET = add(new VehicleType("rocket_ship", Category.ROCKET, 2.6f, 4.0f)
            .seat(0, 7.6, 0.3).seat(0.5, 6.0, 0).seat(-0.5, 6.0, 0)
            .speed(2.4f, 0.05f, 2.5f).health(200f).paint(0xF2F2F2).camera(22f).sound("engine_rocket"));

    // ----------------------------------------------------- RV, rail, water
    public static final VehicleType RV = add(new VehicleType("rv", Category.RV, 2.6f, 3.2f)
            .seat(0.55, 1.0, 3.2).seat(-0.55, 1.0, 3.2).seat(0.55, 1.0, 1.4).seat(-0.55, 1.0, 1.4)
            .speed(0.75f, 0.016f, 2.6f).health(160f).paint(0xF2EEE2).camera(12f).horn(Horn.TRUCK).sound("engine_truck").room().step(1.1f));
    public static final VehicleType TRAIN = add(new VehicleType("steam_train", Category.TRAIN, 0.98f, 2.8f)
            .seat(0.3, 1.0, -1.8).seat(-0.3, 1.0, -1.8)
            .speed(1.3f, 0.012f, 0f).health(400f).paint(0x1E3A2A).camera(14f).horn(Horn.TRAIN).sound("engine_train").carriages(2));
    public static final VehicleType SPEEDBOAT = add(new VehicleType("speedboat", Category.BOAT, 2.0f, 1.0f)
            .seat(0.4, 0.15, 0.2).seat(-0.4, 0.15, 0.2).seat(0.4, 0.15, -1.0).seat(-0.4, 0.15, -1.0)
            .speed(1.6f, 0.04f, 4f).health(60f).paint(0xF2F2F2).camera(8f).horn(Horn.BOAT).sound("engine_boat").step(0.6f));
    public static final VehicleType YACHT = add(new VehicleType("yacht", Category.BOAT, 3.4f, 1.6f)
            .seat(0.5, 1.75, 1.3).seat(-0.5, 1.75, 1.3).seat(0.8, 0.65, -2.0).seat(-0.8, 0.65, -2.0).seat(0, 0.65, -3.0)
            .speed(1.1f, 0.016f, 2.4f).health(200f).paint(0xFAFAFA).camera(14f).horn(Horn.BOAT).sound("engine_boat").step(0.6f));

    private VehicleTypes() {}

    public static void init() {
        // touching the class runs the static initialisers above
    }
}
