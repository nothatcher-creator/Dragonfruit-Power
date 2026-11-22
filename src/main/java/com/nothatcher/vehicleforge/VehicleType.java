package com.nothatcher.vehicleforge;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about one kind of vehicle except its 3D model (models live client-side in VehicleModels,
 * looked up by the same id). Local space everywhere: x = LEFT, y = up, z = forward.
 */
public final class VehicleType {
    public enum Category { CAR, BIKE, TRUCK, MILITARY, BOAT, HELICOPTER, PLANE, ROCKET, TRAIN, RV, SUBMARINE, ANIMAL, BALL, BALLOON }

    public enum Weapon {
        NONE, TANK_CANNON, ROCKET_SALVO, MACHINE_GUN, AIR_ROCKETS, WATER_CANNON,
        /** wave 3 specials */
        TIME_JUMP, SHRINK, TRACTOR_BEAM, BOOST, MOWER, COMPACTOR, FORKS,
        /** construction: excavator digs, bulldozer/loader clear land, roller paves, crane lifts, tow truck tows */
        DIG, BLADE, PAVE, CRANE, TOW
    }

    public enum Horn { CAR, TRUCK, BIKE_BELL, TRAIN, BOAT, OSTRICH, ICE_CREAM }

    public final String id;
    public final Category category;
    public final float width, height;
    public final List<Vec3d> seats = new ArrayList<>();
    public float maxSpeed = 0.9f, accel = 0.03f, brake = 0.06f, turn = 4f;
    public float health = 60f, stepHeight = 1.0f, cameraDistance = 6f;
    public Weapon weapon = Weapon.NONE;
    public Horn horn = Horn.CAR;
    public boolean siren, interiorRoom, amphibious;
    /** the build frame: blocks can be placed on its deck and ride along */
    public boolean buildable;
    public int defaultPaint = 0xC8202A;
    /** for trains: number of carriages a new train starts with */
    public int carriages = 0;
    public String engineSound = "engine_car";
    /** model extents along z (front > 0 > rear); axles sit at 70% of these */
    public float frontZ, rearZ;
    /** automatic gearbox: number of forward gears (0 = no gears, continuous rpm) */
    public int gears = 5;
    /** loses little speed on grass, sand, snow and mud */
    public boolean offroad;
    /** hovercraft: slides everywhere, full speed on water; snowmobile: grips and runs fast on snow and ice */
    public boolean hover, snow;
    /** forklift fork / bulldozer blade / loader bucket travel */
    public float lift = 2.0f;
    /** the driver's seat swings round with the turret (excavator cab) */
    public boolean seatTurns;
    /** minutes of running at full speed on one tank (0 = needs no fuel) */
    public float endurance = 20f;
    /** roof gun pivot (local) and barrel length, for ground machine guns */
    public Vec3d turret = new Vec3d(0, 2.55, -1.0);
    public double barrelLen = 1.7;
    /** trains: chimney / exhaust position (null = none), carriage model and spacing */
    public Vec3d smoke;
    public String carriageModel = "train_carriage";
    public float carriageStart = 9.7f;

    public float axleF() {
        return frontZ * 0.7f;
    }

    public float axleR() {
        return rearZ * 0.7f;
    }

    VehicleType(String id, Category category, float width, float height) {
        this.id = id;
        this.category = category;
        this.width = width;
        this.height = height;
        this.frontZ = width * 1.15f;
        this.rearZ = -width * 1.15f;
        if (category == Category.TRUCK || category == Category.RV) gears = 6;
        if (category == Category.BIKE) gears = 5;
        if (category == Category.MILITARY) offroad = true;
        if (category == Category.BOAT || category == Category.HELICOPTER || category == Category.PLANE
                || category == Category.ROCKET || category == Category.TRAIN || category == Category.SUBMARINE
                || category == Category.ANIMAL || category == Category.BALL || category == Category.BALLOON) gears = 0;
    }

    VehicleType len(double front, double rear) {
        this.frontZ = (float) front;
        this.rearZ = (float) rear;
        return this;
    }

    VehicleType gears(int g) {
        this.gears = g;
        return this;
    }

    VehicleType offroad() {
        this.offroad = true;
        return this;
    }

    VehicleType endurance(float minutes) {
        this.endurance = minutes;
        return this;
    }

    VehicleType turret(double x, double y, double z, double barrel) {
        this.turret = new Vec3d(x, y, z);
        this.barrelLen = barrel;
        return this;
    }

    VehicleType smoke(double x, double y, double z) {
        this.smoke = new Vec3d(x, y, z);
        return this;
    }

    VehicleType carriage(String model, float start) {
        this.carriageModel = model;
        this.carriageStart = start;
        return this;
    }

    VehicleType lift(float h) {
        this.lift = h;
        return this;
    }

    VehicleType seatTurns() {
        this.seatTurns = true;
        return this;
    }

    VehicleType hover() {
        this.hover = true;
        this.amphibious = true;
        return this;
    }

    VehicleType snow() {
        this.snow = true;
        this.offroad = true;
        return this;
    }

    VehicleType amphibious() {
        this.amphibious = true;
        return this;
    }

    public boolean needsWater() {
        return category == Category.BOAT || category == Category.SUBMARINE;
    }

    public boolean usesFuel() {
        return endurance > 0;
    }

    /** cars, trucks, bikes: things with wheels on the ground */
    public boolean wheeled() {
        return switch (category) {
            case CAR, BIKE, TRUCK, MILITARY, RV -> true;
            default -> false;
        };
    }

    VehicleType seat(double leftX, double y, double forwardZ) {
        seats.add(new Vec3d(leftX, y, forwardZ));
        return this;
    }

    VehicleType speed(float max, float accel, float turn) {
        this.maxSpeed = max;
        this.accel = accel;
        this.turn = turn;
        return this;
    }

    VehicleType health(float h) {
        this.health = h;
        return this;
    }

    VehicleType camera(float d) {
        this.cameraDistance = d;
        return this;
    }

    VehicleType weapon(Weapon w) {
        this.weapon = w;
        return this;
    }

    VehicleType horn(Horn h) {
        this.horn = h;
        return this;
    }

    VehicleType paint(int rgb) {
        this.defaultPaint = rgb;
        return this;
    }

    VehicleType sound(String s) {
        this.engineSound = s;
        return this;
    }

    VehicleType siren() {
        this.siren = true;
        return this;
    }

    VehicleType buildable() {
        this.buildable = true;
        return this;
    }

    VehicleType room() {
        this.interiorRoom = true;
        return this;
    }

    VehicleType step(float s) {
        this.stepHeight = s;
        return this;
    }

    VehicleType carriages(int n) {
        this.carriages = n;
        return this;
    }

    public boolean flies() {
        return category == Category.HELICOPTER || category == Category.PLANE || category == Category.ROCKET || category == Category.BALLOON;
    }
}
