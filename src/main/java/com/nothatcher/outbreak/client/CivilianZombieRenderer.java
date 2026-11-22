package com.nothatcher.outbreak.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

/**
 * Zombies look like the townsfolk they were: each one wears one of Outbreak's civilian skins (pale, bloodied,
 * in everyday clothes), picked from its UUID so it never changes. Husks, drowned and zombie villagers keep theirs.
 */
public class CivilianZombieRenderer extends ZombieEntityRenderer {
    public static final int SKINS = 16;
    private static final Identifier[] TEXTURES = new Identifier[SKINS];

    static {
        for (int i = 0; i < SKINS; i++) TEXTURES[i] = new Identifier("outbreak", "textures/entity/zombie/civilian_" + i + ".png");
    }

    public CivilianZombieRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    private static final java.util.Map<String, Identifier> JOB_TEXTURES = new java.util.HashMap<>();

    /** the uniform for a zombie's old job (two faces per job), or its civilian clothes */
    public static Identifier skinFor(java.util.UUID id, com.nothatcher.outbreak.ZombieJob job) {
        if (job == null || job == com.nothatcher.outbreak.ZombieJob.CIVILIAN) return skinFor(id);
        String key = job.id() + "_" + (id.getLeastSignificantBits() & 1);
        return JOB_TEXTURES.computeIfAbsent(key, k -> new Identifier("outbreak", "textures/entity/zombie/job_" + k + ".png"));
    }

    public static Identifier skinFor(java.util.UUID id) {
        long h = id.getLeastSignificantBits() ^ id.getMostSignificantBits();
        return TEXTURES[(int) Math.floorMod(h ^ (h >>> 29), (long) SKINS)];
    }

    @Override
    public Identifier getTexture(ZombieEntity zombie) {
        return skinFor(zombie.getUuid(), com.nothatcher.outbreak.ZombieJob.of(zombie));
    }
}
