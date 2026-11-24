package com.nothatcher.vehicleforge.client.model;

import com.nothatcher.vehicleforge.Weapons;
import com.nothatcher.vehicleforge.client.model.Model.Builder;
import com.nothatcher.vehicleforge.client.model.Model.Kind;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Every vehicle model, built from boxes, tapered shells, panes and cylinders.
 * Conventions: x = LEFT, y = up, z = forward, ground at y = 0. Riders' cushions sit at seatY + 0.4.
 */
public final class VehicleModels {
    private static final Map<String, Model> MODELS = new HashMap<>();

    private VehicleModels() {}

    public static Model get(String id) {
        return MODELS.computeIfAbsent(id, VehicleModels::create);
    }

    /** the add-ons fitted to a vehicle, sized from its type (see Upgrades) */
    public static Model upgrades(com.nothatcher.vehicleforge.VehicleType t, int mask) {
        return MODELS.computeIfAbsent("upgrades/" + t.id + "/" + mask, k -> upgradeModel(t, mask));
    }

    private static Model upgradeModel(com.nothatcher.vehicleforge.VehicleType t, int mask) {
        Builder b = Model.builder();
        double w = t.width / 2, f = t.frontZ, r = t.rearZ, h = t.height;
        if ((mask & com.nothatcher.vehicleforge.Upgrades.ARMOR) != 0) {
            b.pair(w, 0.42, r + 0.3, w + 0.06, h * 0.55, f - 0.3, Mat.METAL);
            for (double z = r + 0.5; z < f - 0.4; z += 0.6) b.pair(w + 0.06, h * 0.5, z, w + 0.08, h * 0.5 + 0.05, z + 0.05, Mat.CHROME);
            b.box(-w + 0.1, 0.42, r - 0.06, w - 0.1, h * 0.5, r, Mat.METAL);
        }
        if ((mask & com.nothatcher.vehicleforge.Upgrades.SPIKES) != 0) {
            b.box(-w + 0.05, 0.32, f, w - 0.05, 0.7, f + 0.12, Mat.METAL);
            for (double x = -w + 0.2; x <= w - 0.2; x += 0.28) b.box(x - 0.04, 0.47, f + 0.12, x + 0.04, 0.55, f + 0.42, Mat.CHROME);
        }
        if ((mask & com.nothatcher.vehicleforge.Upgrades.BARS) != 0) {
            for (double z = r + 0.4; z < f - 0.5; z += 0.22) b.pair(w + 0.01, h * 0.58, z, w + 0.03, h * 0.9, z + 0.04, Mat.BLACK);
            for (double x = -w + 0.25; x <= w - 0.25; x += 0.22) b.box(x, h * 0.6, f - 0.95, x + 0.04, h * 0.88, f - 0.91, Mat.BLACK);
        }
        if ((mask & com.nothatcher.vehicleforge.Upgrades.RACK) != 0) {
            b.pair(w * 0.7, h, r * 0.55, w * 0.76, h + 0.1, f * 0.35, Mat.BLACK);
            for (double z = r * 0.55; z <= f * 0.35; z += 0.5) b.box(-w * 0.76, h + 0.08, z, w * 0.76, h + 0.12, z + 0.05, Mat.BLACK);
            b.box(-w * 0.55, h + 0.12, r * 0.4, w * 0.25, h + 0.5, 0, Mat.WOOD);
            b.box(w * 0.3, h + 0.12, r * 0.3, w * 0.6, h + 0.55, r * 0.1, Mat.RED);
        }
        if ((mask & com.nothatcher.vehicleforge.Upgrades.PLOW) != 0) {
            b.box(-w - 0.25, 0.1, f + 0.35, w + 0.25, 0.9, f + 0.5, Mat.YELLOW);
            b.box(-w - 0.25, 0.1, f + 0.5, w + 0.25, 0.2, f + 0.62, Mat.METAL);
            b.pair(0.35, 0.3, f - 0.1, 0.45, 0.42, f + 0.4, Mat.BLACK);
        }
        return b.build();
    }

    private static Model trailer(int kind) {
        Builder b = Model.builder();
        double len = com.nothatcher.vehicleforge.Upgrades.trailerLength(kind), half = len / 2;
        b.box(-0.06, 0.42, half - 0.2, 0.06, 0.52, half + 0.6, Mat.BLACK);                          // drawbar and hitch
        b.cyl(0, 0.52, half + 0.6, 'y', 0.08, 0.08, 0.06, 8, Mat.CHROME, Mat.CHROME);
        switch (kind) {
            case 1 -> {                                                                              // cargo
                b.box(-0.85, 0.45, -half, 0.85, 0.55, half, Mat.METAL);
                b.pair(0.8, 0.55, -half, 0.85, 1.35, half, Mat.PAINT);
                b.box(-0.85, 0.55, half - 0.05, 0.85, 1.35, half, Mat.PAINT);
                b.box(-0.85, 0.55, -half, 0.85, 1.2, -half + 0.05, Mat.PAINT);
                b.box(-0.8, 0.55, -1.2, 0.8, 1.15, 0.6, Mat.CANVAS);
                b.pair(0.55, 0.5, -half - 0.02, 0.8, 0.65, -half, Mat.TAILLIGHT);
                b.wheel(0.98, 0.35, -0.2, 0.35, 0.2, false);
                b.wheel(-0.98, 0.35, -0.2, 0.35, 0.2, false);
            }
            case 2 -> {                                                                              // camper caravan
                b.box(-1.05, 0.45, -half, 1.05, 2.3, half - 0.4, Mat.WHITE);
                b.box(-1.0, 2.3, -half + 0.1, 1.0, 2.4, half - 0.5, Mat.WHITE);
                b.pair(1.05, 0.9, -half, 1.06, 1.05, half - 0.4, Mat.PAINT);
                b.box(-0.9, 0.45, half - 0.4, 0.9, 1.6, half - 0.1, Mat.PAINT);                     // front locker, sloped
                for (int sd = -1; sd <= 1; sd += 2) {
                    b.sidePane(sd * 1.055, 1.3, 1.9, -1.8, -0.6, -1.8, -0.6, Mat.GLASS);
                    b.sidePane(sd * 1.055, 1.3, 1.9, 0.6, 1.5, 0.6, 1.5, Mat.GLASS);
                }
                b.box(1.05, 0.5, -0.4, 1.07, 2.0, 0.3, Mat.PAINT);                                  // door
                b.pair(0.55, 0.55, -half - 0.02, 0.9, 0.7, -half, Mat.TAILLIGHT);
                b.wheel(1.12, 0.35, -0.3, 0.35, 0.2, false);
                b.wheel(-1.12, 0.35, -0.3, 0.35, 0.2, false);
            }
            default -> {                                                                             // boat trailer, with a boat
                b.pair(0.5, 0.35, -half, 0.6, 0.48, half - 0.2, Mat.METAL);
                for (double z = -half + 0.4; z < half - 0.3; z += 1.0) b.box(-0.6, 0.38, z, 0.6, 0.45, z + 0.08, Mat.METAL);
                b.hull(-0.25, 0.25, -half + 0.3, half - 0.6, 0.55, -0.85, 0.85, -half + 0.1, half - 0.2, 1.25, Mat.WHITE, Mat.DECK);
                b.pair(0.84, 1.05, -half + 0.1, 0.86, 1.25, half - 0.2, Mat.PAINT);
                b.box(-0.6, 1.25, -0.2, 0.6, 1.6, 0.6, Mat.GLASS);
                b.box(-0.15, 0.7, -half - 0.1, 0.15, 1.6, -half + 0.15, Mat.BLACK);                   // outboard
                b.wheel(0.75, 0.3, -0.3, 0.3, 0.18, false);
                b.wheel(-0.75, 0.3, -0.3, 0.3, 0.18, false);
            }
        }
        return b.build();
    }

    private static Model create(String id) {
        return switch (id) {
            case "cargo_trailer" -> trailer(1);
            case "camper_trailer" -> trailer(2);
            case "boat_trailer" -> trailer(3);
            case "classic_cruiser" -> classic();
            case "sedan" -> sedan();
            case "sports_car" -> sports();
            case "pickup_truck" -> pickup();
            case "van" -> van(0);
            case "semi_truck" -> semi();
            case "fire_truck" -> fireTruck();
            case "bicycle" -> bicycle();
            case "motorbike" -> motorbike(false);
            case "dirt_bike" -> motorbike(true);
            case "army_jeep" -> jeep();
            case "tank" -> tank();
            case "rocket_artillery" -> artillery();
            case "helicopter" -> helicopter();
            case "prop_plane" -> propPlane();
            case "jet" -> jet();
            case "rocket_ship" -> rocket();
            case "rv" -> rv();
            case "steam_train" -> locomotive();
            case "train_carriage" -> carriage();
            case "speedboat" -> speedboat();
            case "yacht" -> yacht();
            case "police_car" -> police();
            case "taxi" -> taxi();
            case "hatchback" -> hatchback();
            case "suv" -> suv();
            case "muscle_car" -> muscle();
            case "vintage_roadster" -> roadster();
            case "limousine" -> limo();
            case "atv" -> atv();
            case "ambulance" -> van(1);
            case "ice_cream_truck" -> van(2);
            case "city_bus" -> bus(false);
            case "school_bus" -> bus(true);
            case "dump_truck" -> dumpTruck();
            case "tractor" -> tractor();
            case "monster_truck" -> monster();
            case "scooter" -> scooter();
            case "apc" -> apc();
            case "airliner" -> airliner();
            case "jet_ski" -> jetSki();
            case "submarine" -> submarine();
            case "diesel_locomotive" -> diesel();
            case "bullet_train" -> bulletTrain();
            case "bullet_carriage" -> bulletCarriage();
            case "ostrich" -> ostrich();
            case "chrono_car" -> chrono();
            case "shrink_car" -> shrinkCar();
            case "ufo" -> ufo();
            case "flying_carpet" -> carpet();
            case "hamster_ball" -> hamsterBall();
            case "rocket_cart" -> rocketCart();
            case "lawn_mower" -> mower();
            case "go_kart" -> goKart();
            case "golf_cart" -> golfCart();
            case "garbage_truck" -> garbageTruck();
            case "cement_mixer" -> mixer();
            case "forklift" -> forklift();
            case "snowmobile" -> snowmobile();
            case "hovercraft" -> hovercraft();
            case "hot_air_balloon" -> balloon();
            case "excavator" -> excavator();
            case "bulldozer" -> bulldozer();
            case "wheel_loader" -> loader();
            case "road_roller" -> roller();
            case "mobile_crane" -> crane();
            case "tow_truck" -> towTruck();
            case "houseboat" -> houseboat();
            case "build_frame" -> buildFrame();
            case "sailboat" -> sailboat();
            case "armored_truck" -> armoredTruck();
            case "food_truck" -> van(3);
            case "police_motorcycle" -> policeBike();
            case "rally_car" -> rally();
            case "fishing_boat" -> fishingBoat();
            case "cargo_plane" -> cargoPlane();
            case "quad_drone" -> drone();
            case "camper_van" -> van(4);
            case "military_truck" -> militaryTruck();
            case "prison_bus" -> prisonBus();
            default -> sedan();
        };
    }

    // ===================================================================== cars

    /** Shared car shell: lower body, hood, trunk, glass cabin with pillars, lights, bumpers, interior. */
    private record CarSpec(double len, double halfW, double bottom, double belt, double roof,
                           double windBase, double roofFront, double roofBack, double rearBase,
                           double wheelR, double wheelX, double wheelF, double wheelB,
                           double cushion, double frontSeatZ, double rearSeatZ, double dashZ) {}

    private static Builder carShell(CarSpec s, Mat glass) {
        Builder b = Model.builder();
        double hl = s.len / 2, w = s.halfW;
        // lower body + hood + trunk
        b.box(-w, s.bottom, -hl, w, s.belt, hl, Mat.PAINT);
        b.hull(-w + 0.02, w - 0.02, s.windBase, hl, s.belt, -w + 0.06, w - 0.06, s.windBase, hl - 0.12, s.belt + 0.08, Mat.PAINT, Mat.PAINT);
        b.hull(-w + 0.02, w - 0.02, -hl, s.rearBase, s.belt, -w + 0.06, w - 0.06, -hl + 0.1, s.rearBase, s.belt + 0.06, Mat.PAINT, Mat.PAINT);
        // roof
        double rw = w - 0.12;
        b.box(-rw, s.roof - 0.08, s.roofBack, rw, s.roof, s.roofFront, Mat.PAINT);
        // pillars
        for (int side = -1; side <= 1; side += 2) {
            double xo = side * (w - 0.06), xi = side * (w - 0.13);
            b.hull(Math.min(xo, xi), Math.max(xo, xi), s.windBase - 0.1, s.windBase, s.belt,
                    Math.min(xo, xi) + side * -0.03, Math.max(xo, xi) + side * -0.03, s.roofFront - 0.1, s.roofFront, s.roof - 0.08, Mat.PAINT, Mat.PAINT);
            b.hull(Math.min(xo, xi), Math.max(xo, xi), s.rearBase, s.rearBase + 0.1, s.belt,
                    Math.min(xo, xi) + side * -0.03, Math.max(xo, xi) + side * -0.03, s.roofBack, s.roofBack + 0.12, s.roof - 0.08, Mat.PAINT, Mat.PAINT);
            double mid = (s.frontSeatZ + (Double.isNaN(s.rearSeatZ) ? s.roofBack : s.rearSeatZ)) / 2 - 0.1;
            if (!Double.isNaN(s.rearSeatZ)) b.box(Math.min(xo, xi), s.belt, mid - 0.05, Math.max(xo, xi), s.roof - 0.08, mid + 0.05, Mat.PAINT);
            // side glass
            b.sidePane(side * (w - 0.09), s.belt, s.roof - 0.08, s.rearBase + 0.05, s.windBase - 0.05, s.roofBack + 0.08, s.roofFront - 0.05, glass);
            // mirror
            b.box(side > 0 ? w : -w - 0.16, s.belt + 0.08, s.windBase - 0.2, side > 0 ? w + 0.16 : -w, s.belt + 0.2, s.windBase - 0.08, Mat.PAINT_DARK);
        }
        // windshield & rear window
        b.pane(rw, -rw, s.belt + 0.05, s.windBase, rw - 0.03, -rw + 0.03, s.roof - 0.08, s.roofFront, glass);
        b.pane(rw, -rw, s.belt + 0.05, s.rearBase, rw - 0.03, -rw + 0.03, s.roof - 0.08, s.roofBack, glass);
        // lights, grille, bumpers, plates
        b.pair(w - 0.35, s.belt - 0.28, hl - 0.02, w - 0.08, s.belt - 0.12, hl + 0.03, Mat.HEADLIGHT);
        b.pair(w - 0.35, s.belt - 0.25, -hl - 0.03, w - 0.08, s.belt - 0.1, -hl + 0.02, Mat.TAILLIGHT);
        b.pair(w - 0.48, s.belt - 0.25, -hl - 0.03, w - 0.36, s.belt - 0.1, -hl + 0.02, Mat.REVERSE);      // reversing lights
        b.pair(w - 0.1, s.belt - 0.08, -hl - 0.03, w - 0.02, s.belt - 0.02, -hl + 0.02, Mat.INDICATOR);   // turn signals
        b.pair(w - 0.1, s.belt - 0.3, hl - 0.02, w - 0.02, s.belt - 0.2, hl + 0.03, Mat.INDICATOR);
        b.pair(w - 0.01, s.belt - 0.05, s.windBase - 0.05, w + 0.005, s.belt + 0.02, s.windBase + 0.15, Mat.INDICATOR);
        // wipers rest along the bottom of the windscreen and sweep it in the rain
        double tilt = Math.toDegrees(Math.atan2(s.roofFront - s.windBase, (s.roof - 0.08) - (s.belt + 0.05)));
        for (int sd = -1; sd <= 1; sd += 2) {
            double px = sd > 0 ? w - 0.25 : -0.05, py = s.belt + 0.1, pz = s.windBase - 0.04;
            b.group(Kind.WIPER, px, py, pz, tilt, false);
            b.box(px - 0.62, py - 0.012, pz - 0.012, px, py + 0.012, pz + 0.012, Mat.BLACK);
        }
        b.body();
        b.box(-w + 0.45, s.bottom + 0.15, hl - 0.02, w - 0.45, s.belt - 0.12, hl + 0.02, Mat.GRILLE);
        b.box(-w, s.bottom, hl, w, s.bottom + 0.14, hl + 0.08, Mat.CHROME);
        b.box(-w, s.bottom, -hl - 0.08, w, s.bottom + 0.14, -hl, Mat.CHROME);
        b.box(-0.22, s.bottom + 0.16, -hl - 0.04, 0.22, s.bottom + 0.32, -hl - 0.01, Mat.PLATE);
        // interior
        b.box(-w + 0.08, s.bottom + 0.02, s.roofBack, w - 0.08, s.bottom + 0.1, s.windBase, Mat.CARPET);
        b.seat(0.4, s.cushion, s.frontSeatZ, 0.55, Mat.SEAT);
        b.seat(-0.4, s.cushion, s.frontSeatZ, 0.55, Mat.SEAT);
        if (!Double.isNaN(s.rearSeatZ)) b.seat(0, s.cushion, s.rearSeatZ, w * 2 - 0.3, Mat.SEAT);
        b.dashboard(w - 0.1, -w + 0.1, s.belt + 0.05, s.dashZ, 0.4);
        b.steeringWheel(0.4, s.belt + 0.08, s.dashZ - 0.2, 0.17, 20);
        b.wheels(s.wheelX, s.wheelR, s.wheelF, s.wheelB, s.wheelR, 0.28);
        return b;
    }

    private static final CarSpec SEDAN = new CarSpec(4.6, 0.95, 0.3, 1.0, 1.95, 0.95, 0.4, -1.3, -1.5,
            0.38, 0.8, 1.45, -1.45, 0.7, 0.15, -0.9, 0.85);

    private static Model sedan() {
        return carShell(SEDAN, Mat.GLASS).build();
    }

    private static Model classic() {
        CarSpec s = new CarSpec(5.2, 1.0, 0.32, 1.05, 2.0, 0.85, 0.35, -1.45, -1.7,
                0.4, 0.82, 1.7, -1.6, 0.75, 0.1, -1.0, 0.8);
        Builder b = carShell(s, Mat.GLASS);
        // tail fins, chrome side strip, round headlights, whitewall rings
        b.pair(0.78, 1.0, -2.6, 0.98, 1.38, -1.7, Mat.PAINT);
        b.pair(0.98, 0.7, -2.5, 1.02, 0.76, 2.5, Mat.CHROME);
        b.cyl(0.65, 0.78, 2.6, 'z', 0.14, 0.14, 0.05, 10, Mat.CHROME, Mat.HEADLIGHT);
        b.cyl(-0.65, 0.78, 2.6, 'z', 0.14, 0.14, 0.05, 10, Mat.CHROME, Mat.HEADLIGHT);
        b.box(-0.9, 0.42, 2.58, 0.9, 0.72, 2.66, Mat.CHROME);
        for (double z : new double[]{1.7, -1.6}) {
            b.cyl(0.97, 0.4, z, 'x', 0.28, 0.28, 0.02, 12, Mat.WHITE, Mat.WHITE);
            b.cyl(-0.97, 0.4, z, 'x', 0.28, 0.28, -0.02, 12, Mat.WHITE, Mat.WHITE);
        }
        return b.build();
    }

    private static Model sports() {
        CarSpec s = new CarSpec(4.5, 0.97, 0.22, 0.78, 1.72, 0.55, -0.15, -1.0, -1.25,
                0.36, 0.84, 1.5, -1.35, 0.55, -0.2, Double.NaN, 0.45);
        Builder b = carShell(s, Mat.GLASS);
        b.box(-0.9, 1.05, -2.3, 0.9, 1.12, -1.95, Mat.PAINT_DARK);            // spoiler wing
        b.pair(0.55, 0.78, -2.15, 0.62, 1.05, -2.05, Mat.PAINT_DARK);
        b.pair(0.96, 0.38, -0.9, 1.0, 0.68, -0.2, Mat.BLACK);                 // side intakes
        b.box(-0.94, 0.22, -2.3, 0.94, 0.3, 2.3, Mat.BLACK);                  // skirt
        b.pair(0.3, 0.28, -2.32, 0.42, 0.4, -2.25, Mat.CHROME);               // exhausts
        b.box(-0.35, 0.79, 1.0, 0.35, 0.86, 2.0, Mat.STRIPE);                // racing stripe
        b.box(-0.35, 1.72, -1.0, 0.35, 1.74, -0.15, Mat.STRIPE);
        return b.build();
    }

