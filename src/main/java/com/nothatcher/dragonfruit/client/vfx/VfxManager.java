package com.nothatcher.dragonfruit.client.vfx;

import com.nothatcher.dragonfruit.DragonfruitMod;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class VfxManager {
    private static final List<VfxEffect> EFFECTS = new ArrayList<>();
    private static final List<VfxEffect> PENDING = new ArrayList<>();
    private static final int MAX_EFFECTS = 600;
    private static boolean loggedRenderError;

    private VfxManager() {}

    public static void add(VfxEffect effect) {
        if (EFFECTS.size() + PENDING.size() < MAX_EFFECTS) PENDING.add(effect);
    }

    public static void tick(ClientWorld world) {
        EFFECTS.addAll(PENDING);
        PENDING.clear();
        if (world == null) {
            EFFECTS.clear();
            return;
        }
        Iterator<VfxEffect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            VfxEffect e = it.next();
            boolean keep;
            try {
                keep = e.tick(world);
            } catch (Exception ex) {
                DragonfruitMod.LOG.error("VFX tick failed: {}", e.getClass().getSimpleName(), ex);
                keep = false;
            }
            if (!keep) it.remove();
        }
    }

    public static void render(WorldRenderContext ctx) {
        if (EFFECTS.isEmpty()) return;
        VertexConsumerProvider consumers = ctx.consumers();
        MatrixStack matrices = ctx.matrixStack();
        if (consumers == null || matrices == null) return;
        Gfx.cam = ctx.camera().getPos();
        float td = ctx.tickDelta();
        for (VfxEffect e : EFFECTS) {
            try {
                e.render(matrices, consumers, td);
            } catch (Exception ex) {
                if (!loggedRenderError) {
                    loggedRenderError = true;
                    DragonfruitMod.LOG.error("VFX render failed: {}", e.getClass().getSimpleName(), ex);
                }
            }
        }
    }

    /** Is this player's arm currently stretched out? (hides the vanilla first-person arm) */
    public static boolean armActive(int ownerId, int side) {
        for (VfxEffect e : EFFECTS) {
            if (e instanceof ArmEffect arm && arm.ownerId == ownerId && arm.side == side && arm.visible()) return true;
            if (e instanceof GatlingEffect g && g.ownerId == ownerId) return true;
        }
        return false;
    }

    /** Is this entity charging something in its hand (raise the arm in third person)? */
    public static boolean charging(int entityId) {
        for (VfxEffect e : EFFECTS) {
            if (e instanceof OrbEffect orb && orb.isHandChargeOf(entityId)) return true;
        }
        return false;
    }

    public static void clear() {
        EFFECTS.clear();
        PENDING.clear();
    }
}
