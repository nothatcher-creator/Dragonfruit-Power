package com.nothatcher.vehicleforge.client;

import com.nothatcher.vehicleforge.VehicleEntity;
import com.nothatcher.vehicleforge.VehicleType.Category;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/** Looping engine sound that follows the vehicle; pitch rises with speed. */
public class EngineSound extends MovingSoundInstance {
    private final VehicleEntity vehicle;

    public EngineSound(VehicleEntity vehicle, SoundEvent sound) {
        super(sound, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.05f;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved()) {
            setDone();
            return;
        }
        x = vehicle.getX();
        y = vehicle.getY() + 1;
        z = vehicle.getZ();
        float frac = Math.min(1.2f, vehicle.displaySpeed() / Math.max(0.1f, vehicle.type.maxSpeed));
        boolean running = vehicle.engineRunning();
        float target;
        if (vehicle.type.category == Category.HELICOPTER) {
            pitch = 0.85f + frac * 0.3f;
            target = running ? 1.0f : 0f;
        } else if (vehicle.type.category == Category.ROCKET) {
            pitch = 0.8f + frac * 0.3f;
            target = vehicle.thrusting ? 1.6f : (running ? 0.15f : 0f);
        } else if (vehicle.type.wheeled() && vehicle.type.gears > 0) {
            // follows the tachometer: climbs through each gear, drops on every upshift
            pitch = 0.55f + vehicle.rpm * 1.15f;
            target = running ? 0.4f + vehicle.rpm * 0.55f + (vehicle.accelVis > 0.004f ? 0.15f : 0f) : 0f;
        } else {
            pitch = 0.65f + frac * 0.95f;
            target = running ? 0.45f + frac * 0.6f : 0f;
        }
        volume += (target - volume) * 0.15f;
        if (!running && volume < 0.02f) setDone();
    }
}
