package com.nothatcher.vehicleforge;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about one kind of vehicle except its 3D model (models live client-side in VehicleModels,
 * looked up by the same id). Local space everywhere: x = LEFT, y = up, z = forward.
 */
public final class VehicleType {
    public enum Category { CAR, BIKE, TRUCK, MILITARY, BOAT, HELICOPTER, PLANE, ROCKET, TRAIN, RV }

    public enum Weapon { NONE, TANK_CANNON, ROCKET_SALVO, MACHINE_GUN, AIR_ROCKETS, WATER_CANNON }

    public enum Horn { CAR, TRUCK, BIKE_BELL, TRAIN, BOAT }

    public final String id;
    public final Category category;
    public final float width, height;
    public final List<Vec3d> seats = new ArrayList<>();
    public float maxSpeed = 0.9f, accel = 0.03f, brake = 0.06f, turn = 4f;
    public float health = 60f, stepHeight = 1.0f, cameraDistance = 6f;
    public Weapon weapon = Weapon.NONE;
    public Horn horn = Horn.CAR;
    public boolean siren, interiorRoom, amphibious;
    public int defaultPaint = 0xC8202A;
    /** for trains: number of carriages a new train starts with */
    public int carriages = 0;
    public String engineSound = "engine_car";

    VehicleType(String id, Category category, float width, float height) {
        this.id = id;
        this.category = category;
        this.width = width;
        this.height = height;
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
        return category == Category.HELICOPTER || category == Category.PLANE || category == Category.ROCKET;
    }
}
