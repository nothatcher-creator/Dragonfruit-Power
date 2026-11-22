package com.nothatcher.outbreak;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Project Zomboid-style character creation, shared by the server (which checks and applies it) and the client
 * (which shows it). An occupation gives you a starting kit and a perk, and points to spend: positive traits cost
 * points, negative ones give you more. You can't start with fewer than zero.
 */
public final class CharacterDefs {
    public record Occupation(String id, int points) {}

    /** cost > 0: a good trait that costs points; cost < 0: a bad one that gives points. opposite: can't have both */
    public record Trait(String id, int cost, String opposite) {}

    public static final Map<String, Occupation> OCCUPATIONS = new LinkedHashMap<>();
    public static final Map<String, Trait> TRAITS = new LinkedHashMap<>();
    /** where you can choose to start */
    public static final List<String> SPAWNS = List.of("suburbs", "downtown", "countryside", "spawn", "random");
    /** how many survivor looks there are (textures/entity/survivor/survivor_N.png); -1 keeps your own skin */
    public static final int LOOKS = 12;

    private static void occ(String id, int points) {
        OCCUPATIONS.put(id, new Occupation(id, points));
    }

    private static void trait(String id, int cost, String opposite) {
        TRAITS.put(id, new Trait(id, cost, opposite));
    }

    static {
        occ("unemployed", 8);
        occ("police_officer", -4);
        occ("firefighter", -2);
        occ("construction_worker", 0);
        occ("doctor", -2);
        occ("mechanic", 0);
        occ("burglar", -2);
        occ("chef", 0);
        occ("park_ranger", -4);
        occ("lumberjack", 0);
        occ("fitness_instructor", -6);
        occ("veteran", -8);

        trait("strong", 6, "weak");
        trait("fast", 4, "slow");
        trait("tough", 6, "frail");
        trait("lucky", 4, "unlucky");
        trait("thick_skinned", 8, "prone_to_illness");
        trait("light_eater", 4, "hearty_appetite");
        trait("fast_healer", 6, null);
        trait("brave", 4, "cowardly");
        trait("weak", -6, "strong");
        trait("slow", -4, "fast");
        trait("frail", -6, "tough");
        trait("unlucky", -4, "lucky");
        trait("prone_to_illness", -6, "thick_skinned");
        trait("hearty_appetite", -4, "light_eater");
        trait("cowardly", -2, "brave");
    }

    private CharacterDefs() {}

    /** points left with this occupation and these traits (must not go below zero) */
    public static int points(String occupation, Collection<String> traits) {
        Occupation o = OCCUPATIONS.get(occupation);
        int p = o == null ? 0 : o.points();
        for (String t : traits) {
            Trait tr = TRAITS.get(t);
            if (tr != null) p -= tr.cost();
        }
        return p;
    }

    public static boolean valid(String occupation, Collection<String> traits) {
        if (!OCCUPATIONS.containsKey(occupation)) return false;
        for (String t : traits) {
            Trait tr = TRAITS.get(t);
            if (tr == null || (tr.opposite() != null && traits.contains(tr.opposite()))) return false;
        }
        return traits.size() == traits.stream().distinct().count() && points(occupation, traits) >= 0;
    }
}