    private static Model pickup() {
        Builder b = Model.builder();
        double w = 1.02;
        // chassis + hood
        b.box(-w, 0.55, 0.9, w, 1.25, 2.7, Mat.PAINT);
        b.hull(-w + 0.02, w - 0.02, 0.9, 2.7, 1.25, -w + 0.06, w - 0.06, 0.9, 2.6, 1.35, Mat.PAINT, Mat.PAINT);
        b.box(-w + 0.3, 0.7, 2.7, w - 0.3, 1.2, 2.74, Mat.GRILLE);
        b.box(-w - 0.02, 0.55, 2.7, w + 0.02, 0.75, 2.85, Mat.CHROME);
        b.pair(w - 0.32, 1.0, 2.7, w - 0.06, 1.18, 2.75, Mat.HEADLIGHT);
        // cab
        b.box(-w, 0.55, -0.55, w, 1.3, 0.9, Mat.PAINT);
        b.box(-w + 0.1, 2.38, -0.5, w - 0.1, 2.48, 0.7, Mat.PAINT);
        b.pane(w - 0.1, -w + 0.1, 1.32, 0.9, w - 0.12, -w + 0.12, 2.38, 0.7, Mat.GLASS);
        b.pane(w - 0.1, -w + 0.1, 1.32, -0.55, w - 0.1, -w + 0.1, 2.38, -0.5, Mat.GLASS);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * (w - 0.05), 1.3, 2.38, -0.5, 0.88, -0.48, 0.7, Mat.GLASS);
            b.box(sd > 0 ? w - 0.1 : -w, 1.3, 0.8, sd > 0 ? w : -w + 0.1, 2.38, 0.9, Mat.PAINT);
            b.box(sd > 0 ? w - 0.1 : -w, 1.3, -0.55, sd > 0 ? w : -w + 0.1, 2.38, -0.45, Mat.PAINT);
            b.box(sd > 0 ? w : -w - 0.16, 1.45, 0.75, sd > 0 ? w + 0.16 : -w, 1.6, 0.88, Mat.PAINT_DARK);
        }
        // bed
        b.box(-w, 0.55, -2.9, w, 0.75, -0.6, Mat.PAINT_DARK);
        b.pair(w - 0.1, 0.75, -2.9, w, 1.35, -0.6, Mat.PAINT);
        b.box(-w, 0.75, -2.9, w, 1.35, -2.8, Mat.PAINT);
        b.pair(w - 0.3, 1.0, -2.95, w - 0.08, 1.25, -2.9, Mat.TAILLIGHT);
        b.box(-w - 0.02, 0.55, -3.0, w + 0.02, 0.72, -2.9, Mat.CHROME);
        // interior
        b.seat(0.42, 0.95, 0.6, 0.6, Mat.SEAT);
        b.seat(-0.42, 0.95, 0.6, 0.6, Mat.SEAT);
        b.dashboard(w - 0.1, -w + 0.1, 1.35, 0.95, 0.42);
        b.steeringWheel(0.42, 1.4, 0.75, 0.19, 25);
        b.wheels(0.88, 0.5, 1.9, -1.9, 0.5, 0.34);
        return b.build();
    }

    private static Model van(int kind) {
        boolean ambulance = kind == 1, iceCream = kind == 2, food = kind == 3, camper = kind == 4;
        Builder b = Model.builder();
        double w = 1.0;
        b.box(-w, 0.45, -2.6, w, 2.4, 1.4, Mat.PAINT);
        b.hull(-w, w, 1.4, 2.5, 0.45, -w, w, 1.4, 2.5, 1.2, Mat.PAINT, Mat.PAINT);
        b.hull(-w, w, 1.4, 2.4, 1.2, -w + 0.05, w - 0.05, 1.4, 1.75, 2.4, Mat.PAINT, Mat.PAINT);
        b.pane(w - 0.06, -w + 0.06, 1.25, 2.42, w - 0.08, -w + 0.08, 2.35, 1.78, Mat.GLASS);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * (w + 0.005), 1.35, 2.15, 0.55, 1.95, 0.55, 1.5, Mat.GLASS);
            b.sidePane(sd * (w + 0.005), 1.4, 2.1, -2.3, 0.3, -2.3, 0.3, Mat.GLASS);
            b.box(sd > 0 ? w : -w - 0.16, 1.5, 1.9, sd > 0 ? w + 0.16 : -w, 1.75, 2.05, Mat.BLACK);
            b.box(sd > 0 ? w : -w - 0.01, 0.6, -0.9, sd > 0 ? w + 0.01 : -w, 0.62, 0.3, Mat.BLACK);
        }
        b.box(-w + 0.3, 0.6, 2.5, w - 0.3, 1.05, 2.53, Mat.GRILLE);
        b.pair(w - 0.3, 0.9, 2.5, w - 0.05, 1.1, 2.55, Mat.HEADLIGHT);
        b.pair(w - 0.15, 1.2, -2.63, w - 0.02, 2.0, -2.6, Mat.TAILLIGHT);
        b.box(-w, 0.45, 2.5, w, 0.65, 2.62, Mat.BLACK);
        b.box(-w, 0.45, -2.7, w, 0.62, -2.6, Mat.BLACK);
        b.pane(w - 0.1, -w + 0.1, 1.4, -2.605, w - 0.1, -w + 0.1, 2.15, -2.605, Mat.GLASS);
        b.box(-w + 0.05, 0.5, -2.5, w - 0.05, 0.58, 1.6, Mat.CARPET);
        b.seat(0.42, 1.0, 1.2, 0.55, Mat.SEAT);
        b.seat(-0.42, 1.0, 1.2, 0.55, Mat.SEAT);
        if (ambulance) {
            b.pair(w, 1.2, -2.6, w + 0.012, 1.35, 1.4, Mat.RED);                       // stripe
            b.pair(w, 1.5, -1.58, w + 0.015, 2.1, -1.38, Mat.RED);                     // cross
            b.pair(w, 1.72, -1.78, w + 0.015, 1.88, -1.18, Mat.RED);
            b.box(-0.8, 2.4, 1.0, 0.8, 2.46, 1.36, Mat.BLACK);                         // light bar
            b.group(Kind.SIREN_A, 0, 0, 0, 0, false);
            b.box(0.05, 2.46, 1.02, 0.76, 2.6, 1.34, Mat.SIREN_RED);
            b.group(Kind.SIREN_B, 0, 0, 0, 0, false);
            b.box(-0.76, 2.46, 1.02, -0.05, 2.6, 1.34, Mat.SIREN_BLUE);
            b.body();
            b.box(-0.78, 2.46, 1.0, 0.78, 2.62, 1.36, Mat.GLASS);
            b.box(0.15, 0.85, -2.4, 0.75, 0.95, -0.4, Mat.WHITE);                      // stretcher
            b.box(0.2, 0.58, -2.3, 0.7, 0.85, -2.2, Mat.METAL);
            b.box(0.2, 0.58, -0.6, 0.7, 0.85, -0.5, Mat.METAL);
            b.box(0.82, 0.58, -2.5, 0.96, 2.25, -0.6, Mat.WHITE);                      // cabinets
            b.box(0.8, 1.4, -2.3, 0.82, 1.7, -1.8, Mat.SCREEN);                        // monitor
            b.seat(-0.55, 0.95, -0.6, 0.55, Mat.SEAT);
            b.seat(-0.55, 0.95, -1.6, 0.55, Mat.SEAT);
        } else if (iceCream) {
            b.sidePane(-w - 0.006, 1.3, 2.1, -2.0, -0.4, -2.0, -0.4, Mat.GLASS);         // serving hatch
            b.box(-w - 0.5, 2.1, -2.1, -w, 2.16, -0.3, Mat.BALLOON);                     // striped awning
            b.box(-w - 0.25, 1.25, -2.0, -w, 1.3, -0.4, Mat.WHITE);                      // counter
            b.box(-0.95, 0.58, -2.3, -0.55, 1.3, -0.3, Mat.WHITE);                       // freezer
            b.box(0.5, 0.58, -2.4, 0.95, 1.5, -1.2, Mat.STEEL);                          // soft-serve machine
            b.cyl(0, 2.4, -0.9, 'y', 0.04, 0.32, 0.75, 10, Mat.WICKER, Mat.WICKER);    // giant cone
            b.cyl(0, 3.15, -0.9, 'y', 0.34, 0.0, 0.45, 10, Mat.WHITE, null);
            b.box(-0.06, 3.55, -0.96, 0.06, 3.68, -0.84, Mat.RED);
            b.pair(w, 0.75, -2.6, w + 0.012, 0.85, 1.4, Mat.RED);
        } else if (camper) {
            b.box(-w + 0.05, 2.4, -2.4, w - 0.05, 2.75, 0.9, Mat.WHITE);                 // pop-top roof
            b.pair(w - 0.06, 2.42, -2.3, w - 0.04, 2.72, 0.8, Mat.CANVAS);
            b.pair(w, 0.45, -2.6, w + 0.01, 1.15, 1.4, Mat.WHITE);                       // two-tone
            b.box(-0.95, 0.58, -2.5, 0.95, 0.95, -1.2, Mat.SEAT);                         // bed at the back
            b.box(-0.95, 0.95, -2.5, 0.95, 1.05, -1.2, Mat.RUG);
            b.box(0.55, 0.58, -0.9, 0.95, 1.3, 0.2, Mat.WOOD);                            // galley
            b.box(0.6, 1.3, -0.8, 0.9, 1.32, -0.4, Mat.STEEL);
            b.cyl(-0.5, 1.2, -2.85, 'x', 0.45, 0.45, 0.04, 12, Mat.BLACK, null);          // bikes on the back
            b.cyl(0.5, 1.2, -2.85, 'x', 0.45, 0.45, 0.04, 12, Mat.BLACK, null);
            b.box(-0.5, 1.15, -2.9, 0.5, 1.6, -2.8, Mat.RED);
            b.box(-0.9, 2.75, -1.0, 0.9, 2.8, 0.4, Mat.SCREEN);                          // solar panel
        } else if (food) {
            b.sidePane(-w - 0.006, 1.3, 2.1, -2.2, -0.2, -2.2, -0.2, Mat.GLASS);         // serving hatch
            b.box(-w - 0.6, 2.15, -2.3, -w, 2.2, -0.1, Mat.YELLOW);                      // awning
            b.box(-w - 0.3, 1.25, -2.2, -w, 1.3, -0.2, Mat.STEEL);                       // counter
            b.box(-0.95, 0.58, -2.4, -0.4, 1.25, -0.3, Mat.STEEL);                       // grill and fryers
            b.box(-0.9, 1.25, -2.3, -0.45, 1.27, -1.3, Mat.BLACK);
            b.box(0.5, 0.58, -2.45, 0.95, 2.0, -1.6, Mat.WHITE);                         // fridge
            b.box(0.6, 1.9, -1.4, 0.95, 2.1, -0.4, Mat.METAL);                           // vent hood
            b.cyl(0, 2.42, -0.9, 'y', 0.5, 0.5, 0.16, 12, Mat.ORANGE, Mat.ORANGE);      // giant burger
            b.cyl(0, 2.58, -0.9, 'y', 0.53, 0.53, 0.1, 12, Mat.WOOD, Mat.WOOD);
            b.cyl(0, 2.68, -0.9, 'y', 0.55, 0.55, 0.04, 12, Mat.YELLOW, Mat.YELLOW);
            b.cyl(0, 2.72, -0.9, 'y', 0.5, 0.18, 0.34, 12, Mat.ORANGE, Mat.ORANGE);
            b.pair(w, 0.75, -2.6, w + 0.012, 0.85, 1.4, Mat.YELLOW);
        } else {
            b.seat(0, 0.95, -0.2, 1.7, Mat.SEAT);
            b.seat(0, 0.95, -1.2, 1.7, Mat.SEAT);
        }
        b.dashboard(w - 0.08, -w + 0.08, 1.35, 1.6, 0.42);
        b.steeringWheel(0.42, 1.45, 1.42, 0.19, 35);
        b.wheels(0.86, 0.42, 1.75, -1.85, 0.42, 0.3);
        return b.build();
    }

    /** Truck cab: conventional nose, tall cab, interior. Returns builder for more parts. */
    private static Builder truckCab(Builder b, double w, double cabBack, double cabFront, double floorY, double roofY, double noseLen) {
        double noseEnd = cabFront + noseLen;
        b.box(-w + 0.15, floorY - 0.35, cabFront, w - 0.15, floorY + 0.75, noseEnd, Mat.PAINT);
        b.hull(-w + 0.15, w - 0.15, cabFront, noseEnd, floorY + 0.75, -w + 0.2, w - 0.2, cabFront, noseEnd - 0.15, floorY + 0.9, Mat.PAINT, Mat.PAINT);
        b.box(-w + 0.3, floorY - 0.25, noseEnd, w - 0.3, floorY + 0.8, noseEnd + 0.04, Mat.GRILLE);
        b.pair(w - 0.35, floorY + 0.35, noseEnd, w - 0.15, floorY + 0.6, noseEnd + 0.05, Mat.HEADLIGHT);
        b.box(-w, floorY - 0.45, noseEnd, w, floorY - 0.2, noseEnd + 0.2, Mat.CHROME);
        b.box(-w, floorY - 0.4, cabBack, w, floorY + 0.95, cabFront, Mat.PAINT);
        b.box(-w, roofY - 0.1, cabBack, w, roofY, cabFront, Mat.PAINT);
        b.pane(w - 0.05, -w + 0.05, floorY + 0.95, cabFront, w - 0.07, -w + 0.07, roofY - 0.1, cabFront - 0.2, Mat.GLASS);
        b.box(-w, floorY + 0.95, cabBack, w, roofY - 0.1, cabBack + 0.1, Mat.PAINT);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * (w - 0.02), floorY + 0.95, roofY - 0.1, cabBack + 0.1, cabFront, cabBack + 0.1, cabFront - 0.2, Mat.GLASS);
            b.box(sd > 0 ? w : -w - 0.2, floorY + 1.0, cabFront - 0.1, sd > 0 ? w + 0.2 : -w, floorY + 1.5, cabFront, Mat.CHROME);
            b.box(sd > 0 ? w - 0.05 : -w - 0.3, floorY - 0.5, cabBack + 0.3, sd > 0 ? w + 0.3 : -w + 0.05, floorY - 0.38, cabFront - 0.3, Mat.METAL);
        }
        b.box(-w + 0.05, floorY, cabBack + 0.1, w - 0.05, floorY + 0.06, cabFront, Mat.CARPET);
        b.seat(0.5, floorY + 0.55, cabBack + 0.75, 0.6, Mat.SEAT);
        b.seat(-0.5, floorY + 0.55, cabBack + 0.75, 0.6, Mat.SEAT);
        b.dashboard(w - 0.08, -w + 0.08, floorY + 1.0, cabFront - 0.2, 0.5);
        b.steeringWheel(0.5, floorY + 1.0, cabFront - 0.45, 0.22, 40);
        return b;
    }

    private static Model semi() {
        Builder b = Model.builder();
        double w = 1.2;
        truckCab(b, w, 2.3, 3.6, 1.15, 3.0, 2.0);
        // sleeper box behind the cab
        b.box(-w, 0.75, 0.9, w, 3.15, 2.3, Mat.PAINT);
        b.box(-w + 0.1, 3.15, 1.0, w - 0.1, 3.45, 3.4, Mat.PAINT_DARK);
        // exhaust stacks, tanks, chassis, fifth wheel
        b.cyl(1.25, 1.2, 2.2, 'y', 0.09, 0.09, 2.5, 8, Mat.CHROME, Mat.BLACK);
        b.cyl(-1.25, 1.2, 2.2, 'y', 0.09, 0.09, 2.5, 8, Mat.CHROME, Mat.BLACK);
        b.cyl(1.0, 0.75, 0.9, 'z', 0.35, 0.35, 1.2, 10, Mat.CHROME, Mat.CHROME);
        b.box(-0.7, 0.5, -3.6, 0.7, 0.85, 3.8, Mat.METAL);
        b.box(-0.9, 0.85, -2.6, 0.9, 1.0, -1.0, Mat.BLACK);
        b.pair(0.9, 0.6, -3.7, 1.2, 1.1, -2.2, Mat.BLACK);
        b.pair(0.9, 0.6, -3.72, 1.2, 0.8, -3.68, Mat.TAILLIGHT);
        b.wheel(1.0, 0.55, 4.6, 0.55, 0.36, true);
        b.wheel(-1.0, 0.55, 4.6, 0.55, 0.36, true);
        for (double z : new double[]{-1.6, -2.9}) {
            b.wheel(1.0, 0.55, z, 0.55, 0.5, false);
            b.wheel(-1.0, 0.55, z, 0.55, 0.5, false);
        }
        return b.build();
    }

    private static Model fireTruck() {
        Builder b = Model.builder();
        double w = 1.2;
        truckCab(b, w, 1.8, 2.9, 0.95, 2.75, 0.6);
        // equipment body with roll-up compartments
        b.box(-w, 0.55, -3.8, w, 2.3, 1.8, Mat.PAINT);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (double z = -3.5; z < 1.4; z += 1.25) {
                b.box(sd > 0 ? w : -w - 0.01, 0.8, z, sd > 0 ? w + 0.01 : -w, 2.05, z + 1.05, Mat.CHROME);
            }
            b.box(sd > 0 ? w : -w - 0.02, 2.05, -3.8, sd > 0 ? w + 0.02 : -w, 2.15, 1.8, Mat.WHITE);
        }
        b.box(-w, 2.3, -3.8, w, 2.4, 1.8, Mat.METAL);
        // long ladder on the roof
        b.pair(0.45, 2.45, -4.2, 0.55, 2.6, 1.6, Mat.CHROME);
        for (double z = -4.0; z < 1.6; z += 0.4) b.box(-0.45, 2.48, z, 0.45, 2.56, z + 0.06, Mat.CHROME);
        // light bar (flashes with the siren)
        b.box(-0.9, 2.75, 2.3, 0.9, 2.82, 2.6, Mat.BLACK);
        b.group(Kind.SIREN_A, 0, 0, 0, 0, false);
        b.box(0.1, 2.82, 2.32, 0.85, 3.02, 2.58, Mat.SIREN_RED);
        b.group(Kind.SIREN_B, 0, 0, 0, 0, false);
        b.box(-0.85, 2.82, 2.32, -0.1, 3.02, 2.58, Mat.SIREN_BLUE);
        b.body();
        // water cannon (aims where the driver looks)
        b.group(Kind.LADDER, 0, Weapons.NOZZLE_Y, Weapons.NOZZLE_Z, 0, false);
        b.cyl(0, Weapons.NOZZLE_Y - 0.4, Weapons.NOZZLE_Z, 'y', 0.18, 0.18, 0.4, 8, Mat.CHROME, Mat.CHROME);
        b.cyl(0, Weapons.NOZZLE_Y, Weapons.NOZZLE_Z, 'z', 0.12, 0.07, 1.4, 8, Mat.BRASS, Mat.BRASS);
        b.body();
        b.box(-w - 0.02, 0.5, -3.95, w + 0.02, 0.7, -3.8, Mat.CHROME);
        b.pair(0.85, 1.0, -3.83, 1.1, 1.3, -3.8, Mat.TAILLIGHT);
        b.wheel(1.0, 0.55, 3.0, 0.55, 0.38, true);
        b.wheel(-1.0, 0.55, 3.0, 0.55, 0.38, true);
        b.wheel(1.0, 0.55, -2.6, 0.55, 0.5, false);
        b.wheel(-1.0, 0.55, -2.6, 0.55, 0.5, false);
        return b.build();
    }

    // ==================================================================== bikes

    private static void spokedWheel(Builder b, double y, double z, double r, double width, boolean steers) {
        b.group(Kind.WHEEL, 0, y, z, r, steers);
        b.cyl(-width / 2, y, z, 'x', r, r, width, 16, Mat.TIRE, null);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * i / 6;
            double dy = Math.sin(a) * r * 0.92, dz = Math.cos(a) * r * 0.92;
            b.quad(new double[]{0, y - dy, z - dz}, new double[]{0, y - dy + 0.02, z - dz}, new double[]{0, y + dy + 0.02, z + dz}, new double[]{0, y + dy, z + dz}, Mat.CHROME);
        }
        b.box(-0.05, y - 0.05, z - 0.05, 0.05, y + 0.05, z + 0.05, Mat.CHROME);
        b.body();
    }

    private static Model bicycle() {
        Builder b = Model.builder();
        double r = 0.36;
        spokedWheel(b, r, 0.62, r, 0.05, true);
        spokedWheel(b, r, -0.62, r, 0.05, false);
        // frame (diamond)
        b.box(-0.03, 0.42, -0.62, 0.03, 0.47, 0.0, Mat.PAINT);
        b.hull(-0.03, 0.03, -0.05, 0.05, 0.42, -0.03, 0.03, -0.2, -0.1, 0.9, Mat.PAINT, Mat.PAINT);
        b.hull(-0.03, 0.03, 0.0, 0.08, 0.45, -0.03, 0.03, 0.45, 0.53, 0.85, Mat.PAINT, Mat.PAINT);
        b.box(-0.03, 0.82, -0.15, 0.03, 0.87, 0.5, Mat.PAINT);
        b.box(-0.06, 0.38, -0.1, 0.06, 0.5, 0.1, Mat.METAL);              // crank
        b.box(-0.12, 0.92, -0.32, 0.12, 0.98, -0.05, Mat.BLACK);           // saddle
        b.box(-0.02, 0.85, -0.2, 0.02, 0.93, -0.15, Mat.CHROME);
        b.group(Kind.BIKE_FORK, 0, 0.6, 0.55, 0, false);
        b.hull(-0.03, 0.03, 0.6, 0.66, r, -0.03, 0.03, 0.5, 0.56, 1.05, Mat.CHROME, Mat.CHROME);
        b.box(-0.32, 1.05, 0.48, 0.32, 1.09, 0.53, Mat.CHROME);
        b.pair(0.25, 1.03, 0.47, 0.34, 1.11, 0.54, Mat.BLACK);
        b.body();
        return b.build();
    }

    private static Model motorbike(boolean dirt) {
        Builder b = Model.builder();
        double r = dirt ? 0.42 : 0.38;
        spokedWheel(b, r, 0.82, r, dirt ? 0.16 : 0.2, true);
        spokedWheel(b, r, -0.75, r, dirt ? 0.18 : 0.26, false);
        b.box(-0.16, 0.35, -0.3, 0.16, 0.75, 0.35, Mat.METAL);                       // engine
        b.cyl(0.2, 0.4, -0.2, 'z', 0.06, 0.06, -0.85, 8, Mat.CHROME, Mat.BLACK);    // exhaust
        b.hull(-0.2, 0.2, -0.05, 0.6, 0.75, -0.17, 0.17, 0.0, 0.5, 1.05, Mat.PAINT, Mat.PAINT); // tank
        b.box(-0.17, dirt ? 0.95 : 0.85, -0.75, 0.17, dirt ? 1.02 : 0.95, -0.05, Mat.BLACK); // seat
        b.hull(-0.06, 0.06, -0.8, -0.3, 0.55, -0.06, 0.06, -0.95, -0.7, dirt ? 1.0 : 0.9, Mat.PAINT, Mat.PAINT); // tail
        b.box(-0.12, dirt ? 0.88 : 0.8, -1.05, 0.12, dirt ? 0.93 : 0.85, -0.6, Mat.PAINT); // rear fender
        b.box(-0.1, 0.82, -1.08, 0.1, 0.9, -1.04, Mat.TAILLIGHT);
        b.group(Kind.BIKE_FORK, 0, 0.9, 0.62, 0, false);
        b.pair(0.08, r, 0.78, 0.12, 1.15, 0.84, Mat.CHROME);
        b.box(-0.4, 1.15, 0.58, 0.4, 1.2, 0.64, Mat.BLACK);
        b.pair(0.33, 1.13, 0.56, 0.45, 1.22, 0.66, Mat.RUBBER);
        b.box(-0.12, 0.95, 0.85, 0.12, 1.15, 0.95, dirt ? Mat.WHITE : Mat.HEADLIGHT);
        if (dirt) b.box(-0.14, r * 2 + 0.05, 0.55, 0.14, r * 2 + 0.12, 1.2, Mat.PAINT);
        else b.box(-0.12, 0.98, 0.84, 0.12, 1.12, 0.9, Mat.HEADLIGHT);
        b.gauge(0, 1.2, 0.6, 0.12);
        b.body();
        return b.build();
    }

    // ================================================================= military

    private static Model jeep() {
        Builder b = Model.builder();
        double w = 1.1;
        b.box(-w, 0.6, -1.9, w, 1.3, 2.4, Mat.PAINT);
        b.box(-w + 0.05, 1.3, 1.0, w - 0.05, 1.4, 2.35, Mat.PAINT);
        b.box(-w + 0.25, 0.7, 2.4, w - 0.25, 1.3, 2.45, Mat.GRILLE);
        b.pair(w - 0.3, 1.0, 2.4, w - 0.08, 1.2, 2.46, Mat.HEADLIGHT);
        b.box(-w - 0.05, 0.55, 2.4, w + 0.05, 0.8, 2.6, Mat.METAL);
        b.box(-w + 0.05, 1.3, 0.9, w - 0.05, 2.0, 1.0, Mat.METAL);            // windshield frame
        b.pane(w - 0.1, -w + 0.1, 1.4, 0.95, w - 0.1, -w + 0.1, 1.95, 0.95, Mat.GLASS);
        b.box(-w + 0.05, 2.3, -1.6, w - 0.05, 2.4, 0.9, Mat.CANVAS);           // canvas roof
        b.pair(w - 0.1, 1.3, 0.8, w, 2.3, 0.9, Mat.METAL);
        b.pair(w - 0.1, 1.3, -1.6, w, 2.3, -1.5, Mat.METAL);
        b.cyl(0, 1.0, -1.95, 'z', 0.4, 0.4, -0.25, 12, Mat.TIRE, Mat.RUBBER); // spare tyre
        b.pair(w - 0.3, 1.0, -1.93, w - 0.08, 1.2, -1.89, Mat.TAILLIGHT);
        b.box(-w + 0.05, 0.62, -1.8, w - 0.05, 0.72, 0.9, Mat.METAL);
        b.seat(0.45, 0.9, 0.3, 0.6, Mat.CANVAS);
        b.seat(-0.45, 0.9, 0.3, 0.6, Mat.CANVAS);
        b.seat(0.45, 0.9, -0.6, 0.6, Mat.CANVAS);
        b.dashboard(w - 0.1, -w + 0.1, 1.3, 0.8, 0.45);
        b.steeringWheel(0.45, 1.35, 0.6, 0.2, 30);
        // gun ring + turret machine gun (aims with the gunner/driver view)
        b.cyl(0, 2.4, -1.0, 'y', 0.55, 0.55, 0.1, 12, Mat.METAL, Mat.METAL);
        b.group(Kind.TURRET, 0, 2.15, -1.0, 0, false);
        b.box(-0.08, 2.0, -1.08, 0.08, 2.5, -0.92, Mat.METAL);
        b.box(-0.3, 2.3, -1.15, 0.3, 2.6, -0.98, Mat.CAMO);
        b.group(Kind.BARREL, 0, 2.55, -1.0, 0, false);
        b.box(-0.08, 2.48, -1.25, 0.08, 2.62, -0.7, Mat.BLACK);
        b.cyl(0, 2.55, -0.7, 'z', 0.04, 0.04, 0.9, 6, Mat.BLACK, Mat.BLACK);
        b.body();
        b.wheels(0.95, 0.5, 1.6, -1.3, 0.5, 0.42);
        return b.build();
    }

    private static Model tank() {
        Builder b = Model.builder();
        double w = 1.4;
        // hull with sloped glacis
        b.box(-w + 0.3, 0.45, -2.8, w - 0.3, 1.45, 1.9, Mat.CAMO);
        b.hull(-w + 0.3, w - 0.3, 1.9, 2.8, 0.45, -w + 0.3, w - 0.3, 1.9, 2.1, 1.45, Mat.CAMO, Mat.CAMO);
        b.box(-w, 1.25, -2.9, w, 1.45, 2.4, Mat.PAINT);                         // fenders
        // tracks with road wheels
        for (int sd = -1; sd <= 1; sd += 2) {
            double x0 = sd > 0 ? w - 0.45 : -w, x1 = sd > 0 ? w : -w + 0.45;
            b.box(x0, 0.0, -2.9, x1, 0.12, 2.6, Mat.TREAD);
            b.box(x0, 1.05, -2.9, x1, 1.2, 2.6, Mat.TREAD);
            b.hull(x0, x1, 2.6, 3.0, 0.3, x0, x1, 2.6, 2.75, 1.1, Mat.TREAD, Mat.TREAD);
            b.hull(x0, x1, -3.1, -2.9, 0.3, x0, x1, -2.9, -2.8, 1.1, Mat.TREAD, Mat.TREAD);
            for (int i = 0; i < 6; i++) {
                double z = -2.35 + i * 0.95;
                b.wheel(sd * (w - 0.22), 0.4, z, 0.36, 0.38, false);
            }
        }
        b.pair(0.9, 1.3, 2.75, 1.1, 1.42, 2.82, Mat.HEADLIGHT);
        b.cyl(0.9, 1.5, -2.6, 'y', 0.25, 0.25, 0.5, 8, Mat.METAL, Mat.METAL);   // fuel drums
        b.cyl(-0.9, 1.5, -2.6, 'y', 0.25, 0.25, 0.5, 8, Mat.METAL, Mat.METAL);
        // turret (rotates with your view) and barrel (pitches)
        double ty = Weapons.TURRET_Y, tz = Weapons.TURRET_Z;
        b.group(Kind.TURRET, 0, ty, tz, 0, false);
        b.hull(-1.0, 1.0, tz - 1.3, tz + 1.0, 1.45, -0.8, 0.8, tz - 1.1, tz + 0.6, 2.45, Mat.CAMO, Mat.PAINT);
        b.box(-0.9, 1.6, tz - 1.6, 0.9, 2.25, tz - 1.25, Mat.PAINT_DARK);
        b.cyl(0.4, 2.45, tz - 0.5, 'y', 0.32, 0.32, 0.15, 10, Mat.METAL, Mat.BLACK);   // commander hatch
        b.box(-0.6, 2.45, tz - 0.3, -0.45, 2.75, tz - 0.15, Mat.METAL);             // antenna base
        b.box(-0.54, 2.75, tz - 0.25, -0.51, 4.0, tz - 0.22, Mat.BLACK);
        b.group(Kind.BARREL, 0, ty, tz, 0, false);
        b.box(-0.3, ty - 0.25, tz + 0.5, 0.3, ty + 0.25, tz + 1.05, Mat.CAMO);       // mantlet
        b.cyl(0, ty, tz + 1.0, 'z', 0.13, 0.13, Weapons.BARREL_LEN - 0.6, 10, Mat.METAL, Mat.BLACK);
        b.cyl(0, ty, tz + Weapons.BARREL_LEN, 'z', 0.18, 0.18, 0.35, 10, Mat.METAL, Mat.BLACK);
        b.body();
        return b.build();
    }

    private static Model artillery() {
        Builder b = Model.builder();
        double w = 1.25;
        truckCab(b, w, 2.0, 3.0, 0.95, 2.75, 0.5);
        b.box(-w, 0.55, -3.6, w, 0.95, 2.0, Mat.CAMO);
        b.box(-w, 0.95, -3.6, w, 1.2, 0.2, Mat.PAINT);
        // launcher rack: 2 rows x 4 tubes, raised and pointing forward-up
        b.hull(-1.0, 1.0, -3.4, 0.0, 1.2, -1.0, 1.0, -2.2, -1.2, 2.6, Mat.METAL, Mat.METAL);
        b.box(-1.05, 2.55, -2.9, 1.05, 3.6, -0.9, Mat.CAMO);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                double x = -0.6 + col * 0.4, y = 2.9 + row * 0.35;
                b.cyl(x, y, -0.88, 'z', 0.14, 0.14, 0.02, 8, Mat.BLACK, Mat.BLACK);
            }
        }
        for (double z : new double[]{2.7, -0.6, -2.4}) {
            b.wheel(1.05, 0.55, z, 0.55, 0.45, z > 0);
            b.wheel(-1.05, 0.55, z, 0.55, 0.45, z > 0);
        }
        return b.build();
    }

    // =============================================================== air & space

    private static Model helicopter() {
        Builder b = Model.builder();
        // cabin
        b.hull(-0.95, 0.95, -0.9, 1.4, 0.35, -0.85, 0.85, -0.8, 1.0, 2.15, Mat.PAINT, Mat.PAINT);
        b.hull(-0.9, 0.9, 1.4, 2.4, 0.4, -0.5, 0.5, 1.4, 1.9, 2.0, Mat.CANOPY, Mat.CANOPY);   // glass nose
        b.box(-0.9, 0.35, -0.9, 0.9, 0.45, 1.4, Mat.CARPET);
        for (int sd = -1; sd <= 1; sd += 2) b.sidePane(sd * 0.93, 1.0, 1.95, -0.6, 1.3, -0.5, 0.9, Mat.GLASS);
        // tail boom and fins
        b.hull(-0.5, 0.5, -2.4, -0.9, 0.9, -0.2, 0.2, -5.6, -0.9, 1.6, Mat.PAINT, Mat.PAINT);
        b.box(-0.05, 1.3, -6.0, 0.05, 2.6, -5.4, Mat.PAINT);
        b.box(-0.9, 1.25, -5.5, 0.9, 1.33, -5.1, Mat.PAINT);
        b.box(-0.45, 2.15, -0.4, 0.45, 2.45, 0.6, Mat.METAL);                       // engine housing
        b.cyl(0, 2.45, 0.1, 'y', 0.1, 0.1, 0.35, 8, Mat.METAL, Mat.METAL);         // mast
        // skids
        b.pair(0.75, 0.0, -1.2, 0.85, 0.08, 1.8, Mat.METAL);
        b.pair(0.7, 0.08, 1.0, 0.76, 0.4, 1.1, Mat.METAL);
        b.pair(0.7, 0.08, -0.6, 0.76, 0.4, -0.5, Mat.METAL);
        // rocket pods
        b.pair(1.0, 0.7, 0.2, 1.3, 1.0, 1.1, Mat.PAINT_DARK);
        b.pair(0.85, 0.82, 0.5, 1.0, 0.9, 0.7, Mat.METAL);
        // interior
        b.seat(0.45, 0.95, 1.0, 0.55, Mat.SEAT);
        b.seat(-0.45, 0.95, 1.0, 0.55, Mat.SEAT);
        b.seat(0.45, 0.95, -0.1, 0.55, Mat.SEAT);
        b.seat(-0.45, 0.95, -0.1, 0.55, Mat.SEAT);
        b.dashboard(0.85, -0.85, 1.35, 1.75, 0.45);
        b.box(0.42, 0.45, 1.35, 0.48, 1.1, 1.42, Mat.BLACK);                         // cyclic stick
        b.box(-0.5, 0.35, -2.4, 0.5, 0.4, -0.9, Mat.PAINT_DARK);
        b.box(-0.02, 2.8, -6.0, 0.02, 2.85, -5.6, Mat.TAILLIGHT);
        // main rotor
        b.group(Kind.ROTOR_Y, 0, 2.85, 0.1, 0, false);
        b.box(-0.18, 2.78, -0.08, 0.18, 2.92, 0.28, Mat.METAL);
        b.box(-5.2, 2.83, 0.02, 5.2, 2.87, 0.22, Mat.ROTOR);
        b.box(-0.1, 2.83, -5.1, 0.1, 2.87, 5.3, Mat.ROTOR);
        // tail rotor
        b.group(Kind.ROTOR_X, 0.12, 2.0, -5.75, 0, false);
        b.box(0.1, 1.2, -5.8, 0.14, 2.8, -5.7, Mat.ROTOR);
        b.box(0.1, 1.95, -6.55, 0.14, 2.05, -4.95, Mat.ROTOR);
        b.body();
        return b.build();
    }

    private static Model propPlane() {
        Builder b = Model.builder();
        b.hull(-0.55, 0.55, -1.8, 2.2, 0.55, -0.45, 0.45, -1.6, 2.0, 1.45, Mat.PAINT, Mat.PAINT);
        b.hull(-0.45, 0.45, -5.2, -1.8, 0.75, -0.12, 0.12, -5.4, -1.8, 1.35, Mat.PAINT, Mat.PAINT);
        b.cyl(0, 1.0, 2.2, 'z', 0.5, 0.18, 0.45, 12, Mat.PAINT_DARK, Mat.METAL);           // cowling
        b.hull(-0.4, 0.4, -1.4, 1.0, 1.45, -0.3, 0.3, -1.1, 0.7, 2.15, Mat.CANOPY, Mat.CANOPY);
        b.box(-4.6, 0.75, 0.0, 4.6, 0.88, 1.3, Mat.PAINT);                                  // wings
        b.pair(4.0, 0.72, 0.0, 4.6, 0.9, 1.3, Mat.STRIPE);
        b.box(-1.6, 1.15, -5.3, 1.6, 1.22, -4.6, Mat.PAINT);                                 // tailplane
        b.hull(-0.05, 0.05, -5.4, -4.4, 1.3, -0.04, 0.04, -5.5, -5.0, 2.3, Mat.PAINT, Mat.PAINT);
        b.pair(1.5, 0.88, 0.9, 1.7, 1.05, 1.5, Mat.BLACK);                                  // guns
        b.pair(4.4, 0.78, 1.28, 4.6, 0.85, 1.33, Mat.HEADLIGHT);
        b.pair(0.9, 0.0, 0.7, 1.0, 0.75, 0.9, Mat.METAL);                                   // gear legs
        b.wheel(0.95, 0.22, 0.8, 0.22, 0.14, false);
        b.wheel(-0.95, 0.22, 0.8, 0.22, 0.14, false);
        b.wheel(0, 0.14, -4.9, 0.14, 0.08, false);
        b.box(-0.4, 0.6, -1.5, 0.4, 0.68, 1.0, Mat.CARPET);
        b.seat(0, 0.95, 0.3, 0.55, Mat.SEAT);
        b.seat(0, 0.95, -0.7, 0.55, Mat.SEAT);
        b.dashboard(0.4, -0.4, 1.5, 1.05, 0);
        b.group(Kind.PROP_Z, 0, 1.0, 2.7, 0, false);
        b.box(-0.1, -0.4 + 1.0, 2.65, 0.1, 2.4, 2.72, Mat.ROTOR);
        b.box(-0.1, 1.0 - 1.4, 2.65, 0.1, 1.0, 2.72, Mat.ROTOR);
        b.cyl(0, 1.0, 2.62, 'z', 0.16, 0.0, 0.35, 8, Mat.CHROME, Mat.CHROME);
        b.body();
        return b.build();
    }

    private static Model jet() {
        Builder b = Model.builder();
        b.hull(-0.65, 0.65, -4.0, 2.2, 0.55, -0.55, 0.55, -3.8, 2.0, 1.55, Mat.PAINT, Mat.PAINT);
        b.hull(-0.55, 0.55, 2.2, 3.4, 0.6, -0.15, 0.15, 3.4, 5.4, 1.15, Mat.PAINT, Mat.PAINT);     // nose
        b.cyl(0, 0.95, 5.4, 'z', 0.15, 0.0, 0.5, 8, Mat.METAL, Mat.METAL);
        b.hull(-0.45, 0.45, 0.6, 3.2, 1.55, -0.25, 0.25, 0.9, 2.6, 2.3, Mat.CANOPY, Mat.CANOPY);
        // delta wings and tail
        b.hull(-4.4, 4.4, -2.6, -1.8, 0.75, -0.6, 0.6, -2.4, 1.4, 0.85, Mat.PAINT, Mat.PAINT);
        b.pair(0.35, 1.5, -4.1, 0.45, 3.2, -3.0, Mat.PAINT);
        b.pair(0.5, 0.95, -4.2, 2.0, 1.02, -3.4, Mat.PAINT);
        // engines and afterburner flame
        b.cyl(0, 1.0, -4.0, 'z', 0.5, 0.45, -0.3, 12, Mat.METAL, Mat.BLACK);
        b.group(Kind.FLAME, 0, 1.0, -4.3, 0, false);
        b.cyl(0, 1.0, -4.3, 'z', 0.4, 0.0, -1.8, 10, Mat.FLAME, null);
        b.body();
        // missiles under the wings
        for (int sd = -1; sd <= 1; sd += 2) {
            b.cyl(sd * 1.6, 0.55, -0.6, 'z', 0.1, 0.1, 1.6, 8, Mat.WHITE, Mat.RED);
            b.box(sd * 1.6 - 0.03, 0.62, -0.2, sd * 1.6 + 0.03, 0.75, 0.2, Mat.METAL);
        }
        b.box(-0.06, 0.0, 3.0, 0.06, 0.6, 3.1, Mat.METAL);
        b.wheel(0, 0.2, 3.05, 0.2, 0.12, false);
        b.pair(0.8, 0.0, -1.6, 0.9, 0.6, -1.5, Mat.METAL);
        b.wheel(0.85, 0.24, -1.55, 0.24, 0.16, false);
        b.wheel(-0.85, 0.24, -1.55, 0.24, 0.16, false);
        b.seat(0, 1.15, 1.6, 0.55, Mat.SEAT);
        b.dashboard(0.45, -0.45, 1.75, 2.4, 0);
        return b.build();
    }

    private static Model rocket() {
        Builder b = Model.builder();
        double r = 1.15;
        b.cyl(0, 1.2, 0, 'y', r, r, 7.4, 16, Mat.PAINT, null);
        b.cyl(0, 8.6, 0, 'y', r, 0.0, 2.6, 16, Mat.RED, null);                  // nose cone
        b.cyl(0, 3.0, 0, 'y', r + 0.02, r + 0.02, 0.35, 16, Mat.BLACK, null);   // stripes
        b.cyl(0, 5.3, 0, 'y', r + 0.02, r + 0.02, 0.15, 16, Mat.RED, null);
        b.cyl(0, 1.2, 0, 'y', 0.8, 1.1, -0.9, 14, Mat.METAL, Mat.BLACK);        // engine bell
        // fins + landing legs
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            double cx = Math.sin(a), cz = Math.cos(a);
            b.hull(cx * r - 0.06, cx * r + 0.06, cz * r - 0.06, cz * r + 0.06, 0.4,
                    cx * (r + 1.0) - 0.06, cx * (r + 1.0) + 0.06, cz * (r + 1.0) - 0.06, cz * (r + 1.0) + 0.06, 0.0, Mat.PAINT, Mat.PAINT);
            b.hull(cx * r - 0.05, cx * r + 0.05, cz * r - 0.5, cz * r + 0.5, 3.0,
                    cx * (r + 0.9) - 0.05, cx * (r + 0.9) + 0.05, cz * (r + 0.9) - 0.4, cz * (r + 0.9) + 0.4, 1.0, Mat.RED, Mat.RED);
        }
        // windows ring around the cabin, cabin floors and seats
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            for (double wy : new double[]{7.15, 8.35}) {
                double x = Math.sin(a) * (r - 0.12), z = Math.cos(a) * (r - 0.12);
                b.box(x - 0.2, wy - 0.2, z - 0.2, x + 0.2, wy + 0.2, z + 0.2, Mat.GLASS);
            }
        }
        b.cyl(0, 6.35, 0, 'y', r - 0.05, r - 0.05, 0.08, 14, Mat.METAL, Mat.CARPET);
        b.cyl(0, 7.95, 0, 'y', r - 0.05, r - 0.05, 0.08, 14, Mat.METAL, Mat.CARPET);
        b.seat(0, 8.0, 0.3, 0.6, Mat.SEAT);
        b.seat(0.5, 6.4, 0.0, 0.5, Mat.SEAT);
        b.seat(-0.5, 6.4, 0.0, 0.5, Mat.SEAT);
        b.dashboard(0.6, -0.6, 8.75, 0.75, 0);
        b.box(-0.4, 1.2, r, 0.4, 2.6, r + 0.03, Mat.METAL);                    // hatch
        b.group(Kind.FLAME, 0, 0.3, 0, 0, false);
        b.cyl(0, 0.3, 0, 'y', 0.95, 0.2, -3.5, 12, Mat.FLAME, null);
        b.body();
        return b.build();
    }

    // ============================================================= RV, rail, sea

    private static Model rv() {
        Builder b = Model.builder();
        double w = 1.3;
        b.box(-w, 0.6, -4.4, w, 3.2, 2.3, Mat.PAINT);
        b.box(-w, 0.6, 2.3, w, 1.55, 4.3, Mat.PAINT);                               // cab nose
        b.hull(-w, w, 2.3, 4.3, 1.55, -w + 0.05, w - 0.05, 2.3, 2.9, 2.75, Mat.PAINT, Mat.PAINT);
        b.box(-w, 2.75, 2.3, w, 3.2, 3.3, Mat.PAINT);                              // over-cab bunk
        b.pane(w - 0.04, -w + 0.04, 1.6, 4.25, w - 0.06, -w + 0.06, 2.7, 2.95, Mat.GLASS);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * (w + 0.005), 1.6, 2.6, 2.4, 4.0, 2.4, 3.0, Mat.GLASS);
            b.sidePane(sd * (w + 0.005), 1.7, 2.5, -3.8, -2.2, -3.8, -2.2, Mat.GLASS);
            b.sidePane(sd * (w + 0.005), 1.7, 2.5, -1.4, 0.3, -1.4, 0.3, Mat.GLASS);
            b.box(sd > 0 ? w : -w - 0.01, 0.9, -2.0, sd > 0 ? w + 0.01 : -w, 0.95, 2.2, Mat.STRIPE);
            b.box(sd > 0 ? w : -w - 0.2, 1.8, 3.6, sd > 0 ? w + 0.2 : -w, 2.2, 3.75, Mat.BLACK);
        }
        b.box(w, 0.7, 0.5, w + 0.01, 2.4, 1.3, Mat.WOOD);                          // entry door (left side)
        b.box(w + 0.01, 1.5, 0.6, w + 0.03, 1.6, 0.75, Mat.CHROME);
        b.box(w, 2.9, -3.0, w + 0.35, 3.0, 1.4, Mat.STRIPE);                       // awning
        b.box(-0.6, 3.2, -1.5, 0.6, 3.55, -0.5, Mat.WHITE);                         // roof AC
        b.box(-1.0, 3.2, -4.0, -0.6, 3.25, -2.0, Mat.SCREEN);                       // solar panel
        b.pair(0.95, 1.0, 4.3, 1.25, 1.3, 4.35, Mat.HEADLIGHT);
        b.box(-0.8, 0.7, 4.3, 0.8, 1.3, 4.34, Mat.GRILLE);
        b.box(-w, 0.55, 4.3, w, 0.75, 4.45, Mat.CHROME);
        b.pair(1.0, 1.0, -4.43, 1.25, 1.8, -4.4, Mat.TAILLIGHT);
        b.box(-0.4, 0.8, -4.45, 0.4, 2.8, -4.4, Mat.CHROME);                       // ladder
        b.cyl(0, 1.3, -4.45, 'z', 0.42, 0.42, -0.25, 12, Mat.TIRE, Mat.RUBBER);    // spare
        // interior: cab seats + dash, lounge visible through the windows
        b.box(-w + 0.05, 0.62, -4.3, w - 0.05, 0.7, 4.0, Mat.CARPET);
        b.seat(0.55, 1.4, 3.2, 0.65, Mat.SEAT);
        b.seat(-0.55, 1.4, 3.2, 0.65, Mat.SEAT);
        b.seat(0.55, 1.4, 1.4, 0.65, Mat.SEAT);
        b.seat(-0.55, 1.4, 1.4, 0.65, Mat.SEAT);
        b.dashboard(w - 0.1, -w + 0.1, 1.8, 4.0, 0.55);
        b.steeringWheel(0.55, 1.85, 3.75, 0.22, 35);
        b.box(-w + 0.1, 0.7, -3.6, -0.2, 1.3, -1.8, Mat.SEAT);                     // sofa
        b.box(-0.1, 1.2, -3.2, 0.7, 1.28, -2.2, Mat.WOOD);                         // table
        b.box(0.25, 0.7, -2.8, 0.35, 1.2, -2.6, Mat.METAL);
        b.box(-w + 0.1, 0.7, -0.4, -0.5, 1.6, 0.6, Mat.WOOD);                      // kitchen
        b.wheels(1.1, 0.55, 3.2, -2.6, 0.55, 0.4);
        return b.build();
    }

    private static Model locomotive() {
        Builder b = Model.builder();
        // frame, boiler, smokebox, chimney, dome
        b.box(-1.15, 0.7, -3.6, 1.15, 1.0, 3.6, Mat.BLACK);
        b.cyl(0, 1.9, -1.2, 'z', 0.9, 0.9, 4.2, 16, Mat.PAINT, Mat.BLACK);
        b.cyl(0, 1.9, 3.0, 'z', 0.95, 0.95, 0.4, 16, Mat.BLACK, Mat.BLACK);
        b.cyl(0, 2.6, 2.4, 'y', 0.25, 0.32, 1.2, 10, Mat.SMOKESTACK, Mat.BLACK);
        b.cyl(0, 2.7, 0.8, 'y', 0.35, 0.3, 0.45, 10, Mat.BRASS, Mat.BRASS);
        b.cyl(0, 2.7, -0.3, 'y', 0.25, 0.2, 0.3, 10, Mat.BRASS, Mat.BRASS);
        for (double z = -1.0; z < 3.0; z += 1.0) b.cyl(0, 1.9, z, 'z', 0.92, 0.92, 0.08, 16, Mat.BRASS, null);
        b.box(-0.25, 2.2, 3.42, 0.25, 2.55, 3.5, Mat.HEADLIGHT);
        b.hull(-1.1, 1.1, 3.4, 4.1, 0.15, -1.1, 1.1, 3.4, 3.5, 1.0, Mat.RED, Mat.RED);      // cowcatcher
        // cab
        b.box(-1.2, 1.0, -3.4, 1.2, 3.1, -1.2, Mat.PAINT);
        b.box(-1.3, 3.1, -3.6, 1.3, 3.25, -1.0, Mat.BLACK);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * 1.205, 2.0, 2.8, -2.9, -1.7, -2.9, -1.7, Mat.GLASS);
            b.box(sd > 0 ? 1.15 : -1.25, 1.2, -1.2, sd > 0 ? 1.25 : -1.15, 3.1, -1.1, Mat.BLACK);
        }
        b.box(-0.9, 2.2, -1.21, -0.3, 2.8, -1.19, Mat.GLASS);
        b.box(0.3, 2.2, -1.21, 0.9, 2.8, -1.19, Mat.GLASS);
        b.box(-1.1, 1.0, -3.4, 1.1, 1.08, -1.25, Mat.WOOD);
        b.box(-0.7, 1.08, -1.6, 0.7, 2.6, -1.25, Mat.METAL);                         // firebox / backhead
        b.box(-0.2, 1.3, -1.62, 0.2, 1.7, -1.6, Mat.FLAME);
        b.gauge(0.4, 2.3, -1.62, 0.2);
        b.seat(0.3, 1.4, -1.8, 0.45, Mat.SEAT);
        b.seat(-0.3, 1.4, -1.8, 0.45, Mat.SEAT);
        // tender (coal)
        b.box(-1.15, 1.0, -6.3, 1.15, 2.4, -3.7, Mat.PAINT);
        b.box(-1.0, 2.4, -6.1, 1.0, 2.6, -3.9, Mat.COAL);
        b.box(-1.15, 0.7, -6.3, 1.15, 1.0, -3.6, Mat.BLACK);
        // drivers + pilot + tender wheels, side rods
        for (int sd = -1; sd <= 1; sd += 2) {
            for (double z : new double[]{-0.6, 0.6, 1.8}) b.wheel(sd * 0.9, 0.62, z, 0.62, 0.15, false);
            b.wheel(sd * 0.9, 0.35, 3.0, 0.35, 0.12, false);
            for (double z : new double[]{-4.3, -5.6}) b.wheel(sd * 0.9, 0.35, z, 0.35, 0.12, false);
            b.box(sd > 0 ? 1.0 : -1.06, 0.5, -0.7, sd > 0 ? 1.06 : -1.0, 0.6, 1.9, Mat.METAL);
            b.cyl(sd * 0.95, 0.9, 2.2, 'z', 0.25, 0.25, 0.9, 8, Mat.BLACK, Mat.METAL);  // cylinders
        }
        return b.build();
    }

    private static Model carriage() {
        Builder b = Model.builder();
        double w = 1.2;
        b.box(-w, 0.7, -3.0, w, 1.0, 3.0, Mat.BLACK);
        b.box(-w, 1.0, -3.0, w, 2.0, 3.0, Mat.PAINT);
        b.box(-w, 2.0, -3.0, w, 2.7, 3.0, Mat.WHITE);
        b.hull(-w, w, -3.0, 3.0, 2.7, -w + 0.4, w - 0.4, -2.9, 2.9, 3.1, Mat.PAINT_DARK, Mat.PAINT_DARK);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (double z = -2.4; z < 2.6; z += 1.2) b.sidePane(sd * (w + 0.005), 2.05, 2.6, z, z + 0.8, z, z + 0.8, Mat.GLASS);
            b.box(sd > 0 ? w : -w - 0.01, 1.85, -3.0, sd > 0 ? w + 0.01 : -w, 1.95, 3.0, Mat.BRASS);
        }
        b.box(-w + 0.05, 1.0, -2.9, w - 0.05, 1.05, 2.9, Mat.CARPET);
        for (double z : new double[]{1.3, -1.3}) {
            b.seat(0.35, 1.35, z, 0.55, Mat.SEAT);
            b.seat(-0.35, 1.35, z, 0.55, Mat.SEAT);
        }
        for (double z : new double[]{2.2, -2.2}) {
            for (int sd = -1; sd <= 1; sd += 2) {
                b.wheel(sd * 0.85, 0.35, z - 0.4, 0.35, 0.12, false);
                b.wheel(sd * 0.85, 0.35, z + 0.4, 0.35, 0.12, false);
            }
        }
        b.box(-0.3, 0.8, 3.0, 0.3, 1.0, 3.3, Mat.METAL);
        b.box(-0.3, 0.8, -3.3, 0.3, 1.0, -3.0, Mat.METAL);
        return b.build();
    }

    private static Model speedboat() {
        Builder b = Model.builder();
        double w = 1.0;
        b.hull(-0.35, 0.35, -2.3, 2.0, -0.35, -w, w, -2.4, 2.4, 0.35, Mat.WHITE, Mat.DECK);
        b.hull(-0.3, 0.3, 2.0, 2.3, -0.3, -0.5, 0.5, 2.4, 3.1, 0.35, Mat.WHITE, Mat.DECK);       // bow
        b.box(-w, 0.35, -2.4, w, 0.75, 1.0, Mat.PAINT);                                          // sides
        b.box(-w + 0.08, 0.15, -2.3, w - 0.08, 0.36, 0.9, Mat.CARPET);
        b.pair(w - 0.01, 0.15, -2.4, w + 0.005, 0.25, 2.4, Mat.STRIPE);
        b.pane(w - 0.1, -w + 0.1, 0.75, 0.9, w - 0.15, -w + 0.15, 1.15, 0.6, Mat.GLASS);
        b.seat(0.4, 0.55, 0.2, 0.55, Mat.SEAT);
        b.seat(-0.4, 0.55, 0.2, 0.55, Mat.SEAT);
        b.seat(0, 0.55, -1.0, 1.6, Mat.SEAT);
        b.box(0.2, 0.55, 0.65, 0.75, 0.9, 0.95, Mat.DASH);
        b.steeringWheel(0.4, 0.9, 0.6, 0.15, 35);
        b.gauge(0.55, 0.85, 0.64, 0.12);
        b.box(-0.25, 0.1, -2.7, 0.25, 1.0, -2.4, Mat.BLACK);                                       // outboard
        b.box(-0.08, -0.5, -2.65, 0.08, 0.1, -2.5, Mat.METAL);
        b.pair(0.85, 0.75, 1.0, 0.9, 0.95, 2.2, Mat.CHROME);
        return b.build();
    }

    private static Model yacht() {
        Builder b = Model.builder();
        double w = 1.6;
        b.hull(-0.6, 0.6, -4.6, 4.0, -0.6, -w, w, -4.8, 4.6, 0.5, Mat.WHITE, Mat.DECK);
        b.hull(-0.5, 0.5, 4.0, 4.6, -0.5, -0.8, 0.8, 4.6, 6.0, 0.5, Mat.WHITE, Mat.DECK);
        b.pair(w - 0.01, 0.0, -4.8, w + 0.005, 0.2, 4.6, Mat.NAVY);
        // main cabin with tinted windows
        b.box(-1.3, 0.5, -2.2, 1.3, 1.5, 2.4, Mat.WHITE);
        b.hull(-1.3, 1.3, 2.4, 3.2, 0.5, -1.25, 1.25, 2.4, 2.6, 1.5, Mat.WHITE, Mat.WHITE);
        for (int sd = -1; sd <= 1; sd += 2) b.sidePane(sd * 1.305, 0.8, 1.35, -1.9, 2.3, -1.9, 2.3, Mat.GLASS);
        b.pane(1.2, -1.2, 0.8, 3.15, 1.15, -1.15, 1.4, 2.55, Mat.GLASS);
        // flybridge
        b.box(-1.2, 1.5, -1.0, 1.2, 1.6, 2.0, Mat.DECK);
        b.pair(1.15, 1.6, -1.0, 1.2, 2.2, 2.0, Mat.CHROME);
        b.box(-1.2, 1.6, 1.95, 1.2, 2.2, 2.0, Mat.CHROME);
        b.seat(0.5, 2.15, 1.3, 0.6, Mat.SEAT);
        b.seat(-0.5, 2.15, 1.3, 0.6, Mat.SEAT);
        b.box(0.2, 1.6, 1.65, 0.85, 2.1, 1.95, Mat.DASH);
        b.steeringWheel(0.5, 2.1, 1.6, 0.18, 35);
        b.cyl(0, 1.6, -0.6, 'y', 0.06, 0.06, 1.6, 6, Mat.WHITE, Mat.WHITE);             // radar mast
        b.box(-0.5, 3.15, -0.7, 0.5, 3.25, -0.5, Mat.BLACK);
        // aft deck with seating and rails
        b.seat(0.8, 1.05, -2.0, 0.6, Mat.SEAT);
        b.seat(-0.8, 1.05, -2.0, 0.6, Mat.SEAT);
        b.seat(0, 1.05, -3.0, 2.0, Mat.SEAT);
        b.pair(w - 0.05, 0.5, -4.8, w, 1.15, 4.4, Mat.CHROME);
        b.box(-0.8, 0.45, 5.0, 0.8, 0.55, 5.9, Mat.DECK);
        return b.build();
    }

    // ================================================================ wave 5

    private static Model houseboat() {
        Builder b = Model.builder();
        double w = 1.6;
        b.hull(-w + 0.15, w - 0.15, -4.5, 4.4, -0.55, -w, w, -4.7, 4.65, 0.3, Mat.WHITE, Mat.DECK);   // pontoon deck
        b.pair(w - 0.01, -0.25, -4.7, w + 0.005, -0.05, 4.65, Mat.NAVY);
        // the house: painted siding, white trim, a little roof deck
        b.box(-1.45, 0.3, -2.8, 1.45, 2.55, 2.4, Mat.PAINT);
        b.box(-1.65, 2.55, -3.4, 1.65, 2.7, 3.0, Mat.WHITE);
        b.pair(1.45, 0.3, -2.85, 1.5, 2.55, -2.75, Mat.WHITE);
        b.pair(1.45, 0.3, 2.35, 1.5, 2.55, 2.45, Mat.WHITE);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * 1.455, 1.2, 2.1, -2.3, -0.9, -2.3, -0.9, Mat.GLASS);
            b.sidePane(sd * 1.455, 1.2, 2.1, -0.2, 1.8, -0.2, 1.8, Mat.GLASS);
            b.box(sd > 0 ? 1.45 : -1.6, 1.1, -2.4, sd > 0 ? 1.6 : -1.45, 1.17, -0.8, Mat.WOOD);   // flower boxes
            b.box(sd > 0 ? 1.45 : -1.6, 1.17, -2.35, sd > 0 ? 1.58 : -1.47, 1.3, -0.85, Mat.RUG);
        }
        b.pane(1.3, 0.25, 1.2, 2.405, 1.3, 0.25, 2.1, 2.405, Mat.GLASS);                      // front windows
        b.pane(-0.25, -1.3, 1.2, 2.405, -0.25, -1.3, 2.1, 2.405, Mat.GLASS);
        b.box(-0.2, 0.3, 2.4, 0.2, 2.2, 2.43, Mat.WOOD);                                       // door
        b.box(0.08, 1.2, 2.43, 0.14, 1.3, 2.46, Mat.BRASS);
        b.pane(1.0, -1.0, 1.2, -2.805, 1.0, -1.0, 2.0, -2.805, Mat.GLASS);                     // back window
        b.cyl(1.0, 2.7, -1.6, 'y', 0.09, 0.09, 0.7, 6, Mat.BLACK, Mat.BLACK);                 // stove pipe
        b.box(0.9, 3.4, -1.7, 1.1, 3.45, -1.5, Mat.BLACK);
        // roof deck rails, deck chairs on top
        b.pair(1.6, 2.7, -3.4, 1.65, 3.25, 3.0, Mat.WHITE);
        b.box(-1.65, 2.7, -3.45, 1.65, 3.25, -3.4, Mat.WHITE);
        b.box(-1.3, 2.7, 0.4, -0.6, 3.0, 1.2, Mat.CANVAS);
        b.box(0.6, 2.7, 0.4, 1.3, 3.0, 1.2, Mat.CANVAS);
        // front porch: helm and seats, railings
        b.box(-w + 0.05, 0.3, 2.45, w - 0.05, 0.34, 4.5, Mat.WOOD);
        b.seat(0.6, 1.25, 3.3, 0.6, Mat.SEAT);
        b.seat(-0.6, 1.25, 3.3, 0.6, Mat.SEAT);
        b.box(0.3, 0.3, 3.85, 0.9, 1.35, 4.15, Mat.DASH);
        b.steeringWheel(0.6, 1.4, 3.8, 0.2, 35);
        b.pair(w - 0.05, 0.3, 2.45, w, 1.0, 4.55, Mat.WHITE);
        b.pair(0.9, 0.3, 4.5, w, 1.0, 4.55, Mat.WHITE);
        // back deck: chairs, a grill, a life ring, the outboard
        b.box(-w + 0.05, 0.3, -4.6, w - 0.05, 0.34, -2.8, Mat.WOOD);
        b.seat(0.8, 1.25, -3.6, 0.6, Mat.SEAT);
        b.seat(-0.8, 1.25, -3.6, 0.6, Mat.SEAT);
        b.cyl(0, 0.34, -4.3, 'y', 0.22, 0.22, 0.75, 8, Mat.BLACK, Mat.BLACK);
        b.cyl(1.5, 1.6, -2.0, 'x', 0.32, 0.32, 0.07, 12, Mat.RED, Mat.WHITE);
        b.pair(w - 0.05, 0.3, -4.65, w, 1.0, -2.8, Mat.WHITE);
        b.box(-0.25, -0.5, -5.05, 0.25, 0.7, -4.7, Mat.BLACK);
        b.box(-0.1, -0.7, -5.0, 0.1, -0.5, -4.8, Mat.METAL);
        return b.build();
    }

    /** a bare chassis with a steel deck at y = 0.85 for building on (see VehicleEntity.DECK) */
    private static Model buildFrame() {
        Builder b = Model.builder();
        b.pair(0.85, 0.45, -2.6, 1.05, 0.7, 2.6, Mat.PAINT);                                 // frame rails
        for (double z = -2.3; z <= 2.31; z += 1.15) b.box(-0.85, 0.5, z - 0.08, 0.85, 0.65, z + 0.08, Mat.PAINT);
        b.box(-1.5, 0.75, -2.5, 1.5, 0.85, 2.5, Mat.METAL, Mat.STEEL);                        // deck plate
        b.pair(1.45, 0.7, -2.55, 1.52, 0.86, 2.55, Mat.PAINT_DARK);
        b.box(-1.52, 0.7, 2.5, 1.52, 0.86, 2.58, Mat.PAINT_DARK);
        b.box(-1.52, 0.7, -2.58, 1.52, 0.86, -2.5, Mat.PAINT_DARK);
        b.seat(0, 1.3, 1.95, 0.6, Mat.SEAT);
        b.box(-0.05, 0.85, 2.3, 0.05, 1.55, 2.4, Mat.BLACK);                                  // steering column
        b.steeringWheel(0, 1.6, 2.3, 0.2, 40);
        b.gauge(0.2, 1.5, 2.36, 0.1);
        b.box(-0.6, 0.35, -2.4, 0.6, 0.75, -1.4, Mat.METAL);                                  // engine under the deck
        b.cyl(0.4, 0.4, -2.4, 'z', 0.06, 0.06, -0.4, 8, Mat.CHROME, Mat.BLACK);
        b.pair(1.25, 0.5, 2.58, 1.45, 0.7, 2.62, Mat.HEADLIGHT);
        b.pair(1.25, 0.5, -2.62, 1.45, 0.7, -2.58, Mat.TAILLIGHT);
        b.wheels(1.25, 0.42, 1.85, -1.85, 0.42, 0.34);
        return b.build();
    }

    private static Model sailboat() {
        Builder b = Model.builder();
        b.hull(-0.35, 0.35, -2.6, 2.3, -0.55, -1.0, 1.0, -2.8, 2.6, 0.3, Mat.WHITE, Mat.DECK);
        b.hull(-0.05, 0.05, 2.3, 2.9, -0.3, -0.6, 0.6, 2.6, 3.0, 0.3, Mat.WHITE, Mat.DECK);
        b.pair(0.99, 0.05, -2.8, 1.0, 0.18, 2.6, Mat.PAINT);
        b.box(-0.08, -1.2, -0.6, 0.08, -0.5, 0.6, Mat.PAINT);                                   // keel
        b.box(-0.6, 0.3, -2.5, 0.6, 0.32, -0.4, Mat.WOOD);                                      // cockpit
        b.pair(0.45, 0.3, -2.5, 0.7, 0.6, -0.4, Mat.WOOD);
        b.box(-0.5, 0.3, -0.3, 0.5, 0.75, 1.6, Mat.WHITE);                                      // cuddy cabin
        b.pane(0.45, -0.45, 0.45, 1.605, 0.45, -0.45, 0.7, 1.605, Mat.GLASS);
        b.cyl(0, 0.3, 0.6, 'y', 0.07, 0.05, 6.4, 8, Mat.WHITE, Mat.WHITE);                     // mast
        b.box(-0.04, 1.25, -2.2, 0.04, 1.33, 0.6, Mat.WOOD);                                    // boom
        double[] m0 = v(0.012, 1.35, 0.55), m1 = v(0.012, 6.5, 0.55), m2 = v(0.012, 1.35, -2.15);
        b.quad(m0, m1, m1, m2, Mat.CANVAS);                                                      // mainsail, both sides
        b.quad(v(-0.012, 1.35, 0.55), v(-0.012, 1.35, -2.15), v(-0.012, 6.5, 0.55), v(-0.012, 6.5, 0.55), Mat.CANVAS);
        double[] j0 = v(0.01, 5.8, 0.65), j1 = v(0.01, 0.45, 2.8), j2 = v(0.01, 0.45, 0.75);
        b.quad(j0, j1, j1, j2, Mat.CANVAS);                                                      // jib
        b.quad(v(-0.01, 5.8, 0.65), v(-0.01, 0.45, 0.75), v(-0.01, 0.45, 2.8), v(-0.01, 0.45, 2.8), Mat.CANVAS);
        b.box(-0.03, 0.3, -2.75, 0.03, 0.9, -2.65, Mat.WOOD);                                   // tiller
        b.box(-0.03, 0.85, -2.7, 0.03, 0.9, -1.9, Mat.WOOD);
        b.pair(0.95, 0.3, -2.7, 1.0, 0.65, 2.5, Mat.CHROME);
        return b.build();
    }

    private static Model armoredTruck() {
        Builder b = Model.builder();
        truckCab(b, 1.2, 1.3, 2.3, 0.9, 2.6, 0.9);
        b.box(-1.2, 0.5, -3.45, 1.2, 2.95, 1.25, Mat.PAINT);                                    // armoured box
        b.box(-1.15, 2.95, -3.35, 1.15, 3.05, 1.15, Mat.PAINT_DARK);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.sidePane(sd * 1.205, 2.0, 2.2, -2.6, -2.0, -2.6, -2.0, Mat.GLASS);                // gun-slit windows
            b.sidePane(sd * 1.205, 2.0, 2.2, -0.8, -0.2, -0.8, -0.2, Mat.GLASS);
            b.box(sd > 0 ? 1.2 : -1.215, 0.9, -3.3, sd > 0 ? 1.215 : -1.2, 0.98, 1.1, Mat.BLACK);
            for (double z = -3.2; z <= 1.0; z += 0.6) b.box(sd > 0 ? 1.2 : -1.22, 2.75, z, sd > 0 ? 1.22 : -1.2, 2.8, z + 0.05, Mat.CHROME);   // rivets
        }
        b.box(-1.1, 0.6, -3.5, -0.02, 2.8, -3.45, Mat.PAINT_DARK);                              // rear doors
        b.box(0.02, 0.6, -3.5, 1.1, 2.8, -3.45, Mat.PAINT_DARK);
        b.pair(0.1, 1.5, -3.55, 0.2, 1.9, -3.5, Mat.CHROME);
        b.pair(0.8, 1.6, -3.53, 1.05, 1.9, -3.5, Mat.TAILLIGHT);
        b.box(-0.5, 3.05, -0.6, 0.5, 3.25, 0.4, Mat.METAL);                                      // roof hatch
        b.box(-1.2, 0.45, 3.15, 1.2, 0.8, 3.45, Mat.BLACK);                                      // push bar
        b.wheels(1.05, 0.52, 2.5, -2.35, 0.52, 0.42);
        return b.build();
    }

    private static Model policeBike() {
        Model m = motorbike(false);
        Builder b = Model.builder();
        b.pair(0.18, 0.55, -0.95, 0.34, 0.9, -0.45, Mat.WHITE);                               // panniers
        b.box(-0.2, 1.15, 0.85, 0.2, 1.5, 0.9, Mat.GLASS);                                    // windshield
        b.group(Kind.SIREN_A, 0, 0, 0, 0, false);
        b.box(0.08, 1.0, -1.05, 0.2, 1.12, -0.98, Mat.SIREN_RED);
        b.box(0.14, 1.0, 0.86, 0.22, 1.08, 0.92, Mat.SIREN_RED);
        b.group(Kind.SIREN_B, 0, 0, 0, 0, false);
        b.box(-0.2, 1.0, -1.05, -0.08, 1.12, -0.98, Mat.SIREN_BLUE);
        b.box(-0.22, 1.0, 0.86, -0.14, 1.08, 0.92, Mat.SIREN_BLUE);
        b.body();
        m.groups.addAll(b.build().groups);
        return m;
    }

    private static Model rally() {
        CarSpec s = new CarSpec(4.2, 0.9, 0.38, 1.0, 1.9, 0.8, 0.3, -1.3, -1.6,
                0.36, 0.76, 1.3, -1.3, 0.7, 0.05, -0.85, 0.7);
        Builder b = carShell(s, Mat.GLASS);
        b.box(-0.85, 1.6, -2.15, 0.85, 1.66, -1.85, Mat.PAINT_DARK);                          // wing
        b.pair(0.7, 1.0, -2.1, 0.76, 1.6, -1.95, Mat.PAINT_DARK);
        b.box(-0.2, 1.9, -0.5, 0.2, 2.02, 0.1, Mat.BLACK);                                    // roof scoop
        b.box(-0.7, 1.05, 2.1, 0.7, 1.35, 2.18, Mat.BLACK);                                   // light pod
        for (double x = -0.55; x <= 0.56; x += 0.37) b.box(x - 0.12, 1.1, 2.18, x + 0.12, 1.3, 2.2, Mat.HEADLIGHT);
        b.pair(0.75, 0.15, 1.0, 0.95, 0.45, 1.04, Mat.BLACK);                                 // mud flaps
        b.pair(0.75, 0.15, -1.62, 0.95, 0.45, -1.58, Mat.BLACK);
        b.pair(0.9, 0.5, -0.6, 0.905, 0.95, 0.3, Mat.WHITE);                                  // door number roundel
        b.box(-0.9, 1.0, 1.4, 0.9, 1.006, 1.6, Mat.WHITE);                                    // stripe
        return b.build();
    }

    // ================================================================ wave 6

    private static Model fishingBoat() {
        Builder b = Model.builder();
        double w = 1.3;
        b.hull(-0.5, 0.5, -3.4, 3.2, -0.65, -w, w, -3.6, 3.8, 0.4, Mat.PAINT, Mat.DECK);
        b.hull(-0.1, 0.1, 3.2, 3.9, -0.4, -0.7, 0.7, 3.8, 4.1, 0.4, Mat.PAINT, Mat.DECK);
        b.pair(w - 0.01, 0.2, -3.6, w + 0.005, 0.4, 3.8, Mat.WHITE);
        b.pair(w - 0.05, 0.4, -3.6, w, 0.9, 3.8, Mat.PAINT_DARK);                          // gunwales
        // wheelhouse forward
        b.box(-0.95, 0.4, 0.6, 0.95, 2.3, 2.5, Mat.WHITE);
        b.box(-1.05, 2.3, 0.5, 1.05, 2.4, 2.6, Mat.PAINT_DARK);
        b.pane(0.85, -0.85, 1.5, 2.505, 0.85, -0.85, 2.15, 2.505, Mat.GLASS);
        for (int sd = -1; sd <= 1; sd += 2) b.sidePane(sd * 0.955, 1.5, 2.15, 0.9, 2.2, 0.9, 2.2, Mat.GLASS);
        b.box(-0.3, 0.4, 0.595, 0.3, 2.0, 0.6, Mat.WOOD);                                  // door aft
        b.seat(0.4, 1.35, 1.5, 0.5, Mat.SEAT);
        b.box(0.1, 0.4, 2.1, 0.8, 1.55, 2.4, Mat.DASH);
        b.steeringWheel(0.4, 1.6, 2.05, 0.2, 30);
        b.cyl(0, 2.4, 1.5, 'y', 0.05, 0.05, 1.4, 6, Mat.WHITE, Mat.WHITE);                // mast and lights
        b.box(-0.4, 3.5, 1.45, 0.4, 3.55, 1.55, Mat.WHITE);
        b.box(-0.06, 3.8, 1.44, 0.06, 3.9, 1.56, Mat.HEADLIGHT);
        // working deck aft: A-frame, winch, nets, crates, a bench
        b.pair(1.0, 0.4, -3.4, 1.12, 3.0, -3.25, Mat.YELLOW);
        b.box(-1.12, 2.9, -3.4, 1.12, 3.05, -3.25, Mat.YELLOW);
        b.cyl(0, 0.75, -1.0, 'x', 0.3, 0.3, 0.9, 10, Mat.METAL, Mat.BLACK);
        b.box(-0.45, 0.4, -1.3, 0.45, 0.6, -0.7, Mat.METAL);
        b.box(-1.1, 0.4, -3.3, -0.2, 0.75, -2.2, Mat.CANVAS);                              // net pile
        b.box(0.3, 0.4, -2.6, 0.9, 0.85, -2.0, Mat.WOOD);
        b.box(0.35, 0.85, -2.5, 0.85, 1.25, -2.05, Mat.WOOD);
        b.seat(0.7, 0.6, -1.8, 0.4, Mat.SEAT);
        b.seat(-0.7, 0.6, -1.8, 0.4, Mat.SEAT);
        b.cyl(w + 0.02, 0.9, -0.2, 'x', 0.3, 0.3, 0.07, 12, Mat.ORANGE, Mat.WHITE);       // life ring
        return b.build();
    }

    private static Model cargoPlane() {
        Builder b = Model.builder();
        double r = 1.9, cy = 2.6;
        tube(b, 0, cy, -6.0, 13.5, r, r, 20, i -> i == 5 || i == 14 ? Mat.PAINT_DARK : Mat.PAINT, null);
        tube(b, 0, cy, 7.5, 2.6, r, 0.3, 20, i -> i == 1 || i == 2 || i == 3 || i == 17 || i == 18 || i == 19 ? Mat.CANOPY : Mat.PAINT, Mat.METAL);
        tube(b, 0, cy + 0.6, -6.0, -4.2, r, 0.35, 20, i -> Mat.PAINT, Mat.METAL);           // upswept tail
        b.box(-1.6, 0.85, -6.0, 1.6, 0.95, 7.8, Mat.METAL);                                   // cargo floor with roller tracks
        b.pair(0.5, 0.95, -6.0, 0.6, 1.0, 7.0, Mat.STEEL);
        b.box(-1.2, 0.95, 2.0, 1.2, 2.2, 4.0, Mat.CANVAS);                                    // pallets under tarps
        b.box(-1.2, 0.95, -1.5, 1.2, 1.9, 1.0, Mat.WOOD);
        b.box(-1.5, 0.9, -6.3, 1.5, 1.05, -5.9, Mat.PAINT_DARK);                              // the ramp
        b.seat(0.55, 1.85, 8.0, 0.5, Mat.SEAT);
        b.seat(-0.55, 1.85, 8.0, 0.5, Mat.SEAT);
        b.dashboard(1.0, -1.0, 2.6, 8.8, 0.55);
        // high wing with four turboprops
        plate(b, 1.0, 4.35, -0.5, 2.6, 13.5, 4.5, 0.4, 1.8, 0.2, Mat.PAINT, true);
        for (double x : new double[]{-8.5, -4.4, 4.4, 8.5}) {
            b.cyl(x, 4.0, -0.2, 'z', 0.5, 0.42, 3.4, 12, Mat.PAINT, Mat.BLACK);
            b.group(Kind.PROP_Z, x, 4.0, 3.3, 0, false);
            b.box(x - 0.12, 4.0 - 1.7, 3.28, x + 0.12, 4.0 + 1.7, 3.34, Mat.ROTOR);
            b.box(x - 1.7, 3.88, 3.28, x + 1.7, 4.12, 3.34, Mat.ROTOR);
            b.cyl(x, 4.0, 3.2, 'z', 0.22, 0.0, 0.4, 8, Mat.METAL, Mat.METAL);
            b.body();
        }
        // T-tail
        b.hull(-0.12, 0.12, -10.0, -6.0, 4.2, -0.08, 0.08, -10.6, -8.8, 8.6, Mat.PAINT, Mat.PAINT);
        plate(b, 0.1, 8.5, -10.6, -9.0, 4.6, 8.6, -10.9, -10.0, 0.12, Mat.PAINT, true);
        b.box(-0.05, 8.6, -10.5, 0.05, 8.7, -10.3, Mat.TAILLIGHT);
        // landing gear in sponsons
        b.pair(1.6, 0.4, -2.0, 2.2, 1.4, 1.5, Mat.PAINT_DARK);
        b.wheel(1.9, 0.4, -1.2, 0.4, 0.3, false);
        b.wheel(-1.9, 0.4, -1.2, 0.4, 0.3, false);
        b.wheel(1.9, 0.4, 0.7, 0.4, 0.3, false);
        b.wheel(-1.9, 0.4, 0.7, 0.4, 0.3, false);
        b.box(-0.06, 0.4, 8.45, 0.06, 1.0, 8.55, Mat.METAL);
        b.wheel(0, 0.3, 8.5, 0.3, 0.2, true);
        return b.build();
    }

    private static Model drone() {
        Builder b = Model.builder();
        b.box(-0.25, 0.25, -0.3, 0.25, 0.45, 0.3, Mat.PAINT);                                 // body
        b.box(-0.18, 0.45, -0.2, 0.18, 0.5, 0.2, Mat.SCREEN);
        b.box(-0.08, 0.12, 0.2, 0.08, 0.25, 0.32, Mat.BLACK);                                  // camera gimbal
        b.box(-0.04, 0.15, 0.32, 0.04, 0.22, 0.34, Mat.GLASS);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                double x = sx * 0.55, z = sz * 0.55;
                b.box(Math.min(0, x) - 0.03, 0.36, Math.min(0, z) - 0.03, Math.max(0, x) + 0.03, 0.42, Math.max(0, z) + 0.03, Mat.PAINT_DARK);
                b.cyl(x, 0.42, z, 'y', 0.06, 0.06, 0.08, 8, Mat.METAL, Mat.METAL);
                b.box(x - 0.02, 0.0, z - 0.02, x + 0.02, 0.36, z + 0.02, Mat.BLACK);           // landing legs
                b.group(Kind.ROTOR_Y, x, 0.5, z, 0, false);
                b.box(x - 0.32, 0.5, z - 0.03, x + 0.32, 0.52, z + 0.03, Mat.ROTOR);
                b.box(x - 0.03, 0.5, z - 0.32, x + 0.03, 0.52, z + 0.32, Mat.ROTOR);
                b.body();
            }
        }
        b.pair(0.1, 0.3, 0.3, 0.16, 0.36, 0.31, Mat.HEADLIGHT);
        b.pair(0.1, 0.3, -0.31, 0.16, 0.36, -0.3, Mat.TAILLIGHT);
        return b.build();
    }

    private static Model militaryTruck() {
        Builder b = Model.builder();
        truckCab(b, 1.2, 1.4, 2.4, 1.0, 2.7, 1.0);
        b.box(-1.25, 0.9, -3.7, 1.25, 1.05, 1.35, Mat.PAINT_DARK);                             // bed
        b.pair(1.15, 1.05, -3.7, 1.25, 1.5, 1.35, Mat.PAINT);
        b.box(-1.25, 1.05, -3.75, 1.25, 1.5, -3.65, Mat.PAINT);
        // canvas tilt over hoops
        b.pair(1.2, 1.5, -3.7, 1.25, 2.85, 1.3, Mat.CANVAS);
        b.hull(-1.25, 1.25, -3.7, 1.3, 2.85, -0.9, 0.9, -3.7, 1.3, 3.25, Mat.CANVAS, Mat.CANVAS);
        b.seat(0.8, 1.45, 0.6, 0.45, Mat.WOOD);
        b.seat(-0.8, 1.45, 0.6, 0.45, Mat.WOOD);
        b.seat(0.8, 1.45, -0.5, 0.45, Mat.WOOD);
        b.seat(-0.8, 1.45, -0.5, 0.45, Mat.WOOD);
        b.seat(0.8, 1.45, -1.6, 0.45, Mat.WOOD);
        b.seat(-0.8, 1.45, -1.6, 0.45, Mat.WOOD);
        b.cyl(0, 1.6, 3.5, 'z', 0.45, 0.45, 0.25, 12, Mat.TIRE, Mat.RUBBER);                  // spare on the bumper
        b.pair(1.0, 2.0, 2.3, 1.15, 2.6, 2.35, Mat.BLACK);                                     // jerry cans
        b.pair(0.8, 1.1, -3.85, 1.15, 1.35, -3.75, Mat.TAILLIGHT);
        b.wheels(1.05, 0.52, 2.6, -1.6, 0.52, 0.42);
        b.wheel(1.05, 0.52, -2.75, 0.52, 0.42, false);
        b.wheel(-1.05, 0.52, -2.75, 0.52, 0.42, false);
        return b.build();
    }

    private static Model prisonBus() {
        Model m = bus(false);
        Builder b = Model.builder();
        for (int sd = -1; sd <= 1; sd += 2) {
            double x = sd * 1.262;
            for (double z = -5.2; z <= 5.2; z += 0.35) b.box(Math.min(x, x + sd * 0.02), 1.25, z, Math.max(x, x + sd * 0.02), 2.6, z + 0.05, Mat.BLACK);   // bars
            b.box(Math.min(x, x + sd * 0.02), 0.85, -5.5, Math.max(x, x + sd * 0.02), 1.0, 5.5, Mat.NAVY);                                                // stripe
        }
        b.box(-1.0, 0.55, 2.8, 1.0, 3.1, 2.9, Mat.METAL);                                         // cage behind the driver
        m.groups.addAll(b.build().groups);
        return m;
    }

    // ================================================================ wave 2

    private static double[] v(double x, double y, double z) {
        return new double[]{x, y, z};
    }

    private interface SegMat {
        Mat at(int i);
    }

    /** Tube along z (centre cx, cy) from z0 to z0 + len, with a material per segment (for window bands). Segment 0 is the top. */
    private static void tube(Builder b, double cx, double cy, double z0, double len, double r0, double r1, int segs, SegMat mat, Mat cap) {
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            double[] p00 = v(cx + Math.sin(a0) * r0, cy + Math.cos(a0) * r0, z0);
            double[] p01 = v(cx + Math.sin(a1) * r0, cy + Math.cos(a1) * r0, z0);
            double[] p10 = v(cx + Math.sin(a0) * r1, cy + Math.cos(a0) * r1, z0 + len);
            double[] p11 = v(cx + Math.sin(a1) * r1, cy + Math.cos(a1) * r1, z0 + len);
            b.quad(p00, p01, p11, p10, mat.at(i));
            if (cap != null && r1 > 0) {
                double[] c1 = v(cx, cy, z0 + len);
                b.quad(c1, c1, p10, p11, cap);
            }
        }
    }

    /** Thin swept plate (wings, tailplanes) from a root chord to a tip chord; mirrored when mirror is set. */
    private static void plate(Builder b, double xr, double yr, double zr0, double zr1, double xt, double yt, double zt0, double zt1,
                              double t, Mat m, boolean mirror) {
        for (int sd = 1; sd >= (mirror ? -1 : 1); sd -= 2) {
            double a = sd * xr, c = sd * xt;
            b.quad(v(a, yr + t, zr0), v(a, yr + t, zr1), v(c, yt + t, zt1), v(c, yt + t, zt0), m);
            b.quad(v(a, yr, zr0), v(c, yt, zt0), v(c, yt, zt1), v(a, yr, zr1), m);
            b.quad(v(a, yr, zr1), v(c, yt, zt1), v(c, yt + t, zt1), v(a, yr + t, zr1), m);
            b.quad(v(a, yr, zr0), v(a, yr + t, zr0), v(c, yt + t, zt0), v(c, yt, zt0), m);
            b.quad(v(c, yt, zt0), v(c, yt + t, zt0), v(c, yt + t, zt1), v(c, yt, zt1), m);
        }
    }

    /** Move a finished model up (monster truck lift kit). */
    private static void lift(Model m, float dy) {
        for (Model.Group g : m.groups) {
            g.pivot.y += dy;
            for (Model.Quad q : g.quads) for (int i = 0; i < 4; i++) q.p[i * 3 + 1] += dy;
        }
        for (Vector3f l : m.lights) l.y += dy;
    }

    // ------------------------------------------------------------- city cars

    private static Model police() {
        Builder b = carShell(SEDAN, Mat.GLASS);
        double w = 0.95;
        b.pair(w, 0.32, -1.25, w + 0.012, 0.98, 0.8, Mat.BLACK);                      // two-tone doors
        b.box(-0.72, 1.95, -0.62, 0.72, 2.02, -0.22, Mat.BLACK);                       // light bar
        b.group(Kind.SIREN_A, 0, 0, 0, 0, false);
        b.box(0.05, 2.02, -0.6, 0.7, 2.16, -0.24, Mat.SIREN_RED);
        b.group(Kind.SIREN_B, 0, 0, 0, 0, false);
        b.box(-0.7, 2.02, -0.6, -0.05, 2.16, -0.24, Mat.SIREN_BLUE);
        b.body();
        b.box(-0.72, 2.02, -0.62, 0.72, 2.18, -0.22, Mat.GLASS);
        b.box(-0.6, 0.35, 2.42, 0.6, 0.92, 2.52, Mat.BLACK);                           // push bar
        b.pair(0.5, 0.35, 2.3, 0.58, 0.92, 2.5, Mat.BLACK);
        b.box(0.98, 1.02, 0.72, 1.08, 1.14, 0.88, Mat.CHROME);                        // spotlight
        b.box(-0.41, 1.95, -1.26, -0.38, 2.7, -1.23, Mat.BLACK);                       // antenna
        b.box(-0.85, 0.75, -0.44, 0.85, 1.8, -0.4, Mat.GRILLE);                        // cage
        b.box(0.15, 1.1, 0.75, 0.35, 1.3, 0.85, Mat.SCREEN);                           // laptop
        return b.build();
    }

    private static Model taxi() {
        Builder b = carShell(SEDAN, Mat.GLASS);
        double w = 0.95;
        b.box(-0.34, 1.95, -0.58, 0.34, 2.2, -0.34, Mat.WHITE);                        // roof sign
        b.box(-0.3, 2.0, -0.345, 0.3, 2.15, -0.33, Mat.SCREEN);
        b.box(-0.3, 2.0, -0.6, 0.3, 2.15, -0.585, Mat.SCREEN);
        for (int i = 0; i < 12; i++) {                                                  // checker band
            double z = -2.2 + i * 0.36;
            b.pair(w, 0.62, z, w + 0.012, 0.74, z + 0.36, i % 2 == 0 ? Mat.BLACK : Mat.WHITE);
        }
        b.box(0.1, 1.05, 0.8, 0.3, 1.2, 0.9, Mat.SCREEN);                              // meter
        return b.build();
    }

    private static Model hatchback() {
        CarSpec s = new CarSpec(3.9, 0.88, 0.28, 0.95, 1.85, 0.75, 0.25, -1.3, -1.75,
                0.34, 0.74, 1.25, -1.25, 0.66, 0.05, -0.85, 0.65);
        Builder b = carShell(s, Mat.GLASS);
        b.box(-0.75, 1.82, -1.45, 0.75, 1.88, -1.22, Mat.PAINT_DARK);                 // roof spoiler
        b.box(-0.02, 1.2, -1.83, 0.02, 1.5, -1.79, Mat.BLACK);                        // rear wiper
        return b.build();
    }

    private static Model suv() {
        CarSpec s = new CarSpec(5.0, 1.02, 0.55, 1.35, 2.4, 1.15, 0.65, -2.05, -2.3,
                0.46, 0.86, 1.6, -1.55, 1.0, 0.3, -0.85, 1.05);
        Builder b = carShell(s, Mat.GLASS);
        b.pair(0.72, 2.4, -1.9, 0.8, 2.5, 0.5, Mat.CHROME);                            // roof rails
        b.box(-0.8, 2.4, -1.9, 0.8, 2.48, -1.82, Mat.CHROME);
        b.cyl(0, 1.15, -2.55, 'z', 0.44, 0.44, -0.24, 12, Mat.TIRE, Mat.RUBBER);       // spare
        b.pair(1.02, 0.55, 1.1, 1.08, 0.95, 2.1, Mat.BLACK);                           // fender flares
        b.pair(1.02, 0.55, -2.05, 1.08, 0.95, -1.05, Mat.BLACK);
        b.box(-0.7, 0.55, 2.5, 0.7, 1.05, 2.62, Mat.BLACK);                            // bull bar
        b.pair(0.98, 0.5, -0.9, 1.15, 0.56, 0.9, Mat.BLACK);                           // side steps
        return b.build();
    }

    private static Model muscle() {
        CarSpec s = new CarSpec(5.0, 1.0, 0.25, 0.92, 1.78, 0.6, -0.05, -1.15, -1.5,
                0.4, 0.84, 1.6, -1.55, 0.62, -0.1, -1.0, 0.5);
        Builder b = carShell(s, Mat.GLASS);
        b.box(-0.25, 1.0, 0.9, 0.25, 1.14, 1.75, Mat.BLACK);                           // hood scoop
        b.pair(0.1, 1.0, 0.62, 0.24, 1.012, 2.36, Mat.WHITE);                          // stripes
        b.pair(0.1, 1.78, -1.15, 0.24, 1.792, -0.05, Mat.WHITE);
        b.pair(0.1, 0.98, -2.48, 0.24, 0.992, -1.5, Mat.WHITE);
        b.box(-0.9, 0.98, -2.5, 0.9, 1.08, -2.3, Mat.PAINT_DARK);                     // ducktail
        b.pair(0.35, 0.28, -2.6, 0.5, 0.42, -2.45, Mat.CHROME);                       // exhausts
        return b.build();
    }

    private static Model limo() {
        CarSpec s = new CarSpec(7.2, 1.0, 0.3, 1.0, 1.95, 1.8, 1.25, -2.4, -2.7,
                0.38, 0.84, 2.45, -2.45, 0.7, 1.0, -0.2, 1.7);
        Builder b = carShell(s, Mat.GLASS);
        double w = 1.0;
        b.pair(w - 0.13, 1.0, -1.0, w - 0.06, 1.87, -0.9, Mat.PAINT);                 // extra pillar
        b.pair(w, 0.68, -3.4, w + 0.012, 0.74, 3.4, Mat.CHROME);
        b.seat(0, 0.7, -1.6, 1.7, Mat.SEAT);
        b.box(-0.9, 0.4, 0.55, 0.9, 1.3, 0.6, Mat.WOOD);                               // partition
        b.box(-0.9, 0.4, 0.3, -0.5, 0.9, 0.55, Mat.WOOD);                              // mini bar
        b.box(-0.85, 0.9, 0.35, -0.55, 1.0, 0.5, Mat.GLASS);
        b.box(-0.2, 1.85, -1.2, 0.2, 1.87, 0.0, Mat.SCREEN);                          // ceiling light strip
        return b.build();
    }

    private static Model roadster() {
        Builder b = Model.builder();
        b.box(-0.55, 0.4, -1.9, 0.55, 0.55, 1.9, Mat.BLACK);                           // frame
        b.box(-0.45, 0.55, 0.75, 0.45, 1.15, 1.95, Mat.PAINT);                         // hood
        b.hull(-0.45, 0.45, 0.75, 1.95, 1.15, -0.3, 0.3, 0.75, 1.95, 1.25, Mat.PAINT, Mat.PAINT);
        b.box(-0.42, 0.5, 1.95, 0.42, 1.32, 2.05, Mat.CHROME);                         // radiator
        b.box(-0.36, 0.58, 2.05, 0.36, 1.24, 2.07, Mat.GRILLE);
        b.cyl(0.55, 1.12, 1.95, 'z', 0.14, 0.14, 0.12, 10, Mat.CHROME, Mat.HEADLIGHT); // lamps
        b.cyl(-0.55, 1.12, 1.95, 'z', 0.14, 0.14, 0.12, 10, Mat.CHROME, Mat.HEADLIGHT);
        b.pair(0.44, 1.0, 1.98, 0.58, 1.04, 2.02, Mat.CHROME);
        b.box(-0.7, 0.55, 0.55, 0.7, 1.25, 0.75, Mat.PAINT);                           // cowl
        b.box(-0.7, 0.55, -1.5, 0.7, 0.62, 0.55, Mat.WOOD);                            // tub floor
        b.pair(0.62, 0.55, -1.5, 0.72, 1.1, 0.55, Mat.PAINT);
        b.box(-0.72, 0.55, -1.6, 0.72, 1.2, -1.5, Mat.PAINT);
        b.hull(-0.72, 0.72, -2.05, -1.6, 0.55, -0.6, 0.6, -1.95, -1.6, 1.1, Mat.PAINT, Mat.PAINT);
        b.pair(0.6, 1.25, 0.6, 0.64, 1.8, 0.66, Mat.CHROME);                           // windshield frame
        b.box(-0.64, 1.76, 0.6, 0.64, 1.8, 0.66, Mat.CHROME);
        b.pane(0.6, -0.6, 1.25, 0.63, 0.6, -0.6, 1.76, 0.63, Mat.GLASS);
        b.box(-0.62, 0.95, 0.45, 0.62, 1.2, 0.55, Mat.WOOD);                           // dash
        b.gauge(0.33, 1.08, 0.44, 0.14);
        b.box(0.31, 0.75, 0.2, 0.35, 1.25, 0.5, Mat.BLACK);
        b.steeringWheel(0.33, 1.3, 0.2, 0.2, 50);
        b.seat(0, 1.0, -0.6, 1.25, Mat.SEAT);
        b.pair(0.62, 0.9, 0.95, 0.88, 0.97, 1.95, Mat.PAINT_DARK);                    // fenders
        b.pair(0.62, 0.9, -1.85, 0.88, 0.97, -0.95, Mat.PAINT_DARK);
        b.pair(0.62, 0.5, -0.95, 0.9, 0.55, 0.95, Mat.BLACK);                          // running boards
        b.cyl(0, 0.95, -2.05, 'z', 0.4, 0.4, -0.16, 12, Mat.TIRE, Mat.RUBBER);         // spare
        b.pair(0.4, 0.8, -2.07, 0.5, 0.9, -2.03, Mat.TAILLIGHT);
        b.cyl(0.6, 1.2, 1.4, 'z', 0.05, 0.12, 0.35, 8, Mat.BRASS, Mat.BLACK);          // bulb horn
        b.wheels(0.75, 0.45, 1.45, -1.4, 0.45, 0.16);
        return b.build();
    }

    private static Model atv() {
        Builder b = Model.builder();
        b.box(-0.42, 0.45, -0.8, 0.42, 0.85, 0.85, Mat.PAINT);
        b.hull(-0.42, 0.42, 0.85, 1.05, 0.45, -0.35, 0.35, 0.85, 0.95, 0.85, Mat.PAINT, Mat.PAINT);
        b.pair(0.42, 0.78, 0.3, 0.85, 0.86, 1.0, Mat.PAINT);                           // fenders
        b.pair(0.42, 0.78, -1.0, 0.85, 0.86, -0.35, Mat.PAINT);
        b.box(-0.5, 0.92, 0.55, 0.5, 0.96, 1.0, Mat.BLACK);                            // racks
        b.box(-0.5, 0.92, -1.05, 0.5, 0.96, -0.6, Mat.BLACK);
        b.box(-0.2, 0.85, -0.7, 0.2, 1.0, 0.2, Mat.BLACK);                             // seat
        b.box(-0.15, 0.85, 0.2, 0.15, 1.0, 0.5, Mat.PAINT);                            // tank
        b.pair(0.3, 0.5, -0.2, 0.45, 0.58, 0.2, Mat.METAL);                            // footpegs
        b.pair(0.15, 0.7, 1.0, 0.3, 0.82, 1.06, Mat.HEADLIGHT);
        b.pair(0.15, 0.7, -0.84, 0.3, 0.8, -0.8, Mat.TAILLIGHT);
        b.cyl(-0.25, 0.6, -0.6, 'z', 0.05, 0.05, -0.4, 6, Mat.CHROME, Mat.BLACK);
        b.group(Kind.BIKE_FORK, 0, 1.0, 0.45, 0, false);
        b.box(-0.04, 0.85, 0.4, 0.04, 1.2, 0.48, Mat.BLACK);
        b.box(-0.45, 1.18, 0.35, 0.45, 1.23, 0.42, Mat.BLACK);
        b.pair(0.38, 1.16, 0.33, 0.5, 1.25, 0.44, Mat.RUBBER);
        b.body();
        b.gauge(0, 1.1, 0.52, 0.1);
        b.wheels(0.62, 0.36, 0.7, -0.7, 0.36, 0.32);
        return b.build();
    }

    // ---------------------------------------------------- service & heavy

    private static Model bus(boolean school) {
        Builder b = Model.builder();
        double w = 1.25, back = -5.5;
        double front = school ? 4.0 : 5.5, floor = school ? 0.9 : 0.55, winLow = school ? 1.55 : 1.25, winTop = 2.6;
        double roof = school ? 3.0 : 3.2;
        double door0 = front - 1.9, door1 = front - 0.8;
        b.box(-w, 0.35, back, w, floor, front, Mat.METAL);                             // underbody
        b.box(-w + 0.05, floor, back + 0.1, w - 0.05, floor + 0.03, front - 0.1, Mat.CARPET);
        // lower walls (door gap on the right side)
        b.box(w - 0.08, floor, back, w, winLow, front, Mat.PAINT);
        b.box(-w, floor, back, -w + 0.08, winLow, door0, Mat.PAINT);
        b.box(-w, floor, door1, -w + 0.08, winLow, front, Mat.PAINT);
        b.box(-w, 0.35, back, w, floor, back + 0.1, Mat.PAINT);
        // window pillars and glass
        for (double z = back; z < front - 0.2; z += 1.45) {
            b.box(w - 0.08, winLow, z, w, winTop, z + 0.12, Mat.PAINT);
            if (z + 0.12 < door0 || z > door1) b.box(-w, winLow, z, -w + 0.08, winTop, z + 0.12, Mat.PAINT);
        }
        b.sidePane(w - 0.04, winLow, winTop, back, front - 0.1, back, front - 0.1, Mat.GLASS);
        b.sidePane(-w + 0.04, winLow, winTop, back, door0, back, door0, Mat.GLASS);
        b.sidePane(-w + 0.04, winLow, winTop, door1, front - 0.1, door1, front - 0.1, Mat.GLASS);
        b.sidePane(-w + 0.04, floor + 0.1, winTop, door0, door1, door0, door1, Mat.GLASS);   // glass doors
        b.box(-w, floor, door0, -w + 0.1, winTop, door0 + 0.08, Mat.BLACK);
        b.box(-w, floor, (door0 + door1) / 2 - 0.03, -w + 0.1, winTop, (door0 + door1) / 2 + 0.03, Mat.BLACK);
        b.box(-w, floor, door1 - 0.08, -w + 0.1, winTop, door1, Mat.BLACK);
        // roof
        b.box(-w, winTop, back, w, roof, front, Mat.PAINT);
        if (school) b.hull(-w, w, back, front, roof, -w + 0.3, w - 0.3, back, front, roof + 0.15, Mat.PAINT, Mat.PAINT);
        else b.box(-0.6, roof, -2.5, 0.6, roof + 0.3, -0.5, Mat.METAL);               // roof AC
        // rear wall + window
        b.box(-w, floor, back, w, winTop, back + 0.1, Mat.PAINT);
        b.pane(w - 0.25, -w + 0.25, winLow + 0.2, back - 0.012, w - 0.25, -w + 0.25, winTop - 0.15, back - 0.012, Mat.GLASS);
        b.pair(w - 0.3, 0.8, back - 0.04, w - 0.08, 1.2, back, Mat.TAILLIGHT);
        b.box(-w, 0.35, back - 0.12, w, 0.6, back, Mat.BLACK);
        // front
        double wallTop = school ? 1.65 : 1.2;
        b.box(-w, 0.35, front - 0.1, w, wallTop, front, Mat.PAINT);
        b.pane(w - 0.05, -w + 0.05, wallTop, front - 0.02, w - 0.05, -w + 0.05, winTop, front - 0.06, Mat.GLASS);
        b.pair(w, 1.9, front - 0.3, w + 0.25, 2.3, front - 0.15, Mat.BLACK);          // mirrors
        if (school) {
            b.box(-0.95, 0.5, front, 0.95, 1.6, 5.4, Mat.PAINT);                       // hood
            b.hull(-0.95, 0.95, front, 5.4, 1.6, -0.8, 0.8, front, 5.3, 1.72, Mat.PAINT, Mat.PAINT);
            b.box(-0.6, 0.6, 5.4, 0.6, 1.45, 5.44, Mat.GRILLE);
            b.pair(0.65, 1.15, 5.4, 0.9, 1.4, 5.45, Mat.HEADLIGHT);
            b.box(-w, 0.45, 5.4, w, 0.7, 5.6, Mat.BLACK);
            b.pair(w, 1.3, back, w + 0.012, 1.38, front, Mat.BLACK);                   // black rub rails
            b.pair(w, 2.62, back, w + 0.012, 2.7, front, Mat.BLACK);
            b.box(w + 0.01, 1.7, 2.9, w + 0.04, 2.2, 3.4, Mat.RED);                    // stop arm
            b.pair(0.55, 2.7, front, 0.8, 2.9, front + 0.04, Mat.TAILLIGHT);           // warning lamps
            b.pair(0.55, 2.7, back - 0.04, 0.8, 2.9, back, Mat.TAILLIGHT);
        } else {
            b.box(-0.9, 2.7, front, 0.9, 3.05, front + 0.02, Mat.SCREEN);              // destination sign
            b.pair(w - 0.4, 0.7, front, w - 0.1, 0.92, front + 0.04, Mat.HEADLIGHT);
            b.box(-w, 0.35, front, w, 0.6, front + 0.12, Mat.BLACK);
            b.pair(w, 1.0, back, w + 0.012, 1.15, front, Mat.WHITE);
        }
        // driver's station
        double cushion = school ? 1.4 : 1.15, dz = school ? 3.4 : 4.4;
        b.seat(0.75, cushion, dz, 0.6, Mat.SEAT);
        b.dashboard(w - 0.1, 0.1, cushion + 0.35, front - 0.4, 0.75);
        b.steeringWheel(0.75, cushion + 0.45, dz + 0.4, 0.25, 60);
        if (!school) b.box(0.12, floor, dz + 0.2, 0.32, cushion + 0.25, dz + 0.4, Mat.METAL);   // farebox
        // passenger rows
        double z0 = school ? 2.3 : 3.0;
        for (int i = 0; i < 7; i++) {
            double z = z0 - i * 1.1;
            b.seat(0.75, cushion, z, 0.9, Mat.SEAT);
            b.seat(-0.75, cushion, z, 0.9, Mat.SEAT);
        }
        if (!school) {
            for (double z : new double[]{2.0, -0.5, -3.0}) b.pair(0.22, floor, z, 0.26, winTop, z + 0.04, Mat.CHROME);
            b.pair(0.25, winTop - 0.15, back + 0.3, 0.28, winTop - 0.12, front - 1.0, Mat.CHROME);
        }
        double fw = school ? 4.6 : 3.9;
        b.wheel(1.05, 0.5, fw, 0.5, 0.35, true);
        b.wheel(-1.05, 0.5, fw, 0.5, 0.35, true);
        b.wheel(1.05, 0.5, -3.2, 0.5, 0.45, false);
        b.wheel(-1.05, 0.5, -3.2, 0.5, 0.45, false);
        return b.build();
    }

    private static Model dumpTruck() {
        Builder b = Model.builder();
        double w = 1.25;
        truckCab(b, w, 2.0, 3.1, 1.15, 3.0, 1.2);
        b.box(-0.75, 0.6, -4.2, 0.75, 1.2, 2.0, Mat.METAL);                            // chassis
        b.box(-w, 1.2, -4.2, w, 1.4, 1.8, Mat.PAINT_DARK);                             // tipper body
        b.pair(w - 0.12, 1.4, -4.2, w, 2.9, 1.8, Mat.PAINT_DARK);
        b.box(-w, 1.4, 1.65, w, 3.3, 1.8, Mat.PAINT_DARK);
        b.box(-w, 3.2, 1.8, w, 3.3, 2.45, Mat.PAINT_DARK);                             // cab guard
        b.box(-w, 1.4, -4.27, w, 2.7, -4.12, Mat.PAINT_DARK);                          // tailgate
        for (double z = -3.6; z < 1.4; z += 1.0) b.pair(w, 1.4, z, w + 0.06, 2.9, z + 0.12, Mat.PAINT_DARK);
        b.hull(-w + 0.12, w - 0.12, -4.1, 1.62, 1.4, -w + 0.5, w - 0.5, -3.2, 0.9, 2.5, Mat.COAL, Mat.COAL);  // load
        b.pair(0.85, 1.0, -4.3, 1.1, 1.25, -4.26, Mat.TAILLIGHT);
        b.box(-w, 0.55, -4.4, w, 0.75, -4.2, Mat.BLACK);
        b.wheel(1.05, 0.62, 3.6, 0.62, 0.42, true);
        b.wheel(-1.05, 0.62, 3.6, 0.62, 0.42, true);
        for (double z : new double[]{-1.8, -3.1}) {
            b.wheel(1.05, 0.62, z, 0.62, 0.6, false);
            b.wheel(-1.05, 0.62, z, 0.62, 0.6, false);
        }
        return b.build();
    }

    private static Model tractor() {
        Builder b = Model.builder();
        b.box(-0.4, 0.75, -0.2, 0.4, 1.45, 2.2, Mat.PAINT);                            // engine hood
        b.hull(-0.4, 0.4, -0.2, 2.2, 1.45, -0.32, 0.32, -0.2, 2.1, 1.55, Mat.PAINT, Mat.PAINT);
        b.box(-0.36, 0.8, 2.2, 0.36, 1.45, 2.26, Mat.GRILLE);
        b.pair(0.2, 1.2, 2.2, 0.36, 1.35, 2.28, Mat.HEADLIGHT);
        b.box(-0.3, 0.45, 0.4, 0.3, 0.75, 2.0, Mat.METAL);
        b.box(-0.75, 0.4, 1.6, 0.75, 0.5, 1.8, Mat.METAL);                             // front axle
        b.cyl(-0.22, 1.55, 1.5, 'y', 0.07, 0.07, 1.1, 8, Mat.SMOKESTACK, Mat.BLACK);   // exhaust
        b.box(-0.65, 1.0, -1.4, 0.65, 1.1, -0.2, Mat.METAL);                           // cab floor
        b.pair(0.55, 1.72, -1.5, 1.15, 1.8, 0.3, Mat.PAINT);                           // fenders
        b.pair(1.1, 1.0, -1.5, 1.15, 1.72, -1.4, Mat.PAINT);
        b.pair(0.6, 1.1, 0.1, 0.68, 2.6, 0.2, Mat.BLACK);                              // cab posts
        b.pair(0.6, 1.1, -1.45, 0.68, 2.6, -1.35, Mat.BLACK);
        b.box(-0.78, 2.6, -1.55, 0.78, 2.7, 0.3, Mat.PAINT);                           // roof
        b.pane(0.6, -0.6, 1.55, 0.15, 0.6, -0.6, 2.6, 0.15, Mat.GLASS);
        b.pane(0.6, -0.6, 1.8, -1.4, 0.6, -0.6, 2.6, -1.4, Mat.GLASS);
        b.sidePane(0.64, 1.8, 2.6, -1.35, 0.1, -1.35, 0.1, Mat.GLASS);
        b.sidePane(-0.64, 1.8, 2.6, -1.35, 0.1, -1.35, 0.1, Mat.GLASS);
        b.box(-0.3, 1.1, -0.2, 0.3, 1.6, 0.1, Mat.PAINT);                              // dash pod
        b.gauge(0.1, 1.5, -0.21, 0.12);
        b.seat(0, 1.5, -0.6, 0.55, Mat.SEAT);
        b.steeringWheel(0, 1.85, -0.05, 0.18, 55);
        b.box(-0.1, 1.1, -0.15, 0.1, 1.8, -0.05, Mat.BLACK);
        b.pair(0.45, 2.48, -1.58, 0.62, 2.6, -1.55, Mat.TAILLIGHT);
        b.box(-0.2, 0.5, -1.7, 0.2, 0.75, -1.4, Mat.METAL);                            // hitch
        b.wheel(0.85, 0.85, -0.6, 0.85, 0.45, false);
        b.wheel(-0.85, 0.85, -0.6, 0.85, 0.45, false);
        b.wheel(0.65, 0.45, 1.7, 0.45, 0.22, true);
        b.wheel(-0.65, 0.45, 1.7, 0.45, 0.22, true);
        return b.build();
    }

    private static Model monster() {
        Model m = pickup();
        m.groups.removeIf(g -> g.kind == Kind.WHEEL);
        lift(m, 1.3f);
        Builder b = Model.builder();
        for (double z : new double[]{1.8, -1.8}) {
            b.wheel(1.25, 1.05, z, 1.05, 0.85, z > 0);
            b.wheel(-1.25, 1.05, z, 1.05, 0.85, z > 0);
            b.box(-1.0, 0.95, z - 0.08, 1.0, 1.15, z + 0.08, Mat.METAL);              // axle
            b.pair(0.55, 1.1, z - 0.25, 0.65, 2.0, z - 0.15, Mat.CHROME);            // shocks
            b.pair(0.55, 1.1, z + 0.15, 0.65, 2.0, z + 0.25, Mat.CHROME);
        }
        b.box(-0.15, 1.0, -1.8, 0.15, 1.85, 1.8, Mat.METAL);                           // frame
        b.pair(0.8, 2.05, -1.0, 0.9, 3.4, -0.9, Mat.CHROME);                           // roll bar
        b.box(-0.9, 3.3, -1.0, 0.9, 3.4, -0.9, Mat.CHROME);
        b.box(-0.8, 3.78, 0.0, 0.8, 3.9, 0.2, Mat.BLACK);                              // light bar
        b.pair(0.2, 3.8, 0.2, 0.7, 3.88, 0.24, Mat.HEADLIGHT);
        Model extra = b.build();
        m.groups.addAll(extra.groups);
        m.lights.addAll(extra.lights);
        return m;
    }

    private static Model scooter() {
        Builder b = Model.builder();
        double r = 0.25;
        spokedWheel(b, r, 0.58, r, 0.12, true);
        spokedWheel(b, r, -0.55, r, 0.14, false);
        b.box(-0.17, 0.22, -0.25, 0.17, 0.32, 0.38, Mat.PAINT);                        // floorboard
        b.hull(-0.22, 0.22, -0.8, -0.2, 0.28, -0.2, 0.2, -0.85, -0.15, 0.85, Mat.PAINT, Mat.PAINT);
        b.box(-0.18, 0.85, -0.7, 0.18, 0.95, -0.12, Mat.BLACK);                        // seat
        b.box(-0.12, 0.7, -0.88, 0.12, 0.8, -0.84, Mat.TAILLIGHT);
        b.hull(-0.2, 0.2, 0.3, 0.45, 0.22, -0.16, 0.16, 0.38, 0.5, 1.0, Mat.PAINT, Mat.PAINT);   // leg shield
        b.box(-0.2, 0.3, -0.75, 0.2, 0.36, -0.3, Mat.CHROME);
        b.group(Kind.BIKE_FORK, 0, 0.9, 0.5, 0, false);
        b.box(-0.04, r, 0.52, 0.04, 1.1, 0.6, Mat.CHROME);
        b.box(-0.38, 1.08, 0.45, 0.38, 1.13, 0.52, Mat.CHROME);
        b.pair(0.3, 1.06, 0.44, 0.42, 1.15, 0.53, Mat.RUBBER);
        b.box(-0.1, 0.95, 0.6, 0.1, 1.08, 0.66, Mat.HEADLIGHT);
        b.hull(-0.12, 0.12, 0.45, 0.75, 0.42, -0.1, 0.1, 0.5, 0.7, 0.5, Mat.PAINT, Mat.PAINT);   // front fender
        b.body();
        b.gauge(0, 1.16, 0.43, 0.1);
        return b.build();
    }

    // ----------------------------------------------------- military, air

    private static Model apc() {
        Builder b = Model.builder();
        double w = 1.1;
        b.box(-w, 0.4, -2.8, w, 0.5, 2.2, Mat.METAL);                                  // floor
        b.box(-w, 1.9, -2.8, w, 2.0, 2.2, Mat.CAMO);                                   // roof
        // side walls with a vision slot at eye height
        b.pair(w - 0.1, 0.5, -2.8, w, 1.5, 2.2, Mat.CAMO);
        b.pair(w - 0.1, 1.75, -2.8, w, 1.9, 2.2, Mat.CAMO);
        b.pair(w - 0.1, 1.5, -2.8, w, 1.75, 0.6, Mat.CAMO);
        b.pair(w - 0.1, 1.5, 1.8, w, 1.75, 2.2, Mat.CAMO);
        b.sidePane(w - 0.05, 1.5, 1.75, 0.6, 1.8, 0.6, 1.8, Mat.GLASS);
        b.sidePane(-w + 0.05, 1.5, 1.75, 0.6, 1.8, 0.6, 1.8, Mat.GLASS);
        b.box(-w, 0.5, -2.8, w, 1.9, -2.7, Mat.CAMO);                                  // rear door wall
        b.box(-0.5, 0.6, -2.82, 0.5, 1.8, -2.8, Mat.PAINT_DARK);
        // sloped glacis with armoured windscreen
        b.hull(-w, w, 2.2, 3.2, 0.4, -w, w, 2.2, 2.794, 1.4, Mat.CAMO, Mat.CAMO);
        b.pane(w, -w, 1.4, 2.794, w, -w, 1.85, 2.611, Mat.GLASS);
        b.hull(-w, w, 2.2, 2.611, 1.85, -w, w, 2.2, 2.55, 2.0, Mat.CAMO, Mat.CAMO);
        b.pair(0.6, 0.95, 2.93, 0.85, 1.1, 2.99, Mat.HEADLIGHT);
        b.pair(0.7, 1.4, -2.84, 0.9, 1.6, -2.8, Mat.TAILLIGHT);
        b.pair(0.7, 1.95, -2.4, 0.95, 2.2, -2.0, Mat.METAL);                            // smoke launchers
        b.cyl(-0.5, 2.0, -1.5, 'y', 0.3, 0.3, 0.08, 10, Mat.METAL, Mat.METAL);          // hatch
        // interior
        b.seat(0.45, 0.85, 1.4, 0.55, Mat.SEAT);
        b.seat(-0.45, 0.85, 1.4, 0.55, Mat.SEAT);
        for (double z : new double[]{-0.6, -1.8}) {
            b.seat(0.6, 0.85, z, 0.55, Mat.CANVAS);
            b.seat(-0.6, 0.85, z, 0.55, Mat.CANVAS);
        }
        b.dashboard(w - 0.12, -w + 0.12, 1.3, 1.95, 0.45);
        b.steeringWheel(0.45, 1.3, 1.8, 0.2, 40);
        // turret + machine gun (aims with your view)
        b.cyl(0, 2.0, 0.3, 'y', 0.55, 0.55, 0.1, 12, Mat.METAL, Mat.METAL);
        b.group(Kind.TURRET, 0, 2.4, 0.3, 0, false);
        b.box(-0.45, 2.1, -0.1, 0.45, 2.55, 0.7, Mat.CAMO);
        b.group(Kind.BARREL, 0, 2.4, 0.3, 0, false);
        b.box(-0.08, 2.33, 0.6, 0.08, 2.47, 0.9, Mat.BLACK);
        b.cyl(0, 2.4, 0.9, 'z', 0.05, 0.05, 1.3, 6, Mat.BLACK, Mat.BLACK);
        b.body();
        for (double z : new double[]{2.2, 0.8, -0.6, -2.0}) {
            b.wheel(1.15, 0.55, z, 0.55, 0.45, z > 0.5);
            b.wheel(-1.15, 0.55, z, 0.55, 0.45, z > 0.5);
        }
        return b.build();
    }

    private static Model airliner() {
        Builder b = Model.builder();
        double r = 1.5, cy = 2.1;
        // fuselage with a window band (segments 4 and 15) and a cheat line (5 and 14)
        tube(b, 0, cy, -9.0, 18.0, r, r, 20, i -> i == 4 || i == 15 ? Mat.GLASS : i == 5 || i == 14 ? Mat.STRIPE : Mat.PAINT, null);
        // nose: wraparound cockpit glass on the upper sides
        tube(b, 0, cy, 9.0, 2.6, r, 0.25, 20, i -> i == 2 || i == 3 || i == 16 || i == 17 ? Mat.CANOPY : i == 5 || i == 14 ? Mat.STRIPE : Mat.PAINT, Mat.METAL);
        tube(b, 0, cy, -9.0, -3.2, r, 0.35, 20, i -> Mat.PAINT, Mat.METAL);
        b.box(-1.25, 1.2, -8.8, 1.25, 1.25, 9.6, Mat.CARPET);                          // cabin floor
        // cabin: 14 rows, the window seats are rideable
        for (int i = 0; i < 14; i++) {
            double z = 6.5 - i * 1.1;
            b.seat(0.9, 1.65, z, 0.5, Mat.SEAT);
            b.seat(-0.9, 1.65, z, 0.5, Mat.SEAT);
            b.seat(0.42, 1.65, z, 0.42, Mat.SEAT);
            b.seat(-0.42, 1.65, z, 0.42, Mat.SEAT);
        }
        b.box(-1.3, 1.25, 8.2, -0.35, 3.4, 8.3, Mat.WALL);                             // cockpit bulkhead
        b.box(0.35, 1.25, 8.2, 1.3, 3.4, 8.3, Mat.WALL);
        b.box(-1.0, 3.2, -8.8, 1.0, 3.28, 8.2, Mat.WALL);                              // overhead bins
        // cockpit
        b.seat(0.5, 1.65, 9.3, 0.5, Mat.SEAT);
        b.seat(-0.5, 1.65, 9.3, 0.5, Mat.SEAT);
        b.dashboard(0.95, -0.95, 2.25, 10.0, 0.5);
        b.box(0.45, 1.6, 9.7, 0.55, 2.1, 9.78, Mat.BLACK);                             // yokes
        b.box(-0.55, 1.6, 9.7, -0.45, 2.1, 9.78, Mat.BLACK);
        // wings, engines, tail
        plate(b, 1.4, 1.15, -1.2, 2.8, 12.0, 1.9, -4.0, -2.6, 0.18, Mat.PAINT, true);
        plate(b, 11.9, 1.9, -3.9, -2.7, 12.0, 2.7, -4.0, -3.3, 0.08, Mat.PAINT_DARK, true);   // winglets
        for (int sd = -1; sd <= 1; sd += 2) {
            double x = sd * 4.5;
            b.cyl(x, 0.85, -0.3, 'z', 0.58, 0.62, 2.9, 14, Mat.METAL, Mat.BLACK);
            b.box(x - 0.08, 1.2, 0.2, x + 0.08, 1.45, 1.8, Mat.METAL);                  // pylon
            b.pair(1.6, 1.05, 2.75, 1.9, 1.15, 2.85, Mat.HEADLIGHT);                    // landing lights
        }
        b.hull(-0.1, 0.1, -11.8, -8.8, 3.3, -0.06, 0.06, -12.6, -11.4, 7.0, Mat.PAINT_DARK, Mat.PAINT_DARK);
        plate(b, 0.4, 2.6, -12.0, -10.2, 4.2, 2.9, -12.8, -11.8, 0.1, Mat.PAINT, true);
        b.box(-0.05, 6.95, -12.5, 0.05, 7.05, -12.3, Mat.TAILLIGHT);
        // landing gear
        b.box(-0.06, 0.3, 7.95, 0.06, 0.7, 8.05, Mat.METAL);
        b.wheel(0, 0.3, 8.0, 0.3, 0.2, true);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.box(sd * 1.5 - 0.06, 0.4, -0.55, sd * 1.5 + 0.06, 1.2, -0.45, Mat.METAL);
            b.wheel(sd * 1.5, 0.4, -0.2, 0.4, 0.3, false);
            b.wheel(sd * 1.5, 0.4, -0.8, 0.4, 0.3, false);
        }
        return b.build();
    }

    // ------------------------------------------------------ water, rail

    private static Model jetSki() {
        Builder b = Model.builder();
        b.hull(-0.3, 0.3, -1.2, 0.9, -0.25, -0.5, 0.5, -1.3, 1.0, 0.3, Mat.WHITE, Mat.DECK);
        b.hull(-0.25, 0.25, 0.9, 1.15, -0.2, -0.42, 0.42, 1.0, 1.5, 0.3, Mat.WHITE, Mat.DECK);
        b.box(-0.5, 0.3, -1.3, 0.5, 0.45, 0.6, Mat.PAINT);
        b.hull(-0.3, 0.3, 0.4, 1.3, 0.45, -0.22, 0.22, 0.5, 1.0, 0.75, Mat.PAINT, Mat.PAINT);
        b.box(-0.18, 0.45, -1.1, 0.18, 0.55, 0.3, Mat.BLACK);                          // seat
        b.box(-0.12, 0.55, 1.25, 0.12, 0.65, 1.32, Mat.HEADLIGHT);
        b.pair(0.5, 0.0, -1.0, 0.512, 0.3, 0.8, Mat.STRIPE);
        b.box(-0.08, -0.2, -1.34, 0.08, 0.0, -1.25, Mat.METAL);                        // jet nozzle
        b.box(-0.1, 0.4, -1.33, 0.1, 0.48, -1.3, Mat.TAILLIGHT);
        b.group(Kind.BIKE_FORK, 0, 0.8, 0.45, 0, false);
        b.box(-0.04, 0.7, 0.42, 0.04, 0.85, 0.5, Mat.BLACK);
        b.box(-0.4, 0.83, 0.38, 0.4, 0.88, 0.45, Mat.BLACK);
        b.pair(0.32, 0.81, 0.36, 0.44, 0.9, 0.47, Mat.RUBBER);
        b.body();
        b.gauge(0, 0.82, 0.6, 0.1);
        return b.build();
    }

    private static Model submarine() {
        Builder b = Model.builder();
        double r = 1.25, cy = 1.3;
        tube(b, 0, cy, -4.5, 2.9, r, r, 18, i -> Mat.PAINT, null);
        tube(b, 0, cy, -1.6, 2.8, r, r, 18, i -> i == 4 || i == 5 || i == 13 || i == 14 ? Mat.GLASS : Mat.PAINT, null);
        tube(b, 0, cy, 1.2, 1.8, r, r, 18, i -> Mat.PAINT, null);
        tube(b, 0, cy, 3.0, 1.8, r, 0.35, 18, i -> Mat.CANOPY, Mat.CANOPY);            // glass bow dome
        tube(b, 0, cy, -4.5, -2.1, r, 0.3, 18, i -> Mat.PAINT, Mat.METAL);
        b.box(-1.0, 0.35, -4.3, 1.0, 0.45, 3.2, Mat.CARPET);
        b.box(-0.35, cy + 1.1, -1.4, 0.35, 3.4, 0.4, Mat.PAINT_DARK);                  // sail
        b.cyl(0.12, 3.4, 0.0, 'y', 0.06, 0.06, 0.8, 6, Mat.METAL, Mat.METAL);         // periscope
        b.box(0.04, 4.15, -0.05, 0.2, 4.3, 0.2, Mat.METAL);
        b.pair(0.35, 3.0, -0.5, 1.2, 3.05, 0.1, Mat.PAINT_DARK);                       // dive planes
        b.pair(0.9, cy - 0.03, -6.0, 2.0, cy + 0.03, -5.2, Mat.PAINT_DARK);
        b.box(-0.03, cy - 1.0, -6.3, 0.03, cy + 1.0, -5.5, Mat.PAINT_DARK);           // rudder
        b.pair(0.6, 0.0, -3.5, 0.7, 0.12, 2.5, Mat.METAL);                             // skids
        b.pair(0.35, 0.05, 2.6, 0.7, 0.2, 2.9, Mat.HEADLIGHT);                         // flood lamps
        b.seat(0.4, 0.85, 2.2, 0.5, Mat.SEAT);
        b.seat(-0.4, 0.85, 2.2, 0.5, Mat.SEAT);
        b.seat(0.4, 0.85, 0.6, 0.5, Mat.SEAT);
        b.seat(-0.4, 0.85, 0.6, 0.5, Mat.SEAT);
        b.seat(0.4, 0.85, -1.0, 0.5, Mat.SEAT);
        b.seat(-0.4, 0.85, -1.0, 0.5, Mat.SEAT);
        b.dashboard(0.9, -0.9, 1.25, 2.9, 0.4);
        b.box(-0.05, cy + 1.0, -4.0, 0.05, cy + 1.06, 2.6, Mat.BRASS);                 // pipes
        b.box(0.75, 0.9, -3.8, 0.95, 1.3, -2.2, Mat.SCREEN);                           // sonar
        b.group(Kind.PROP_Z, 0, cy, -6.65, 0, false);
        b.box(-0.08, cy - 0.9, -6.7, 0.08, cy + 0.9, -6.6, Mat.BRASS);
        b.box(-0.9, cy - 0.08, -6.7, 0.9, cy + 0.08, -6.6, Mat.BRASS);
        b.body();
        return b.build();
    }

    private static Model diesel() {
        Builder b = Model.builder();
        double w = 1.2;
        b.box(-w, 0.7, -5.0, w, 1.0, 5.0, Mat.BLACK);                                  // deck
        b.box(-0.85, 1.0, -4.8, 0.85, 2.9, 2.5, Mat.PAINT);                            // long hood
        b.hull(-0.85, 0.85, -4.8, 2.5, 2.9, -0.6, 0.6, -4.8, 2.5, 3.05, Mat.PAINT, Mat.PAINT);
        for (double z = -4.4; z < 2.0; z += 1.2) b.pair(0.85, 1.6, z, 0.862, 2.6, z + 0.7, Mat.GRILLE);
        b.pair(0.85, 1.25, -4.8, 0.862, 1.4, 2.5, Mat.WHITE);
        b.box(-0.15, 3.05, 0.8, 0.15, 3.2, 1.2, Mat.BLACK);                            // exhaust
        b.pair(w - 0.05, 1.85, -4.8, w, 1.9, 2.5, Mat.YELLOW);                         // handrails
        for (double z = -4.8; z < 2.6; z += 1.0) b.pair(w - 0.05, 1.0, z, w, 1.9, z + 0.05, Mat.YELLOW);
        // cab
        b.box(-w, 1.0, 2.5, w, 3.2, 2.6, Mat.PAINT);
        b.pair(w - 0.1, 1.0, 2.6, w, 1.9, 4.4, Mat.PAINT);
        b.pair(w - 0.1, 1.9, 2.6, w, 2.8, 2.7, Mat.PAINT);
        b.pair(w - 0.1, 1.9, 4.3, w, 2.8, 4.4, Mat.PAINT);
        b.pair(w - 0.1, 2.8, 2.6, w, 3.2, 4.4, Mat.PAINT);
        b.sidePane(w - 0.05, 1.9, 2.8, 2.7, 4.3, 2.7, 4.3, Mat.GLASS);
        b.sidePane(-w + 0.05, 1.9, 2.8, 2.7, 4.3, 2.7, 4.3, Mat.GLASS);
        b.box(-w, 1.0, 4.4, w, 1.9, 4.5, Mat.PAINT);
        b.pane(w - 0.05, -w + 0.05, 1.9, 4.46, w - 0.05, -w + 0.05, 2.9, 4.42, Mat.GLASS);
        b.box(-w, 2.9, 4.4, w, 3.2, 4.5, Mat.PAINT);
        b.box(-w - 0.05, 3.2, 2.5, w + 0.05, 3.35, 4.55, Mat.PAINT);
        b.cyl(0.4, 3.35, 3.4, 'z', 0.08, 0.14, 0.4, 8, Mat.CHROME, Mat.BLACK);          // horn
        b.box(-0.25, 3.0, 4.5, 0.25, 3.15, 4.55, Mat.HEADLIGHT);
        b.box(-0.85, 1.0, 4.5, 0.85, 2.0, 5.0, Mat.PAINT);                             // short nose
        b.box(-0.85, 1.2, 5.0, 0.85, 1.35, 5.02, Mat.YELLOW);
        b.pair(0.4, 1.6, 5.0, 0.6, 1.8, 5.04, Mat.HEADLIGHT);
        b.pair(0.55, 1.5, -5.04, 0.75, 1.7, -5.0, Mat.TAILLIGHT);
        b.box(-0.7, 0.25, -2.0, 0.7, 0.7, 1.5, Mat.METAL);                             // fuel tank
        b.box(-0.3, 0.8, 5.0, 0.3, 1.0, 5.3, Mat.METAL);                               // couplers
        b.box(-0.3, 0.8, -5.3, 0.3, 1.0, -5.0, Mat.METAL);
        b.seat(0.4, 1.5, 3.4, 0.5, Mat.SEAT);
        b.seat(-0.4, 1.5, 3.4, 0.5, Mat.SEAT);
        b.dashboard(w - 0.1, -w + 0.1, 1.9, 4.1, 0.4);
        for (double zc : new double[]{3.4, -3.4}) {
            b.pair(0.85, 0.3, zc - 1.3, 1.05, 0.7, zc + 1.3, Mat.BLACK);
            for (double dz : new double[]{-0.9, 0, 0.9}) {
                b.wheel(0.9, 0.42, zc + dz, 0.42, 0.12, false);
                b.wheel(-0.9, 0.42, zc + dz, 0.42, 0.12, false);
            }
        }
        return b.build();
    }

    private static Model bulletTrain() {
        Builder b = Model.builder();
        double w = 1.2;
        b.box(-w, 0.5, -6.0, w, 3.0, 3.0, Mat.PAINT);
        b.hull(-w, w, -6.0, 3.0, 3.0, -0.9, 0.9, -6.0, 3.0, 3.25, Mat.PAINT, Mat.PAINT);
        // streamlined nose in three parts so the cab can see out through the screen
        b.hull(-w, w, 3.0, 8.0, 0.5, -1.032, 1.032, 3.0, 5.872, 1.9, Mat.PAINT, Mat.PAINT);
        b.pane(1.032, -1.032, 1.9, 5.872, 0.93, -0.93, 2.75, 4.58, Mat.CANOPY);
        for (int sd = -1; sd <= 1; sd += 2) {
            b.quad(v(sd * 1.032, 1.9, 3.0), v(sd * 1.032, 1.9, 5.872), v(sd * 0.93, 2.75, 4.58), v(sd * 0.93, 2.75, 3.0), Mat.PAINT);
        }
        b.hull(-0.93, 0.93, 3.0, 4.58, 2.75, -0.9, 0.9, 3.0, 4.2, 3.0, Mat.PAINT, Mat.PAINT);
        b.pair(w, 1.35, -6.0, w + 0.012, 1.55, 3.0, Mat.NAVY);
        b.sidePane(w + 0.006, 1.8, 2.4, -5.5, 2.5, -5.5, 2.5, Mat.GLASS);
        b.sidePane(-w - 0.006, 1.8, 2.4, -5.5, 2.5, -5.5, 2.5, Mat.GLASS);
        b.box(-w + 0.1, 0.3, -6.0, w - 0.1, 0.5, 7.0, Mat.BLACK);                      // skirt
        b.pair(0.4, 0.8, 7.28, 0.8, 1.0, 7.5, Mat.HEADLIGHT);
        b.pair(0.5, 1.6, -6.03, 0.8, 1.8, -6.0, Mat.TAILLIGHT);
        b.box(-0.3, 3.25, -3.0, 0.3, 3.35, -2.4, Mat.METAL);                           // pantograph
        b.hull(-0.04, 0.04, -2.9, -2.5, 3.35, -0.04, 0.04, -2.2, -1.8, 4.0, Mat.CHROME, Mat.CHROME);
        b.box(-0.5, 3.98, -2.1, 0.5, 4.04, -1.9, Mat.CHROME);
        b.seat(0.4, 1.6, 3.4, 0.5, Mat.SEAT);
        b.seat(-0.4, 1.6, 3.4, 0.5, Mat.SEAT);
        b.dashboard(0.95, -0.95, 1.95, 4.0, 0.4);
        for (double zc : new double[]{1.5, -4.5}) {
            for (double dz : new double[]{-0.7, 0.7}) {
                b.wheel(0.85, 0.35, zc + dz, 0.35, 0.12, false);
                b.wheel(-0.85, 0.35, zc + dz, 0.35, 0.12, false);
            }
        }
        return b.build();
    }

    private static Model bulletCarriage() {
        Builder b = Model.builder();
        double w = 1.2;
        b.box(-w, 0.5, -3.0, w, 1.0, 3.0, Mat.PAINT);
        b.pair(w - 0.08, 1.0, -3.0, w, 1.7, 3.0, Mat.PAINT);
        b.pair(w - 0.08, 2.4, -3.0, w, 3.0, 3.0, Mat.PAINT);
        for (double z = -3.0; z < 3.0; z += 1.2) b.pair(w - 0.08, 1.7, z, w, 2.4, z + 0.15, Mat.PAINT);
        b.sidePane(w - 0.04, 1.7, 2.4, -3.0, 3.0, -3.0, 3.0, Mat.GLASS);
        b.sidePane(-w + 0.04, 1.7, 2.4, -3.0, 3.0, -3.0, 3.0, Mat.GLASS);
        b.hull(-w, w, -3.0, 3.0, 3.0, -0.9, 0.9, -3.0, 3.0, 3.25, Mat.PAINT, Mat.PAINT);
        b.box(-w, 1.0, 2.9, w, 3.0, 3.0, Mat.PAINT);
        b.box(-w, 1.0, -3.0, w, 3.0, -2.9, Mat.PAINT);
        b.box(-0.55, 0.8, 3.0, 0.55, 2.9, 3.15, Mat.BLACK);                            // gangways
        b.box(-0.55, 0.8, -3.15, 0.55, 2.9, -3.0, Mat.BLACK);
        b.pair(w, 1.42, -3.0, w + 0.012, 1.6, 3.0, Mat.NAVY);
        b.box(-w + 0.05, 1.0, -2.9, w - 0.05, 1.05, 2.9, Mat.CARPET);
        b.box(-0.2, 2.94, -2.5, 0.2, 2.99, 2.5, Mat.SCREEN);                           // ceiling light
        for (double z : new double[]{1.3, -1.3}) {
            b.seat(0.35, 1.35, z, 0.55, Mat.SEAT);
            b.seat(-0.35, 1.35, z, 0.55, Mat.SEAT);
        }
        for (double z : new double[]{2.2, -2.2}) {
            for (int sd = -1; sd <= 1; sd += 2) {
                b.wheel(sd * 0.85, 0.35, z - 0.4, 0.35, 0.12, false);
                b.wheel(sd * 0.85, 0.35, z + 0.4, 0.35, 0.12, false);
            }
        }
        return b.build();
    }

    // ================================================================ wave 3

    private interface SphereMat {
        Mat at(int lat, int lon);
    }

    /** Ellipsoid from latitude/longitude quads; lat 0 is the top. */
    private static void sphere(Builder b, double cx, double cy, double cz, double rx, double ry, int lat, int lon, SphereMat mat) {
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                b.quad(sp(cx, cy, cz, rx, ry, t0, p0), sp(cx, cy, cz, rx, ry, t0, p1), sp(cx, cy, cz, rx, ry, t1, p1), sp(cx, cy, cz, rx, ry, t1, p0), mat.at(i, j));
            }
        }
    }

    private static double[] sp(double cx, double cy, double cz, double rx, double ry, double t, double p) {
        return v(cx + rx * Math.sin(t) * Math.cos(p), cy + ry * Math.cos(t), cz + rx * Math.sin(t) * Math.sin(p));
    }

    // ------------------------------------------------------------- weird

    private static Model ostrich() {
        Builder b = Model.builder();
        b.hull(-0.38, 0.38, -0.65, 0.55, 1.15, -0.32, 0.32, -0.55, 0.45, 1.75, Mat.FEATHER, Mat.FEATHER);     // body
        b.box(-0.3, 1.35, -0.98, 0.3, 1.9, -0.6, Mat.WHITE);                                                   // tail plume
        b.pair(0.36, 1.25, -0.5, 0.42, 1.62, 0.3, Mat.FEATHER);                                                // wings
        b.pair(0.38, 1.3, -0.65, 0.44, 1.5, -0.4, Mat.WHITE);
        b.box(-0.3, 1.75, -0.45, 0.3, 1.8, 0.15, Mat.SEAT);                                                    // saddle
        b.pair(0.3, 1.45, -0.2, 0.33, 1.78, -0.1, Mat.BLACK);                                                  // girth strap
        b.group(Kind.NECK, 0, 1.6, 0.45, 0, false);
        b.box(-0.08, 1.55, 0.38, 0.08, 2.55, 0.54, Mat.SKIN);
        b.box(-0.13, 2.48, 0.38, 0.13, 2.72, 0.78, Mat.SKIN);                                                  // head
        b.box(-0.07, 2.5, 0.78, 0.07, 2.6, 0.95, Mat.ORANGE);                                                  // beak
        b.pair(0.13, 2.6, 0.6, 0.14, 2.67, 0.67, Mat.BLACK);                                                   // eyes
        b.box(-0.09, 2.72, 0.45, 0.09, 2.78, 0.6, Mat.FEATHER);                                                // tuft
        for (int sd = -1; sd <= 1; sd += 2) {
            b.group(sd > 0 ? Kind.LEG_L : Kind.LEG_R, sd * 0.18, 1.2, 0.0, 0, false);
            double x0 = sd > 0 ? 0.1 : -0.26, x1 = sd > 0 ? 0.26 : -0.1;
            b.box(x0, 0.75, -0.1, x1, 1.22, 0.12, Mat.SKIN);                                                   // thigh
            b.box(x0 + 0.04, 0.06, -0.04, x1 - 0.04, 0.78, 0.04, Mat.SKIN);                                    // shin
            b.box(x0, 0.0, -0.06, x1, 0.06, 0.24, Mat.SKIN);                                                   // foot
            b.box(x0 + 0.02, 0.0, 0.24, x1 - 0.02, 0.05, 0.32, Mat.BLACK);                                     // claw
        }
        b.body();
        return b.build();
    }

    private static Model chrono() {
        CarSpec s = new CarSpec(4.7, 0.97, 0.22, 0.8, 1.7, 0.6, -0.1, -1.0, -1.3,
                0.37, 0.84, 1.55, -1.45, 0.55, -0.2, Double.NaN, 0.45);
        Builder b = carShell(s, Mat.GLASS);
        b.pair(0.97, 0.35, -2.0, 1.0, 0.75, 1.9, Mat.STEEL);                                  // steel sills
        for (int sd = -1; sd <= 1; sd += 2) {                                                  // time coils on the rear deck
            b.cyl(sd * 0.48, 1.1, -1.95, 'z', 0.24, 0.24, 0.07, 14, Mat.TIME_GLOW, Mat.STEEL);
            b.cyl(sd * 0.48, 1.1, -1.75, 'z', 0.2, 0.2, 0.05, 14, Mat.STEEL, Mat.TIME_GLOW);
            b.box(sd * 0.48 - 0.04, 0.8, -1.95, sd * 0.48 + 0.04, 0.88, -1.7, Mat.STEEL);
        }
        b.cyl(0, 1.7, -0.6, 'y', 0.22, 0.22, 0.06, 14, Mat.STEEL, Mat.TIME_GLOW);           // chrono dial
        b.box(0.1, 1.05, 0.5, 0.35, 1.2, 0.55, Mat.TIME_GLOW);                               // dashboard time display
        b.box(-0.8, 0.85, -2.4, 0.8, 0.92, -2.2, Mat.STEEL);
        b.group(Kind.ABILITY, 0, 0, 0, 0, false);                                              // charged: glowing wheels and underglow
        for (int sd = -1; sd <= 1; sd += 2) {
            for (double z : new double[]{1.55, -1.45}) b.cyl(sd * 0.99, 0.37, z, 'x', 0.42, 0.42, sd * 0.03, 16, Mat.TIME_GLOW, null);
        }
        b.box(-0.8, 0.12, -1.8, 0.8, 0.14, 1.8, Mat.TIME_GLOW);
        b.body();
        return b.build();
    }

    private static Model shrinkCar() {
        CarSpec s = new CarSpec(3.0, 0.75, 0.25, 0.85, 1.75, 0.6, 0.15, -0.8, -1.2,
                0.3, 0.6, 1.0, -1.0, 0.65, -0.1, Double.NaN, 0.45);
        Builder b = carShell(s, Mat.CANOPY);
        b.cyl(0, 1.75, -0.3, 'y', 0.04, 0.04, 0.45, 6, Mat.STEEL, Mat.STEEL);               // shrink-ray antenna
        b.cyl(0, 2.2, -0.3, 'y', 0.25, 0.0, -0.18, 10, Mat.STEEL, Mat.STEEL);               // dish
        b.box(-0.06, 2.2, -0.36, 0.06, 2.32, -0.24, Mat.ALIEN);
        b.pair(0.75, 0.5, -1.4, 0.76, 0.6, 1.4, Mat.WHITE);
        b.box(0.12, 1.0, 0.4, 0.32, 1.1, 0.44, Mat.ALIEN);                                   // shrink button
        return b.build();
    }

    private static Model ufo() {
        Builder b = Model.builder();
        b.cyl(0, 0.4, 0, 'y', 1.0, 3.0, 0.5, 24, Mat.STEEL, Mat.METAL);                     // lower hull
        b.cyl(0, 0.9, 0, 'y', 3.0, 1.6, 0.45, 24, Mat.STEEL, null);                         // upper hull
        b.cyl(0, 0.95, 0, 'y', 1.55, 1.55, 0.05, 20, Mat.METAL, Mat.CARPET);                // floor
        b.cyl(0, 1.35, 0, 'y', 1.6, 1.25, 0.5, 20, Mat.CANOPY, null);                        // glass dome
        b.cyl(0, 1.85, 0, 'y', 1.25, 0.3, 0.45, 20, Mat.CANOPY, Mat.CANOPY);
        b.cyl(0, 0.4, 0, 'y', 0.6, 0.6, -0.05, 12, Mat.ALIEN, Mat.ALIEN);                   // belly light
        for (int i = 0; i < 3; i++) {                                                         // landing legs
            double a = Math.PI * 2 * i / 3;
            double x = Math.sin(a) * 1.6, z = Math.cos(a) * 1.6;
            b.box(x - 0.05, 0.0, z - 0.05, x + 0.05, 0.6, z + 0.05, Mat.METAL);
            b.box(x - 0.18, 0.0, z - 0.18, x + 0.18, 0.04, z + 0.18, Mat.METAL);
        }
        b.seat(0.45, 1.0, 0, 0.5, Mat.SEAT);
        b.seat(-0.45, 1.0, 0, 0.5, Mat.SEAT);
        b.box(-0.6, 0.97, 0.6, 0.6, 1.4, 0.9, Mat.DASH);
        b.box(-0.5, 1.3, 0.59, 0.5, 1.38, 0.6, Mat.ALIEN);
        b.group(Kind.ROTOR_Y, 0, 0.9, 0, 0, false);                                           // spinning rim lights
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            double x = Math.sin(a) * 2.95, z = Math.cos(a) * 2.95;
            b.box(x - 0.12, 0.84, z - 0.12, x + 0.12, 0.96, z + 0.12, i % 2 == 0 ? Mat.ALIEN : Mat.SIREN_RED);
        }
        b.group(Kind.ABILITY, 0, 0, 0, 0, false);                                             // tractor beam
        b.cyl(0, 0.35, 0, 'y', 0.7, 2.6, -22, 16, Mat.ALIEN, null);
        b.body();
        return b.build();
    }

    private static Model carpet() {
        Builder b = Model.builder();
        b.group(Kind.FLAG, 0, 0, 0, 0, false);
        double w = 0.85;
        for (double z = -1.5; z < 1.49; z += 0.25) {
            b.quad(v(-w, 0.42, z), v(w, 0.42, z), v(w, 0.42, z + 0.25), v(-w, 0.42, z + 0.25), Mat.RUG);
            b.quad(v(-w, 0.38, z), v(-w, 0.38, z + 0.25), v(w, 0.38, z + 0.25), v(w, 0.38, z), Mat.RUG);
            b.quad(v(w, 0.38, z), v(w, 0.38, z + 0.25), v(w, 0.42, z + 0.25), v(w, 0.42, z), Mat.RUG);
            b.quad(v(-w, 0.38, z), v(-w, 0.42, z), v(-w, 0.42, z + 0.25), v(-w, 0.38, z + 0.25), Mat.RUG);
        }
        for (double x = -0.8; x <= 0.81; x += 0.2) {                                         // tassels
            b.box(x - 0.03, 0.3, 1.5, x + 0.03, 0.42, 1.62, Mat.YELLOW);
            b.box(x - 0.03, 0.3, -1.62, x + 0.03, 0.42, -1.5, Mat.YELLOW);
        }
        b.box(0.15, 0.42, -0.25, 0.65, 0.5, 0.25, Mat.RED);                                  // cushions
        b.box(-0.65, 0.42, -0.25, -0.15, 0.5, 0.25, Mat.RED);
        b.body();
        return b.build();
    }

    private static Model hamsterBall() {
        Builder b = Model.builder();
        b.box(-0.3, 0.35, -0.3, 0.3, 0.6, 0.3, Mat.BLACK);                                  // seat (stays level)
        b.group(Kind.BALL, 0, 1.0, 0, 1.0, false);
        sphere(b, 0, 1.0, 0, 1.0, 1.0, 8, 12, (i, j) -> j % 4 == 0 ? Mat.PAINT : Mat.CANOPY);
        b.cyl(0, 1.0, 0, 'x', 1.01, 1.01, 0.0, 20, Mat.PAINT, null);
        b.body();
        return b.build();
    }

    private static Model rocketCart() {
        Builder b = Model.builder();
        b.box(-0.45, 0.45, -0.7, 0.45, 0.5, 0.6, Mat.GRILLE);                                // basket floor
        b.pair(0.42, 0.5, -0.7, 0.45, 1.15, 0.6, Mat.GRILLE);
        b.box(-0.45, 0.5, 0.55, 0.45, 1.05, 0.6, Mat.GRILLE);
        b.box(-0.45, 0.5, -0.75, 0.45, 1.15, -0.7, Mat.GRILLE);
        b.box(-0.47, 1.2, -0.88, 0.47, 1.26, -0.82, Mat.RED);                               // handle
        b.pair(0.4, 1.0, -0.86, 0.44, 1.22, -0.74, Mat.CHROME);
        b.pair(0.33, 0.12, -0.6, 0.38, 0.45, -0.55, Mat.CHROME);                            // legs
        b.pair(0.33, 0.12, 0.45, 0.38, 0.45, 0.5, Mat.CHROME);
        b.box(-0.38, 0.1, -0.62, 0.38, 0.14, 0.52, Mat.CHROME);
        b.cyl(0, 0.75, -0.75, 'z', 0.18, 0.18, -0.65, 10, Mat.RED, Mat.METAL);              // strapped-on rocket
        b.cyl(0, 0.75, -1.4, 'z', 0.18, 0.12, -0.12, 10, Mat.METAL, Mat.BLACK);
        b.pair(0.16, 0.73, -1.35, 0.32, 0.77, -1.1, Mat.RED);
        b.box(-0.02, 0.9, -1.35, 0.02, 1.05, -1.1, Mat.RED);
        b.box(-0.2, 0.6, -0.8, 0.2, 0.64, -0.72, Mat.BLACK);                                // duct tape
        b.group(Kind.ABILITY, 0, 0, 0, 0, false);
        b.cyl(0, 0.75, -1.52, 'z', 0.15, 0.0, -1.3, 10, Mat.FLAME, null);
        b.body();
        b.wheels(0.35, 0.1, 0.5, -0.57, 0.1, 0.06);
        return b.build();
    }

    private static Model mower() {
        Builder b = Model.builder();
        b.box(-0.62, 0.15, -0.2, 0.62, 0.32, 0.8, Mat.PAINT_DARK);                          // deck
        b.box(-0.78, 0.18, 0.1, -0.62, 0.32, 0.5, Mat.BLACK);                               // grass chute
        b.box(-0.4, 0.35, 0.2, 0.4, 0.75, 1.05, Mat.PAINT);                                 // hood
        b.hull(-0.4, 0.4, 0.2, 1.05, 0.75, -0.32, 0.32, 0.25, 0.95, 0.82, Mat.PAINT, Mat.PAINT);
        b.box(-0.3, 0.4, 1.05, 0.3, 0.7, 1.08, Mat.GRILLE);
        b.pair(0.18, 0.55, 1.05, 0.32, 0.65, 1.09, Mat.HEADLIGHT);
        b.box(-0.45, 0.32, -0.95, 0.45, 0.58, -0.1, Mat.PAINT);                             // rear body
        b.seat(0, 0.85, -0.35, 0.5, Mat.BLACK);
        b.box(-0.04, 0.6, 0.15, 0.04, 1.05, 0.22, Mat.BLACK);
        b.steeringWheel(0, 1.05, 0.18, 0.15, 40);
        b.box(0.18, 0.75, 0.15, 0.24, 0.95, 0.2, Mat.RED);                                  // blade lever
        b.group(Kind.ROTOR_Y, 0, 0.2, 0.3, 0, false);
        b.box(-0.55, 0.16, 0.26, 0.55, 0.19, 0.34, Mat.METAL);
        b.box(-0.04, 0.16, -0.25, 0.04, 0.19, 0.85, Mat.METAL);
        b.body();
        b.wheel(0.45, 0.2, 0.75, 0.2, 0.14, true);
        b.wheel(-0.45, 0.2, 0.75, 0.2, 0.14, true);
        b.wheel(0.5, 0.35, -0.6, 0.35, 0.24, false);
        b.wheel(-0.5, 0.35, -0.6, 0.35, 0.24, false);
        return b.build();
    }

    // ------------------------------------------------------------ normal

    private static Model goKart() {
        Builder b = Model.builder();
        b.box(-0.55, 0.1, -1.0, 0.55, 0.16, 1.0, Mat.BLACK);                                // frame
        b.hull(-0.42, 0.42, 0.55, 1.08, 0.16, -0.3, 0.3, 0.55, 0.95, 0.4, Mat.PAINT, Mat.PAINT);   // nose
        b.pair(0.45, 0.16, -0.45, 0.62, 0.36, 0.3, Mat.PAINT);                              // side pods
        b.box(-0.22, 0.16, -0.55, 0.22, 0.45, 0.05, Mat.BLACK);                             // seat shell
        b.box(-0.22, 0.45, -0.6, 0.22, 0.85, -0.5, Mat.BLACK);
        b.box(0.2, 0.16, -0.95, 0.5, 0.48, -0.55, Mat.METAL);                               // engine
        b.cyl(0.42, 0.35, -0.95, 'z', 0.05, 0.05, -0.25, 6, Mat.CHROME, Mat.BLACK);
        b.box(-0.6, 0.12, -1.12, 0.6, 0.24, -1.0, Mat.BLACK);                               // bumper
        b.box(-0.32, 0.4, 0.95, 0.32, 0.5, 1.0, Mat.WHITE);                                 // number board
        b.box(-0.03, 0.2, 0.15, 0.03, 0.6, 0.45, Mat.BLACK);
        b.steeringWheel(0, 0.6, 0.28, 0.15, 60);
        b.wheel(0.62, 0.2, 0.7, 0.2, 0.2, true);
        b.wheel(-0.62, 0.2, 0.7, 0.2, 0.2, true);
        b.wheel(0.62, 0.23, -0.7, 0.23, 0.28, false);
        b.wheel(-0.62, 0.23, -0.7, 0.23, 0.28, false);
        return b.build();
    }

    private static Model golfCart() {
        Builder b = Model.builder();
        b.box(-0.65, 0.3, -1.4, 0.65, 0.55, 1.3, Mat.PAINT);
        b.hull(-0.6, 0.6, 0.7, 1.35, 0.55, -0.55, 0.55, 0.75, 1.25, 0.95, Mat.PAINT, Mat.PAINT);
        b.pair(0.35, 0.65, 1.3, 0.55, 0.78, 1.36, Mat.HEADLIGHT);
        b.box(-0.7, 1.85, -1.3, 0.7, 1.92, 1.0, Mat.PAINT);                                 // canopy
        b.pair(0.6, 0.55, 0.85, 0.64, 1.85, 0.9, Mat.BLACK);
        b.pair(0.6, 0.55, -1.25, 0.64, 1.85, -1.2, Mat.BLACK);
        b.pane(0.6, -0.6, 0.95, 0.9, 0.6, -0.6, 1.8, 0.88, Mat.GLASS);
        b.seat(0, 0.75, 0.0, 1.1, Mat.SEAT);
        b.seat(0, 0.75, -1.0, 1.1, Mat.SEAT);
        b.box(-0.6, 0.55, 0.55, 0.6, 0.95, 0.7, Mat.DASH);
        b.steeringWheel(0.38, 1.05, 0.5, 0.16, 50);
        b.cyl(0.4, 0.55, -1.38, 'y', 0.13, 0.13, 0.8, 8, Mat.BLACK, Mat.BLACK);             // golf bag
        b.box(0.32, 1.35, -1.45, 0.48, 1.55, -1.3, Mat.CHROME);
        b.pair(0.3, 0.6, -1.42, 0.48, 0.7, -1.4, Mat.TAILLIGHT);
        b.wheels(0.55, 0.25, 0.9, -0.9, 0.25, 0.2);
        return b.build();
    }

    private static Model garbageTruck() {
        Builder b = Model.builder();
        double w = 1.25;
        // cab-over cab with an interior
        b.box(-w, 0.6, 2.6, w, 1.1, 4.3, Mat.METAL);
        b.box(-w, 1.1, 4.2, w, 1.7, 4.3, Mat.PAINT);
        b.pane(w - 0.05, -w + 0.05, 1.7, 4.28, w - 0.05, -w + 0.05, 2.9, 4.24, Mat.GLASS);
        b.box(-w, 2.9, 2.6, w, 3.2, 4.3, Mat.PAINT);
        b.pair(w - 0.1, 1.1, 2.6, w, 1.7, 4.3, Mat.PAINT);
        b.sidePane(w - 0.05, 1.7, 2.9, 2.7, 4.2, 2.7, 4.2, Mat.GLASS);
        b.sidePane(-w + 0.05, 1.7, 2.9, 2.7, 4.2, 2.7, 4.2, Mat.GLASS);
        b.box(-w, 1.1, 2.6, w, 2.9, 2.7, Mat.PAINT);
        b.box(-0.9, 0.7, 4.3, 0.9, 1.05, 4.34, Mat.GRILLE);
        b.pair(0.75, 0.75, 4.3, 1.1, 0.95, 4.35, Mat.HEADLIGHT);
        b.box(-w, 0.45, 4.3, w, 0.65, 4.5, Mat.BLACK);
        b.pair(w, 2.0, 4.0, w + 0.25, 2.5, 4.15, Mat.BLACK);
        b.box(-0.35, 3.2, 3.4, 0.35, 3.32, 3.6, Mat.INDICATOR);                              // beacon
        b.seat(0.55, 1.6, 3.3, 0.6, Mat.SEAT);
        b.seat(-0.55, 1.6, 3.3, 0.6, Mat.SEAT);
        b.dashboard(w - 0.1, -w + 0.1, 1.75, 4.0, 0.55);
        b.steeringWheel(0.55, 1.85, 3.75, 0.22, 50);
        // compactor body and rear loader
        b.box(-0.75, 0.55, -4.4, 0.75, 0.8, 2.6, Mat.METAL);
        b.box(-w, 0.8, -4.2, w, 3.2, 2.5, Mat.PAINT);
        b.hull(-w, w, -4.2, 2.5, 3.2, -w + 0.25, w - 0.25, -4.2, 2.5, 3.35, Mat.PAINT, Mat.PAINT);
        b.pair(w, 1.4, -4.2, w + 0.012, 1.6, 2.5, Mat.WHITE);
        b.box(-w, 0.8, -4.65, w, 2.7, -4.2, Mat.STEEL);
        b.box(-0.9, 1.2, -4.66, 0.9, 2.2, -4.64, Mat.BLACK);
        b.box(-w, 0.8, -4.8, w, 0.95, -4.65, Mat.STEEL);                                    // riding step
        b.pair(0.85, 1.0, -4.67, 1.15, 1.3, -4.64, Mat.TAILLIGHT);
        b.pair(0.95, 2.4, -4.67, 1.15, 2.6, -4.64, Mat.INDICATOR);
        b.wheel(1.05, 0.6, 3.4, 0.6, 0.4, true);
        b.wheel(-1.05, 0.6, 3.4, 0.6, 0.4, true);
        for (double z : new double[]{-1.8, -3.0}) {
            b.wheel(1.05, 0.6, z, 0.6, 0.6, false);
            b.wheel(-1.05, 0.6, z, 0.6, 0.6, false);
        }
        return b.build();
    }

    private static Model mixer() {
        Builder b = Model.builder();
        double w = 1.25;
        truckCab(b, w, 2.0, 3.1, 1.15, 3.0, 1.2);
        b.box(-0.75, 0.6, -4.2, 0.75, 1.2, 2.0, Mat.METAL);
        b.box(-w, 1.2, -4.2, w, 1.35, 1.9, Mat.METAL);
        b.box(-0.2, 1.35, 0.6, 0.2, 2.0, 1.0, Mat.METAL);                                    // front pedestal
        b.box(-0.3, 1.35, -3.9, 0.3, 1.8, -3.5, Mat.METAL);
        b.cyl(0.9, 2.2, 1.2, 'y', 0.3, 0.3, 0.8, 10, Mat.STEEL, Mat.STEEL);                 // water tank
        b.hull(-0.3, 0.3, -4.6, -4.2, 2.0, -0.25, 0.25, -5.0, -4.6, 1.6, Mat.METAL, Mat.METAL);   // chute
        b.pair(0.85, 1.0, -4.27, 1.1, 1.25, -4.2, Mat.TAILLIGHT);
        // rotating drum with fins so you can see it turn
        b.group(Kind.DRUM, 0, 2.3, 0, 0, false);
        b.cyl(0, 2.3, -3.6, 'z', 0.6, 1.15, 1.6, 16, Mat.PAINT, Mat.STEEL);
        b.cyl(0, 2.3, -2.0, 'z', 1.15, 1.15, 1.2, 16, Mat.PAINT, null);
        b.cyl(0, 2.3, -0.8, 'z', 1.15, 0.5, 1.1, 16, Mat.PAINT, Mat.STEEL);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            double x = Math.sin(a) * 1.16, y = 2.3 + Math.cos(a) * 1.16;
            b.box(x - 0.08, y - 0.08, -2.6 + i * 0.2, x + 0.08, y + 0.08, -0.6 + i * 0.2, Mat.ORANGE);
        }
        b.body();
        b.wheel(1.05, 0.62, 3.6, 0.62, 0.42, true);
        b.wheel(-1.05, 0.62, 3.6, 0.62, 0.42, true);
        for (double z : new double[]{-1.8, -3.1}) {
            b.wheel(1.05, 0.62, z, 0.62, 0.6, false);
            b.wheel(-1.05, 0.62, z, 0.62, 0.6, false);
        }
        return b.build();
    }

    private static Model forklift() {
        Builder b = Model.builder();
        b.box(-0.6, 0.3, -1.2, 0.6, 1.1, -0.4, Mat.PAINT);                                  // counterweight
        b.box(-0.6, 0.2, -0.6, 0.6, 0.7, 1.0, Mat.PAINT);                                   // chassis
        b.pair(0.5, 0.7, -0.5, 0.56, 2.3, -0.44, Mat.BLACK);                                 // overhead guard
        b.pair(0.5, 0.7, 0.9, 0.56, 2.3, 0.96, Mat.BLACK);
        b.box(-0.56, 2.3, -0.55, 0.56, 2.36, 0.98, Mat.GRILLE);
        b.seat(0, 1.25, -0.5, 0.55, Mat.BLACK);
        b.box(-0.04, 0.7, 0.2, 0.04, 1.55, 0.3, Mat.BLACK);
        b.steeringWheel(0, 1.6, 0.25, 0.17, 45);
        b.box(0.15, 0.7, 0.5, 0.25, 1.3, 0.6, Mat.BLACK);                                    // levers
        b.pair(0.35, 0.2, 1.05, 0.45, 2.45, 1.15, Mat.METAL);                                // mast
        b.box(-0.45, 2.35, 1.05, 0.45, 2.45, 1.15, Mat.METAL);
        b.pair(0.3, 2.3, 1.1, 0.42, 2.42, 1.16, Mat.HEADLIGHT);
        b.box(-0.7, 2.36, -0.3, -0.56, 2.48, -0.1, Mat.INDICATOR);                           // beacon
        b.group(Kind.FORKS, 0, 0, 0, 0, false);
        b.box(-0.45, 0.25, 1.15, 0.45, 0.85, 1.22, Mat.BLACK);                               // carriage
        b.pair(0.2, 0.2, 1.22, 0.32, 0.8, 1.3, Mat.STEEL);
        b.pair(0.2, 0.2, 1.3, 0.32, 0.27, 2.4, Mat.STEEL);
        b.box(-0.55, 0.27, 1.35, 0.55, 0.4, 2.4, Mat.WOOD);                                  // pallet
        b.box(-0.45, 0.4, 1.45, 0.45, 0.95, 2.3, Mat.CANVAS);                                // load
        b.body();
        b.wheel(0.5, 0.3, 0.7, 0.3, 0.25, false);
        b.wheel(-0.5, 0.3, 0.7, 0.3, 0.25, false);
        b.wheel(0.5, 0.25, -0.8, 0.25, 0.2, true);
        b.wheel(-0.5, 0.25, -0.8, 0.25, 0.2, true);
        return b.build();
    }

    private static Model snowmobile() {
        Builder b = Model.builder();
        b.hull(-0.45, 0.45, -1.3, 1.0, 0.25, -0.38, 0.38, -1.2, 0.8, 0.75, Mat.PAINT, Mat.PAINT);
        b.hull(-0.4, 0.4, 0.6, 1.45, 0.3, -0.25, 0.25, 0.65, 1.1, 0.85, Mat.PAINT, Mat.PAINT);   // hood
        b.pane(0.3, -0.3, 0.85, 0.75, 0.25, -0.25, 1.15, 0.6, Mat.GLASS);
        b.box(-0.25, 0.75, -1.2, 0.25, 0.88, 0.0, Mat.BLACK);                                // seat
        b.box(-0.3, 0.0, -1.4, 0.3, 0.3, 0.2, Mat.TREAD);                                    // track
        b.pair(0.45, 0.3, -1.1, 0.5, 0.5, 0.4, Mat.WHITE);
        b.box(-0.12, 0.55, 1.42, 0.12, 0.68, 1.47, Mat.HEADLIGHT);
        b.box(-0.15, 0.6, -1.33, 0.15, 0.68, -1.3, Mat.TAILLIGHT);
        b.group(Kind.BIKE_FORK, 0, 0.3, 1.0, 0, false);                                       // steering skis
        b.pair(0.33, 0.0, 0.4, 0.5, 0.06, 1.5, Mat.METAL);
        b.pair(0.33, 0.06, 1.45, 0.5, 0.18, 1.6, Mat.METAL);
        b.pair(0.38, 0.06, 0.85, 0.44, 0.35, 0.95, Mat.METAL);
        b.body();
        b.group(Kind.BIKE_FORK, 0, 1.0, 0.55, 0, false);                                      // handlebar
        b.box(-0.04, 0.75, 0.5, 0.04, 1.0, 0.58, Mat.BLACK);
        b.box(-0.4, 0.98, 0.48, 0.4, 1.03, 0.55, Mat.BLACK);
        b.pair(0.32, 0.96, 0.46, 0.44, 1.05, 0.57, Mat.RUBBER);
        b.body();
        b.gauge(0, 0.95, 0.62, 0.1);
        return b.build();
    }

    private static Model hovercraft() {
        Builder b = Model.builder();
        b.box(-1.2, 0.0, -2.3, 1.2, 0.5, 2.3, Mat.RUBBER);                                   // skirt
        b.hull(-1.2, 1.2, 2.3, 2.6, 0.0, -0.9, 0.9, 2.3, 2.5, 0.5, Mat.RUBBER, Mat.RUBBER);
        b.box(-1.1, 0.5, -2.4, 1.1, 0.75, 2.3, Mat.PAINT);                                   // deck
        b.hull(-1.1, 1.1, 2.3, 2.6, 0.5, -0.8, 0.8, 2.3, 2.45, 0.75, Mat.PAINT, Mat.PAINT);
        b.pair(1.05, 0.75, -1.4, 1.1, 1.3, 1.6, Mat.PAINT);                                  // cockpit sides
        b.pane(1.0, -1.0, 0.75, 1.55, 0.9, -0.9, 1.6, 1.2, Mat.GLASS);
        b.box(-1.0, 0.75, 1.55, 1.0, 0.8, 1.65, Mat.PAINT);
        b.seat(0.4, 0.95, 0.3, 0.55, Mat.SEAT);
        b.seat(-0.4, 0.95, 0.3, 0.55, Mat.SEAT);
        b.seat(0.4, 0.95, -0.8, 0.55, Mat.SEAT);
        b.seat(-0.4, 0.95, -0.8, 0.55, Mat.SEAT);
        b.dashboard(0.9, -0.9, 1.15, 1.25, 0.4);
        b.pair(0.7, 0.6, 2.42, 0.95, 0.72, 2.47, Mat.HEADLIGHT);
        b.cyl(0, 1.6, -2.4, 'z', 1.0, 1.0, 0.45, 16, Mat.PAINT_DARK, null);                 // fan duct
        b.box(-0.08, 0.75, -2.2, 0.08, 1.6, -2.0, Mat.METAL);
        b.pair(0.3, 0.75, -2.95, 0.36, 2.4, -2.55, Mat.PAINT_DARK);                          // rudders
        b.pair(0.9, 0.85, -2.5, 1.05, 1.0, -2.4, Mat.TAILLIGHT);
        b.group(Kind.PROP_Z, 0, 1.6, -2.15, 0, false);
        b.box(-0.08, 0.7, -2.18, 0.08, 2.5, -2.12, Mat.ROTOR);
        b.box(-0.9, 1.52, -2.18, 0.9, 1.68, -2.12, Mat.ROTOR);
        b.cyl(0, 1.6, -2.1, 'z', 0.15, 0.0, 0.2, 8, Mat.CHROME, null);
        b.body();
        return b.build();
    }

    private static Model balloon() {
        Builder b = Model.builder();
        // wicker basket
        b.box(-0.75, 0.0, -0.75, 0.75, 0.1, 0.75, Mat.WICKER);
        b.pair(0.68, 0.1, -0.75, 0.75, 1.1, 0.75, Mat.WICKER);
        b.box(-0.68, 0.1, 0.68, 0.68, 1.1, 0.75, Mat.WICKER);
        b.box(-0.68, 0.1, -0.75, 0.68, 1.1, -0.68, Mat.WICKER);
        b.pair(0.66, 1.1, -0.77, 0.77, 1.18, 0.77, Mat.CANVAS);
        b.box(-0.77, 1.1, 0.66, 0.77, 1.18, 0.77, Mat.CANVAS);
        b.box(-0.77, 1.1, -0.77, 0.77, 1.18, -0.66, Mat.CANVAS);
        b.cyl(0.45, 0.1, -0.45, 'y', 0.13, 0.13, 0.6, 8, Mat.STEEL, Mat.METAL);              // gas bottles
        b.cyl(-0.45, 0.1, -0.45, 'y', 0.13, 0.13, 0.6, 8, Mat.STEEL, Mat.METAL);
        // ropes and burner frame
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                b.hull(sx * 0.7 - 0.02, sx * 0.7 + 0.02, sz * 0.7 - 0.02, sz * 0.7 + 0.02, 1.1,
                        sx * 0.32 - 0.02, sx * 0.32 + 0.02, sz * 0.32 - 0.02, sz * 0.32 + 0.02, 3.3, Mat.BLACK, Mat.BLACK);
                b.hull(sx * 0.32 - 0.02, sx * 0.32 + 0.02, sz * 0.32 - 0.02, sz * 0.32 + 0.02, 3.3,
                        sx * 1.6 - 0.02, sx * 1.6 + 0.02, sz * 1.6 - 0.02, sz * 1.6 + 0.02, 4.6, Mat.BLACK, Mat.BLACK);
            }
        }
        b.box(-0.35, 3.25, -0.35, 0.35, 3.45, 0.35, Mat.METAL);
        b.group(Kind.FLAME, 0, 3.45, 0, 0, false);
        b.cyl(0, 3.45, 0, 'y', 0.18, 0.0, 1.3, 8, Mat.FLAME, null);
        b.body();
        // envelope: striped gores, narrowing to a throat over the burner
        b.cyl(0, 4.4, 0, 'y', 0.9, 2.4, 1.2, 16, Mat.BALLOON, null);
        sphere(b, 0, 7.6, 0, 3.5, 3.4, 10, 16, (i, j) -> i < 2 ? Mat.PAINT : Mat.BALLOON);
        return b.build();
    }

    // ================================================================ wave 4: construction

    private static void tracks(Builder b, double xIn, double xOut, double z0, double z1, double h) {
        b.pair(xIn, 0.0, z0, xOut, h, z1, Mat.TREAD);
        double r = h / 2;
        for (int sd = -1; sd <= 1; sd += 2) {
            double x = sd * (xIn + xOut) / 2;
            b.cyl(x - (xOut - xIn) / 2 * sd, r, z0, 'x', r, r, sd * (xOut - xIn), 10, Mat.TREAD, Mat.METAL);
            b.cyl(x - (xOut - xIn) / 2 * sd, r, z1, 'x', r, r, sd * (xOut - xIn), 10, Mat.TREAD, Mat.METAL);
        }
        b.pair(xIn - 0.02, h * 0.25, z0 + 0.2, xIn, h * 0.75, z1 - 0.2, Mat.METAL);
    }

    private static Model excavator() {
        Builder b = Model.builder();
        tracks(b, 0.75, 1.3, -1.9, 1.9, 0.8);
        b.box(-0.75, 0.3, -1.4, 0.75, 0.75, 1.4, Mat.METAL);
        b.cyl(0, 0.75, 0, 'y', 0.65, 0.65, 0.25, 14, Mat.METAL, Mat.METAL);              // slew ring
        // the house swings round with the operator's view
        b.group(Kind.TURRET, 0, 1.0, 0, 0, false);
        b.box(-1.15, 1.0, -1.7, 1.15, 1.15, 1.1, Mat.METAL);                            // deck
        b.box(-1.15, 1.15, -1.7, 0.1, 2.1, 0.4, Mat.PAINT);                             // engine housing
        b.box(-1.2, 1.0, -2.2, 1.2, 2.0, -1.7, Mat.PAINT_DARK);                         // counterweight
        b.pair(1.0, 1.2, -2.21, 1.15, 1.35, -2.18, Mat.TAILLIGHT);
        b.cyl(-0.8, 2.1, -1.2, 'y', 0.07, 0.07, 0.6, 6, Mat.SMOKESTACK, Mat.BLACK);
        b.box(-1.15, 1.6, 0.4, -1.12, 1.8, 0.6, Mat.GRILLE);
        // cab
        b.box(0.15, 1.15, -0.45, 1.15, 2.75, -0.4, Mat.PAINT);
        b.pane(1.15, 0.15, 1.15, 1.1, 1.15, 0.15, 2.65, 1.08, Mat.GLASS);
        b.sidePane(1.15, 1.15, 2.65, -0.4, 1.1, -0.4, 1.1, Mat.GLASS);
        b.sidePane(0.15, 1.15, 2.65, -0.4, 1.1, -0.4, 1.1, Mat.GLASS);
        b.box(0.12, 2.65, -0.45, 1.18, 2.78, 1.12, Mat.PAINT);
        b.pair(0.13, 1.15, 1.05, 0.17, 2.7, 1.12, Mat.BLACK);
        b.seat(0.55, 1.55, 0.3, 0.5, Mat.BLACK);
        b.box(0.25, 1.4, 0.45, 0.35, 1.75, 0.55, Mat.BLACK);                           // joysticks
        b.box(0.75, 1.4, 0.45, 0.85, 1.75, 0.55, Mat.BLACK);
        b.box(0.3, 2.3, 1.05, 0.5, 2.42, 1.12, Mat.HEADLIGHT);
        b.box(0.12, 2.78, 0.2, 0.32, 2.9, 0.4, Mat.INDICATOR);                          // beacon
        // boom, stick and bucket: lift and lower as you look up and down
        b.group(Kind.BOOM, -0.3, 1.6, 1.0, 50, false);
        b.hull(-0.5, -0.1, 0.9, 1.4, 1.45, -0.48, -0.12, 2.8, 3.3, 3.3, Mat.PAINT, Mat.PAINT);
        b.hull(-0.48, -0.12, 3.8, 4.2, 1.3, -0.48, -0.12, 2.9, 3.3, 3.35, Mat.PAINT, Mat.PAINT);
        b.hull(-0.34, -0.26, 1.2, 1.4, 1.2, -0.34, -0.26, 2.2, 2.4, 2.7, Mat.CHROME, Mat.CHROME);   // ram
        b.box(-0.75, 0.6, 3.8, 0.15, 1.3, 4.5, Mat.METAL);                               // bucket
        b.box(-0.75, 1.3, 3.75, 0.15, 1.4, 4.0, Mat.METAL);
        b.box(-0.75, 0.52, 4.42, 0.15, 0.62, 4.62, Mat.STEEL);                           // teeth
        b.body();
        return b.build();
    }

    private static Model bulldozer() {
        Builder b = Model.builder();
        tracks(b, 0.8, 1.3, -2.0, 1.6, 0.85);
        b.box(-0.8, 0.4, -2.1, 0.8, 1.5, 1.8, Mat.PAINT);
        b.box(-0.7, 1.5, 0.3, 0.7, 2.0, 1.8, Mat.PAINT);                                 // engine hood
        b.box(-0.6, 0.7, 1.8, 0.6, 1.9, 1.85, Mat.GRILLE);
        b.cyl(0.4, 2.0, 1.2, 'y', 0.07, 0.07, 0.8, 6, Mat.SMOKESTACK, Mat.BLACK);
        b.pair(0.75, 1.5, -1.95, 0.85, 3.0, -1.85, Mat.BLACK);                           // cab posts
        b.pair(0.75, 1.5, -0.1, 0.85, 3.0, 0.0, Mat.BLACK);
        b.box(-0.9, 2.95, -2.05, 0.9, 3.08, 0.05, Mat.PAINT);
        b.pane(0.78, -0.78, 1.5, 0.0, 0.78, -0.78, 2.95, -0.02, Mat.GLASS);
        b.sidePane(0.8, 1.9, 2.95, -1.85, -0.1, -1.85, -0.1, Mat.GLASS);
        b.sidePane(-0.8, 1.9, 2.95, -1.85, -0.1, -1.85, -0.1, Mat.GLASS);
        b.seat(0, 1.65, -1.0, 0.6, Mat.BLACK);
        b.box(-0.6, 1.5, -0.4, 0.6, 1.9, -0.1, Mat.DASH);
        b.pair(0.4, 2.75, 0.03, 0.6, 2.9, 0.08, Mat.HEADLIGHT);
        b.box(-0.2, 3.08, -1.2, 0.2, 3.2, -0.8, Mat.INDICATOR);
        b.box(-0.15, 0.2, -2.8, 0.15, 1.2, -2.1, Mat.METAL);                             // ripper
        b.box(-0.12, 0.0, -2.9, 0.12, 0.3, -2.7, Mat.STEEL);
        b.group(Kind.FORKS, 0, 0, 0, 0, false);                                           // blade (raised when off)
        b.pair(0.85, 0.5, 1.6, 1.0, 0.8, 2.7, Mat.METAL);
        b.box(-1.45, 0.0, 2.7, 1.45, 1.3, 2.95, Mat.PAINT);
        b.box(-1.45, 0.0, 2.9, 1.45, 0.15, 3.02, Mat.STEEL);
        b.pair(0.3, 0.9, 1.9, 0.4, 1.6, 2.7, Mat.CHROME);
        b.body();
        return b.build();
    }

    private static Model loader() {
        Builder b = Model.builder();
        b.box(-0.9, 0.7, -2.7, 0.9, 1.9, -0.3, Mat.PAINT);                               // rear frame / engine
        b.box(-0.8, 0.8, -2.72, 0.8, 1.7, -2.7, Mat.GRILLE);
        b.box(-0.6, 0.7, -0.3, 0.6, 1.3, 1.5, Mat.PAINT);                                // front frame
        b.pair(0.5, 0.85, -2.74, 0.75, 1.05, -2.7, Mat.TAILLIGHT);
        b.cyl(-0.5, 1.9, -2.0, 'y', 0.07, 0.07, 0.9, 6, Mat.SMOKESTACK, Mat.BLACK);
        b.box(-0.75, 1.9, -1.0, 0.75, 2.0, 0.45, Mat.METAL);                             // cab floor
        b.pane(0.75, -0.75, 2.0, 0.45, 0.75, -0.75, 3.35, 0.4, Mat.GLASS);
        b.pane(0.75, -0.75, 2.0, -1.0, 0.75, -0.75, 3.35, -1.0, Mat.GLASS);
        b.sidePane(0.75, 2.0, 3.35, -1.0, 0.45, -1.0, 0.45, Mat.GLASS);
        b.sidePane(-0.75, 2.0, 3.35, -1.0, 0.45, -1.0, 0.45, Mat.GLASS);
        b.pair(0.7, 2.0, 0.38, 0.78, 3.4, 0.47, Mat.BLACK);
        b.pair(0.7, 2.0, -1.05, 0.78, 3.4, -0.97, Mat.BLACK);
        b.box(-0.8, 3.35, -1.1, 0.8, 3.48, 0.5, Mat.PAINT);
        b.box(-0.2, 3.48, -0.5, 0.2, 3.6, -0.2, Mat.INDICATOR);
        b.seat(0, 2.3, -0.25, 0.55, Mat.BLACK);
        b.steeringWheel(0, 2.75, 0.2, 0.18, 45);
        b.pair(0.3, 3.2, 0.48, 0.55, 3.3, 0.52, Mat.HEADLIGHT);
        b.group(Kind.FORKS, 0, 0, 0, 0, false);                                           // bucket arms
        b.pair(0.55, 0.9, 0.8, 0.7, 1.2, 2.6, Mat.METAL);
        b.box(-1.25, 0.1, 2.5, 1.25, 1.0, 2.62, Mat.METAL);
        b.box(-1.25, 0.1, 2.5, 1.25, 0.2, 3.25, Mat.METAL);
        b.pair(1.15, 0.2, 2.5, 1.25, 0.9, 3.2, Mat.METAL);
        b.box(-1.25, 0.05, 3.15, 1.25, 0.15, 3.32, Mat.STEEL);
        b.body();
        b.wheel(1.05, 0.75, 1.5, 0.75, 0.55, true);
        b.wheel(-1.05, 0.75, 1.5, 0.75, 0.55, true);
        b.wheel(1.05, 0.75, -1.6, 0.75, 0.55, false);
        b.wheel(-1.05, 0.75, -1.6, 0.75, 0.55, false);
        return b.build();
    }

    private static Model roller() {
        Builder b = Model.builder();
        b.wheel(0, 0.7, 1.4, 0.7, 1.9, true);                                            // front drum
        b.pair(0.98, 0.6, 1.0, 1.08, 1.6, 1.8, Mat.PAINT);
        b.box(-1.08, 1.5, 0.9, 1.08, 1.7, 1.9, Mat.PAINT);
        b.box(-0.85, 0.5, -2.0, 0.85, 1.6, 0.6, Mat.PAINT);                             // engine body
        b.box(-0.8, 0.6, -2.02, 0.8, 1.4, -2.0, Mat.GRILLE);
        b.box(-0.6, 1.6, -1.2, 0.6, 1.75, 0.3, Mat.METAL);                               // platform
        b.seat(0, 1.75, -0.6, 0.55, Mat.BLACK);
        b.box(-0.04, 1.6, -0.15, 0.04, 2.05, -0.05, Mat.BLACK);
        b.steeringWheel(0, 2.1, -0.1, 0.2, 40);
        b.pair(0.65, 1.75, -1.3, 0.72, 3.0, -1.22, Mat.BLACK);                           // canopy
        b.pair(0.65, 1.75, 0.3, 0.72, 3.0, 0.38, Mat.BLACK);
        b.box(-0.85, 3.0, -1.4, 0.85, 3.1, 0.5, Mat.PAINT);
        b.box(-0.2, 3.1, -0.6, 0.2, 3.22, -0.3, Mat.INDICATOR);
        b.pair(0.4, 1.3, 1.9, 0.7, 1.45, 1.93, Mat.HEADLIGHT);
        b.pair(0.5, 1.2, -2.03, 0.75, 1.35, -2.0, Mat.TAILLIGHT);
        b.cyl(-0.5, 1.6, -1.6, 'y', 0.06, 0.06, 0.7, 6, Mat.SMOKESTACK, Mat.BLACK);
        b.wheel(0.85, 0.55, -1.5, 0.55, 0.5, false);
        b.wheel(-0.85, 0.55, -1.5, 0.55, 0.5, false);
        return b.build();
    }

    private static Model crane() {
        Builder b = Model.builder();
        double w = 1.25;
        b.box(-1.2, 0.7, -4.6, 1.2, 1.3, 4.1, Mat.PAINT);                                // carrier
        // cab
        b.box(-w, 1.1, 3.0, w, 1.25, 4.6, Mat.METAL);
        b.box(-w, 1.25, 4.5, w, 1.8, 4.6, Mat.PAINT);
        b.pane(w - 0.05, -w + 0.05, 1.8, 4.58, w - 0.05, -w + 0.05, 2.8, 4.52, Mat.GLASS);
        b.box(-w, 2.8, 3.0, w, 3.0, 4.6, Mat.PAINT);
        b.pair(w - 0.1, 1.25, 3.0, w, 1.8, 4.6, Mat.PAINT);
        b.sidePane(w - 0.05, 1.8, 2.8, 3.1, 4.5, 3.1, 4.5, Mat.GLASS);
        b.sidePane(-w + 0.05, 1.8, 2.8, 3.1, 4.5, 3.1, 4.5, Mat.GLASS);
        b.box(-w, 1.25, 3.0, w, 2.8, 3.1, Mat.PAINT);
        b.box(-0.9, 0.8, 4.6, 0.9, 1.15, 4.64, Mat.GRILLE);
        b.pair(0.8, 0.85, 4.6, 1.15, 1.05, 4.65, Mat.HEADLIGHT);
        b.seat(0.5, 1.6, 3.6, 0.6, Mat.SEAT);
        b.seat(-0.5, 1.6, 3.6, 0.6, Mat.SEAT);
        b.dashboard(w - 0.1, -w + 0.1, 1.75, 4.3, 0.5);
        b.steeringWheel(0.5, 1.95, 4.05, 0.22, 55);
        b.pair(1.2, 0.6, -3.7, 1.7, 0.8, -3.3, Mat.METAL);                               // outriggers
        b.pair(1.2, 0.6, 2.4, 1.7, 0.8, 2.8, Mat.METAL);
        b.pair(1.55, 0.0, -3.75, 1.85, 0.6, -3.25, Mat.METAL);
        b.pair(1.55, 0.0, 2.35, 1.85, 0.6, 2.85, Mat.METAL);
        b.pair(0.9, 0.9, -4.62, 1.15, 1.15, -4.6, Mat.TAILLIGHT);
        // house + boom swing and lift with the operator's view
        b.group(Kind.TURRET, 0, 1.9, -1.8, 0, false);
        b.box(-1.1, 1.3, -3.6, 1.1, 2.4, -0.6, Mat.PAINT);
        b.box(-1.15, 1.3, -4.5, 1.15, 2.3, -3.6, Mat.PAINT_DARK);                        // counterweight
        b.box(0.6, 1.4, -1.5, 1.1, 2.6, -0.6, Mat.GLASS);
        b.box(-0.2, 2.4, -2.6, 0.2, 2.5, -1.0, Mat.INDICATOR);
        b.group(Kind.BOOM, 0, 2.2, -1.0, 5, false);
        b.box(-0.38, 1.95, -1.6, 0.38, 2.45, 5.0, Mat.PAINT);
        b.box(-0.3, 2.0, 5.0, 0.3, 2.4, 8.5, Mat.YELLOW);
        b.box(-0.22, 2.05, 8.5, 0.22, 2.35, 10.4, Mat.PAINT);
        b.cyl(0, 2.2, 10.4, 'x', 0.25, 0.25, 0.2, 10, Mat.METAL, Mat.METAL);              // sheave
        b.box(-0.03, -0.4, 10.4, 0.03, 2.2, 10.46, Mat.BLACK);                           // cable
        b.box(-0.22, -0.9, 10.22, 0.22, -0.4, 10.64, Mat.YELLOW);                        // hook block
        b.box(-0.06, -1.2, 10.37, 0.06, -0.9, 10.49, Mat.STEEL);
        b.body();
        for (double z : new double[]{3.2, 1.8, -1.6, -3.0}) {
            b.wheel(1.05, 0.6, z, 0.6, 0.4, z > 0);
            b.wheel(-1.05, 0.6, z, 0.6, 0.4, z > 0);
        }
        return b.build();
    }

    private static Model towTruck() {
        Model m = pickup();
        Builder b = Model.builder();
        b.pair(0.6, 1.0, -2.6, 0.7, 2.4, -2.4, Mat.PAINT_DARK);                          // A-frame
        b.box(-0.7, 2.3, -2.6, 0.7, 2.45, -2.4, Mat.PAINT_DARK);
        b.hull(-0.08, 0.08, -2.55, -2.45, 2.35, -0.08, 0.08, -3.4, -3.3, 1.2, Mat.PAINT_DARK, Mat.PAINT_DARK);   // boom
        b.box(-0.03, 0.55, -3.38, 0.03, 1.2, -3.32, Mat.BLACK);
        b.box(-0.6, 0.3, -3.7, 0.6, 0.45, -3.0, Mat.METAL);                              // wheel lift
        b.pair(0.45, 0.3, -3.7, 0.6, 0.6, -3.55, Mat.METAL);
        b.box(-0.8, 1.6, 0.6, 0.8, 1.68, 0.9, Mat.BLACK);                                // light bar
        b.pair(0.1, 1.68, 0.62, 0.75, 1.8, 0.88, Mat.INDICATOR);
        Model extra = b.build();
        m.groups.addAll(extra.groups);
        return m;
    }
}
