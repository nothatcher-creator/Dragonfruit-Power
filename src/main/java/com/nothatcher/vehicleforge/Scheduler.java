package com.nothatcher.vehicleforge;

import com.nothatcher.vehicleforge.VehicleForge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server tick scheduler so abilities can time hits to match their visuals. */
public final class Scheduler {
    @FunctionalInterface
    public interface Ticker {
        /** @return true to keep running next tick */
        boolean tick(int age);
    }

    private static final class Task {
        int delay;
        int age;
        final Ticker ticker;

        Task(int delay, Ticker ticker) {
            this.delay = delay;
            this.ticker = ticker;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Scheduler() {}

    public static void after(int delayTicks, Runnable r) {
        PENDING.add(new Task(Math.max(0, delayTicks), age -> {
            r.run();
            return false;
        }));
    }

    /** Runs every tick (starting after delay) until the ticker returns false. */
    public static void loop(int delayTicks, Ticker ticker) {
        PENDING.add(new Task(Math.max(0, delayTicks), ticker));
    }

    public static void tick() {
        TASKS.addAll(PENDING);
        PENDING.clear();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.delay > 0) {
                task.delay--;
                continue;
            }
            boolean keep;
            try {
                keep = task.ticker.tick(task.age++);
            } catch (Exception e) {
                VehicleForge.LOG.error("Vehicle task crashed, cancelled it", e);
                keep = false;
            }
            if (!keep) it.remove();
        }
    }

    public static void clear() {
        TASKS.clear();
        PENDING.clear();
    }
}
