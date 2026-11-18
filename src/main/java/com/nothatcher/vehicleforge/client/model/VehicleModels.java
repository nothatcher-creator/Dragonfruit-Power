package com.nothatcher.vehicleforge.client.model;

import com.nothatcher.vehicleforge.Weapons;
import com.nothatcher.vehicleforge.client.model.Model.Builder;
import com.nothatcher.vehicleforge.client.model.Model.Kind;

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

    private static Model create(String id) {
        return switch (id) {
            case "classic_cruiser" -> classic();
            case "sedan" -> sedan();
            case "sports_car" -> sports();
            case "pickup_truck" -> pickup();
            case "van" -> van();
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

    private static Model sedan() {
        CarSpec s = new CarSpec(4.6, 0.95, 0.3, 1.0, 1.95, 0.95, 0.4, -1.3, -1.5,
                0.38, 0.8, 1.45, -1.45, 0.7, 0.15, -0.9, 0.85);
        return carShell(s, Mat.GLASS).build();
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

    private static Model van() {
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
        b.seat(0, 0.95, -0.2, 1.7, Mat.SEAT);
        b.seat(0, 0.95, -1.2, 1.7, Mat.SEAT);
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
}
