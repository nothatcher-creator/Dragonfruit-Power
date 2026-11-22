package com.nothatcher.outbreak.client;

import com.nothatcher.outbreak.Skills;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/** your skills and your safehouse, as the server last told us */
public final class SkillsClient {
    public static int[] xp = new int[Skills.Skill.values().length];
    public static BlockBox safehouse;

    private SkillsClient() {}

    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Skills.SYNC, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            int[] a = new int[Math.max(n, Skills.Skill.values().length)];
            for (int i = 0; i < n; i++) a[i] = buf.readVarInt();
            BlockBox box = buf.readBoolean() ? new BlockBox(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()) : null;
            client.execute(() -> {
                xp = a;
                safehouse = box;
            });
        });
    }

    public static boolean inSafehouse(BlockPos p) {
        return safehouse != null && safehouse.contains(p);
    }
}
