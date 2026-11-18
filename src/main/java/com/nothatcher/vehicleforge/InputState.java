package com.nothatcher.vehicleforge;

/**
 * Driving input of the local player, written every client tick by the client mod and read by the
 * vehicle the local player is driving (vehicles are simulated on the driver's client, like boats).
 */
public final class InputState {
    public static volatile boolean forward, back, left, right, up, down;

    private InputState() {}
}
