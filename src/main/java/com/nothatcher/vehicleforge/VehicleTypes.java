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
            .speed(0.85f, 0.022f, 3.6f).paint(0x5FB8B0).camera(6.5f).len(2.66, -2.66).gears(3));
    public static final VehicleType SEDAN = add(new VehicleType("sedan", Category.CAR, 1.9f, 1.5f)
            .seat(0.4, 0.3, 0.15).seat(-0.4, 0.3, 0.15).seat(0.4, 0.3, -0.9).seat(-0.4, 0.3, -0.9)
            .speed(1.0f, 0.03f, 4f).paint(0x2E4E8F).camera(6.5f).len(2.38, -2.38));
    public static final VehicleType SPORTS = add(new VehicleType("sports_car", Category.CAR, 1.9f, 1.2f)
            .seat(0.38, 0.15, -0.2).seat(-0.38, 0.15, -0.2)
            .speed(1.5f, 0.05f, 4.6f).paint(0xE0301E).camera(6f).len(2.3, -2.32).gears(6));
    public static final VehicleType PICKUP = add(new VehicleType("pickup_truck", Category.TRUCK, 2.1f, 1.9f)
            .seat(0.42, 0.55, 0.6).seat(-0.42, 0.55, 0.6).seat(0.5, 0.35, -1.3).seat(-0.5, 0.35, -1.3)
            .speed(0.95f, 0.028f, 3.6f).paint(0x3C6E3A).camera(7f).horn(Horn.TRUCK).sound("engine_truck").len(2.85, -3.0).gears(5).offroad());
    public static final VehicleType VAN = add(new VehicleType("van", Category.TRUCK, 2.0f, 2.2f)
            .seat(0.42, 0.6, 1.2).seat(-0.42, 0.6, 1.2).seat(0.42, 0.55, -0.2).seat(-0.42, 0.55, -0.2).seat(0.42, 0.55, -1.2).seat(-0.42, 0.55, -1.2)
            .speed(0.85f, 0.024f, 3.4f).paint(0xF2F2F2).camera(7f).horn(Horn.TRUCK).sound("engine_truck").len(2.62, -2.7).gears(5));
    public static final VehicleType SEMI = add(new VehicleType("semi_truck", Category.TRUCK, 2.4f, 3.0f)
            .seat(0.5, 1.3, 3.05).seat(-0.5, 1.3, 3.05)
            .speed(0.8f, 0.016f, 2.6f).health(140f).paint(0xB0122A).camera(11f).horn(Horn.TRUCK).sound("engine_truck").step(1.1f).len(5.8, -3.72).gears(8));
    public static final VehicleType FIRETRUCK = add(new VehicleType("fire_truck", Category.TRUCK, 2.4f, 2.8f)
            .seat(0.5, 1.1, 2.55).seat(-0.5, 1.1, 2.55)
            .speed(0.8f, 0.02f, 2.8f).health(140f).paint(0xC81E1E).camera(10f).horn(Horn.TRUCK).sound("engine_truck")
            .siren().weapon(Weapon.WATER_CANNON).len(3.7, -3.95));
    public static final VehicleType BICYCLE = add(new VehicleType("bicycle", Category.BIKE, 0.7f, 1.2f)
            .seat(0, 0.58, -0.18).speed(0.45f, 0.02f, 6f).health(20f).paint(0x2A9DF4).camera(4.5f).horn(Horn.BIKE_BELL).sound("none").len(0.98, -0.98).gears(0).endurance(0));
    public static final VehicleType MOTORBIKE = add(new VehicleType("motorbike", Category.BIKE, 0.8f, 1.2f)
            .seat(0, 0.55, -0.3).seat(0, 0.6, -0.68).speed(1.4f, 0.05f, 5.5f).health(35f).paint(0x161616).camera(5f).sound("engine_bike").len(1.2, -1.13).gears(6));
    public static final VehicleType DIRTBIKE = add(new VehicleType("dirt_bike", Category.BIKE, 0.8f, 1.3f)
            .seat(0, 0.62, -0.3).speed(1.15f, 0.05f, 6f).health(30f).paint(0xF07A10).camera(5f).sound("engine_bike").step(1.25f).len(1.24, -1.17).offroad());

    // -------------------------------------------------------------- military
    public static final VehicleType JEEP = add(new VehicleType("army_jeep", Category.MILITARY, 2.2f, 2.0f)
            .seat(0.45, 0.5, 0.3).seat(-0.45, 0.5, 0.3).seat(0, 1.07, -1.25).seat(0.45, 0.5, -0.6)
            .speed(1.0f, 0.03f, 3.8f).health(120f).paint(0x5B6B3A).camera(7.5f).horn(Horn.TRUCK).sound("engine_truck")
            .weapon(Weapon.MACHINE_GUN).step(1.25f).len(2.6, -2.2));
    public static final VehicleType TANK = add(new VehicleType("tank", Category.MILITARY, 3.2f, 2.4f)
            .seat(0, 1.4, -0.3).seat(0.5, 1.4, -1.3)
            .speed(0.5f, 0.012f, 2.4f).health(400f).paint(0x4F5D33).camera(12f).horn(Horn.TRUCK).sound("engine_tank")
            .weapon(Weapon.TANK_CANNON).step(1.3f).len(3.0, -3.1).gears(4));
    public static final VehicleType ARTILLERY = add(new VehicleType("rocket_artillery", Category.MILITARY, 2.6f, 3.0f)
            .seat(0.5, 1.1, 2.75).seat(-0.5, 1.1, 2.75)
            .speed(0.7f, 0.016f, 2.6f).health(180f).paint(0x55643A).camera(12f).horn(Horn.TRUCK).sound("engine_truck")
            .weapon(Weapon.ROCKET_SALVO).step(1.1f).len(3.7, -3.6));

    // ---------------------------------------------------------- air & space
    public static final VehicleType HELICOPTER = add(new VehicleType("helicopter", Category.HELICOPTER, 2.6f, 2.6f)
            .seat(0.45, 0.55, 1.0).seat(-0.45, 0.55, 1.0).seat(0.45, 0.55, -0.1).seat(-0.45, 0.55, -0.1)
            .speed(1.3f, 0.04f, 4f).health(120f).paint(0x1E2A3A).camera(12f).sound("engine_heli").weapon(Weapon.AIR_ROCKETS).len(2.4, -6.0).endurance(15));
    public static final VehicleType PROP_PLANE = add(new VehicleType("prop_plane", Category.PLANE, 2.6f, 2.2f)
            .seat(0, 0.55, 0.3).seat(0, 0.55, -0.7)
            .speed(1.6f, 0.02f, 3.5f).health(90f).paint(0xE8C21A).camera(13f).sound("engine_prop").weapon(Weapon.MACHINE_GUN).len(3.0, -5.5).endurance(25));
    public static final VehicleType JET = add(new VehicleType("jet", Category.PLANE, 2.8f, 2.3f)
            .seat(0, 0.75, 1.6)
            .speed(2.6f, 0.03f, 3.2f).health(140f).paint(0x7E8A96).camera(16f).sound("engine_jet").weapon(Weapon.AIR_ROCKETS).len(5.9, -6.0).endurance(25));
    public static final VehicleType ROCKET = add(new VehicleType("rocket_ship", Category.ROCKET, 2.6f, 4.0f)
            .seat(0, 7.6, 0.3).seat(0.5, 6.0, 0).seat(-0.5, 6.0, 0)
            .speed(2.4f, 0.05f, 2.5f).health(200f).paint(0xF2F2F2).camera(22f).sound("engine_rocket").len(1.2, -1.2).endurance(3));

    // ----------------------------------------------------- RV, rail, water
    public static final VehicleType RV = add(new VehicleType("rv", Category.RV, 2.6f, 3.2f)
            .seat(0.55, 1.0, 3.2).seat(-0.55, 1.0, 3.2).seat(0.55, 1.0, 1.4).seat(-0.55, 1.0, 1.4)
            .speed(0.75f, 0.016f, 2.6f).health(160f).paint(0xF2EEE2).camera(12f).horn(Horn.TRUCK).sound("engine_truck").room().step(1.1f).len(4.45, -4.45));
    public static final VehicleType TRAIN = add(new VehicleType("steam_train", Category.TRAIN, 0.98f, 2.8f)
            .seat(0.3, 1.0, -1.8).seat(-0.3, 1.0, -1.8)
            .speed(1.3f, 0.012f, 0f).health(400f).paint(0x1E3A2A).camera(14f).horn(Horn.TRAIN).sound("engine_train").carriages(2).len(4.1, -6.3).smoke(0, 3.9, 2.9).carriage("train_carriage", 9.65f).endurance(40));
    public static final VehicleType SPEEDBOAT = add(new VehicleType("speedboat", Category.BOAT, 2.0f, 1.0f)
            .seat(0.4, 0.15, 0.2).seat(-0.4, 0.15, 0.2).seat(0.4, 0.15, -1.0).seat(-0.4, 0.15, -1.0)
            .speed(1.6f, 0.04f, 4f).health(60f).paint(0xF2F2F2).camera(8f).horn(Horn.BOAT).sound("engine_boat").step(0.6f).len(3.1, -2.7).endurance(25));
    public static final VehicleType YACHT = add(new VehicleType("yacht", Category.BOAT, 3.4f, 1.6f)
            .seat(0.5, 1.75, 1.3).seat(-0.5, 1.75, 1.3).seat(0.8, 0.65, -2.0).seat(-0.8, 0.65, -2.0).seat(0, 0.65, -3.0)
            .speed(1.1f, 0.016f, 2.4f).health(200f).paint(0xFAFAFA).camera(14f).horn(Horn.BOAT).sound("engine_boat").step(0.6f).len(6.0, -4.8).endurance(30));


    // ============================================================ wave 2
    // ---------------------------------------------------------- city cars
    public static final VehicleType POLICE = add(new VehicleType("police_car", Category.CAR, 1.9f, 1.5f)
            .seat(0.4, 0.3, 0.15).seat(-0.4, 0.3, 0.15).seat(0.4, 0.3, -0.9).seat(-0.4, 0.3, -0.9)
            .speed(1.25f, 0.04f, 4.2f).health(90f).paint(0xF5F5F5).camera(6.5f).siren().len(2.45, -2.45));
    public static final VehicleType TAXI = add(new VehicleType("taxi", Category.CAR, 1.9f, 1.5f)
            .seat(0.4, 0.3, 0.15).seat(-0.4, 0.3, 0.15).seat(0.4, 0.3, -0.9).seat(-0.4, 0.3, -0.9)
            .speed(1.0f, 0.03f, 4f).paint(0xF2C21B).camera(6.5f).len(2.38, -2.38));
    public static final VehicleType HATCHBACK = add(new VehicleType("hatchback", Category.CAR, 1.8f, 1.5f)
            .seat(0.4, 0.26, 0.05).seat(-0.4, 0.26, 0.05).seat(0.4, 0.26, -0.85).seat(-0.4, 0.26, -0.85)
            .speed(0.95f, 0.032f, 4.6f).health(50f).paint(0x3FA34D).camera(6f).len(2.0, -2.0));
    public static final VehicleType SUV = add(new VehicleType("suv", Category.CAR, 2.1f, 2.0f)
            .seat(0.4, 0.6, 0.3).seat(-0.4, 0.6, 0.3).seat(0.4, 0.6, -0.85).seat(-0.4, 0.6, -0.85)
            .speed(1.05f, 0.03f, 3.8f).health(90f).paint(0x4A5560).camera(7f).len(2.75, -2.75).offroad().step(1.1f).gears(6));
    public static final VehicleType MUSCLE = add(new VehicleType("muscle_car", Category.CAR, 1.95f, 1.3f)
            .seat(0.4, 0.22, -0.1).seat(-0.4, 0.22, -0.1).seat(0.4, 0.22, -1.0).seat(-0.4, 0.22, -1.0)
            .speed(1.35f, 0.05f, 3.9f).paint(0xF07818).camera(6.5f).len(2.6, -2.6).gears(4));
    public static final VehicleType ROADSTER = add(new VehicleType("vintage_roadster", Category.CAR, 1.8f, 1.6f)
            .seat(0.33, 0.6, -0.6).seat(-0.33, 0.6, -0.6)
            .speed(0.6f, 0.018f, 3.6f).health(50f).paint(0x6B1E2A).camera(6f).len(2.25, -2.1).gears(3));
    public static final VehicleType LIMO = add(new VehicleType("limousine", Category.CAR, 2.0f, 1.5f)
            .seat(0.4, 0.3, 1.0).seat(-0.4, 0.3, 1.0).seat(0.4, 0.3, -0.2).seat(-0.4, 0.3, -0.2).seat(0.4, 0.3, -1.6).seat(-0.4, 0.3, -1.6)
            .speed(0.95f, 0.022f, 3.0f).health(90f).paint(0x111114).camera(9f).len(3.7, -3.7));
    public static final VehicleType ATV = add(new VehicleType("atv", Category.CAR, 1.5f, 1.3f)
            .seat(0, 0.6, -0.2)
            .speed(0.85f, 0.04f, 5.5f).health(35f).paint(0xB71C1C).camera(5f).len(1.1, -1.1).offroad().step(1.2f));

    // --------------------------------------------------- service & heavy
    public static final VehicleType AMBULANCE = add(new VehicleType("ambulance", Category.TRUCK, 2.0f, 2.4f)
            .seat(0.42, 0.6, 1.2).seat(-0.42, 0.6, 1.2).seat(-0.55, 0.55, -0.6).seat(-0.55, 0.55, -1.6)
            .speed(0.95f, 0.028f, 3.4f).health(100f).paint(0xF4F4F4).camera(7f).horn(Horn.TRUCK).sound("engine_truck").siren().len(2.62, -2.7));
    public static final VehicleType CITY_BUS = add(rows(new VehicleType("city_bus", Category.TRUCK, 2.6f, 3.2f)
            .seat(0.75, 0.75, 4.4), 0.75, 0.75, 3.0, 7)
            .speed(0.75f, 0.015f, 2.4f).health(180f).paint(0x1F6FB5).camera(13f).horn(Horn.TRUCK).sound("engine_truck").len(5.55, -5.55).step(1.1f));
    public static final VehicleType SCHOOL_BUS = add(rows(new VehicleType("school_bus", Category.TRUCK, 2.6f, 3.2f)
            .seat(0.7, 1.0, 3.4), 0.7, 1.0, 2.3, 7)
            .speed(0.7f, 0.014f, 2.4f).health(180f).paint(0xF2B90F).camera(13f).horn(Horn.TRUCK).sound("engine_truck").len(5.7, -5.6).step(1.1f));
    public static final VehicleType DUMP = add(new VehicleType("dump_truck", Category.TRUCK, 2.6f, 3.2f)
            .seat(0.5, 1.3, 2.75).seat(-0.5, 1.3, 2.75)
            .speed(0.7f, 0.015f, 2.6f).health(160f).paint(0xE0A020).camera(11f).horn(Horn.TRUCK).sound("engine_truck").len(4.5, -4.25).offroad().step(1.2f).gears(8));
    public static final VehicleType TRACTOR = add(new VehicleType("tractor", Category.TRUCK, 2.0f, 2.7f)
            .seat(0, 1.1, -0.6)
            .speed(0.45f, 0.02f, 4.0f).health(80f).paint(0x2F7D32).camera(6.5f).horn(Horn.TRUCK).sound("engine_truck").len(2.3, -1.6).offroad().step(1.3f).gears(4));
    public static final VehicleType MONSTER = add(new VehicleType("monster_truck", Category.TRUCK, 2.6f, 3.6f)
            .seat(0.42, 1.85, 0.6).seat(-0.42, 1.85, 0.6).seat(0.5, 1.65, -1.3).seat(-0.5, 1.65, -1.3)
            .speed(1.0f, 0.035f, 3.6f).health(160f).paint(0x7B2FBE).camera(9f).horn(Horn.TRUCK).sound("engine_truck").len(3.0, -3.0).offroad().step(2.0f).gears(4));
    public static final VehicleType SCOOTER = add(new VehicleType("scooter", Category.BIKE, 0.7f, 1.2f)
            .seat(0, 0.55, -0.3).seat(0, 0.6, -0.62)
            .speed(0.75f, 0.035f, 6f).health(25f).paint(0x8FD3C8).camera(4.5f).sound("engine_bike").len(0.85, -0.85).gears(0));

    // ----------------------------------------------------- military, air
    public static final VehicleType APC = add(new VehicleType("apc", Category.MILITARY, 2.8f, 2.6f)
            .seat(0.45, 0.45, 1.4).seat(-0.45, 0.45, 1.4).seat(0.6, 0.45, -0.6).seat(-0.6, 0.45, -0.6).seat(0.6, 0.45, -1.8).seat(-0.6, 0.45, -1.8)
            .speed(0.85f, 0.02f, 2.8f).health(300f).paint(0x5B6B3A).camera(11f).horn(Horn.TRUCK).sound("engine_tank")
            .weapon(Weapon.MACHINE_GUN).turret(0, 2.4, 0.3, 1.9).amphibious().len(3.2, -2.8).step(1.3f).gears(6));
    public static final VehicleType AIRLINER = add(rows(new VehicleType("airliner", Category.PLANE, 4.0f, 3.8f)
            .seat(0.5, 1.25, 9.3).seat(-0.5, 1.25, 9.3), 0.9, 1.25, 6.5, 6)
            .speed(2.4f, 0.012f, 1.6f).health(300f).paint(0xF4F6F8).camera(30f).sound("engine_jet").len(11.5, -12.0).endurance(40));

    // ------------------------------------------------------ water, rail
    public static final VehicleType JET_SKI = add(new VehicleType("jet_ski", Category.BOAT, 1.2f, 1.0f)
            .seat(0, 0.15, -0.3).seat(0, 0.2, -0.85)
            .speed(1.5f, 0.05f, 5.5f).health(35f).paint(0x1565C0).camera(5f).horn(Horn.BOAT).sound("engine_bike").len(1.3, -1.3).step(0.6f).endurance(20));
    public static final VehicleType SUBMARINE = add(new VehicleType("submarine", Category.SUBMARINE, 2.6f, 2.6f)
            .seat(0.4, 0.45, 2.2).seat(-0.4, 0.45, 2.2).seat(0.4, 0.45, 0.6).seat(-0.4, 0.45, 0.6).seat(0.4, 0.45, -1.0).seat(-0.4, 0.45, -1.0)
            .speed(0.7f, 0.015f, 2.4f).health(250f).paint(0x2F3E46).camera(12f).horn(Horn.BOAT).sound("engine_boat").len(4.6, -6.6).endurance(40));
    public static final VehicleType DIESEL = add(new VehicleType("diesel_locomotive", Category.TRAIN, 0.98f, 3.4f)
            .seat(0.4, 1.1, 3.4).seat(-0.4, 1.1, 3.4)
            .speed(1.6f, 0.012f, 0f).health(450f).paint(0x1F4E8C).camera(14f).horn(Horn.TRAIN).sound("engine_truck")
            .carriages(2).len(5.0, -5.0).smoke(0, 3.1, 1.0).carriage("train_carriage", 8.35f).endurance(40));
    public static final VehicleType BULLET = add(new VehicleType("bullet_train", Category.TRAIN, 0.98f, 3.0f)
            .seat(0.4, 1.2, 3.4).seat(-0.4, 1.2, 3.4)
            .speed(2.4f, 0.016f, 0f).health(450f).paint(0xF4F6F8).camera(16f).horn(Horn.TRAIN).sound("engine_jet")
            .carriages(3).len(8.0, -6.0).carriage("bullet_carriage", 9.35f).endurance(40));

    /** adds rows of seats: pairs at x = +-x, starting at z0 going back 1.1 per row */
    private static VehicleType rows(VehicleType t, double x, double y, double z0, int count) {
        for (int i = 0; i < count; i++) {
            double z = z0 - i * 1.1;
            t.seat(x, y, z).seat(-x, y, z);
        }
        return t;
    }

    private VehicleTypes() {}

    public static void init() {
        // touching the class runs the static initialisers above
    }
}
