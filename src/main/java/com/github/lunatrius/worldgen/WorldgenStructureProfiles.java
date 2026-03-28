// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.worldgen;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Item;
import net.minecraft.WeightedRandomChestContent;

/**
 * Single source of truth for per-structure worldgen behavior.
 *
 * This profile contains both:
 * 1) Marker chest loot configuration
 * 2) Entity replacement targets by marker level
 */
public final class WorldgenStructureProfiles {
    private static final WeightedRandomChestContent[] EMPTY = new WeightedRandomChestContent[0];
    private static final StructureProfile DEFAULT_PROFILE = createStructure10Profile();
    private static final Map<String, StructureProfile> PROFILE_BY_ALIAS = createProfiles();

    private WorldgenStructureProfiles() {
    }

    public static StructureProfile resolveProfile(String structureKey) {
        StructureProfile profile = findProfile(structureKey);
        return profile != null ? profile : DEFAULT_PROFILE;
    }

    public static StructureProfile findProfile(String structureKey) {
        String normalized = normalizeStructure(structureKey);
        if (normalized.isEmpty()) {
            return null;
        }
        return PROFILE_BY_ALIAS.get(normalized);
    }

    static String normalizeStructure(String structureKey) {
        if (structureKey == null) {
            return "";
        }

        String normalized = structureKey.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    private static Map<String, StructureProfile> createProfiles() {
        LinkedHashMap<String, StructureProfile> profiles = new LinkedHashMap<String, StructureProfile>();

        StructureProfile structure10 = createStructure10Profile();
        registerProfile(
                profiles,
                structure10,
                new String[]{
                        "/assets/schematica/structures/10.schematic",
                        "assets/schematica/structures/10.schematic",
                        "10.schematic",
                        "10"
                }
        );

        return profiles;
    }

    private static void registerProfile(Map<String, StructureProfile> profiles, StructureProfile profile, String[] aliases) {
        if (profiles == null || profile == null || aliases == null) {
            return;
        }

        for (String alias : aliases) {
            String normalized = normalizeStructure(alias);
            if (!normalized.isEmpty()) {
                profiles.put(normalized, profile);
            }
        }
    }

    private static StructureProfile createStructure10Profile() {
        return new StructureProfile(
                "structure-10",
                createDefaultMarkerLevels(),
                new WeightedRandomChestContent[][]{
                        EMPTY,
                        new WeightedRandomChestContent[]{entry(Item.stick, 1, 1, 25)},
                        new WeightedRandomChestContent[]{entry(Item.flint, 1, 1, 20)},
                        new WeightedRandomChestContent[]{entry(Item.coal, 1, 1, 24)},
                        new WeightedRandomChestContent[]{entry(Item.ingotIron, 1, 1, 22)},
                        new WeightedRandomChestContent[]{entry(Item.ingotGold, 1, 1, 22)},
                        new WeightedRandomChestContent[]{entry(Item.diamond, 1, 1, 16)}
                },
                new int[]{0, 3, 3, 4, 4, 5, 6},
                new int[]{0, 5, 5, 6, 7, 8, 9},
                new float[7][],
                new String[]{null, "Zombie", "Skeleton", "Spider"},
                true
        );
    }

    private static Map<Integer, Integer> createDefaultMarkerLevels() {
        LinkedHashMap<Integer, Integer> markerLevels = new LinkedHashMap<Integer, Integer>();
        markerLevels.put(Item.stick.itemID, 1);
        markerLevels.put(Item.flint.itemID, 2);
        markerLevels.put(Item.coal.itemID, 3);
        markerLevels.put(Item.ingotIron.itemID, 4);
        markerLevels.put(Item.ingotGold.itemID, 5);
        markerLevels.put(Item.diamond.itemID, 6);
        return markerLevels;
    }

    private static WeightedRandomChestContent entry(Item item, int minQuantity, int maxQuantity, int weight) {
        return new WeightedRandomChestContent(item.itemID, 0, minQuantity, maxQuantity, weight);
    }

    public static final class StructureProfile {
        private final String profileId;
        private final Map<Integer, Integer> markerLevels;
        private final WeightedRandomChestContent[][] lootTables;
        private final int[] minRolls;
        private final int[] maxRolls;
        private final float[][] artifactChances;
        private final String[] entityLevelTargets;
        private final boolean forceIronSwordForSkeletonReplacements;

        private StructureProfile(
                String profileId,
                Map<Integer, Integer> markerLevels,
                WeightedRandomChestContent[][] lootTables,
                int[] minRolls,
                int[] maxRolls,
                float[][] artifactChances,
                String[] entityLevelTargets,
                boolean forceIronSwordForSkeletonReplacements
        ) {
            this.profileId = profileId;
            this.markerLevels = markerLevels;
            this.lootTables = lootTables;
            this.minRolls = minRolls;
            this.maxRolls = maxRolls;
            this.artifactChances = artifactChances;
            this.entityLevelTargets = entityLevelTargets;
            this.forceIronSwordForSkeletonReplacements = forceIronSwordForSkeletonReplacements;
        }

        public String getProfileId() {
            return this.profileId;
        }

        public Integer getMarkerLevel(int itemId) {
            return this.markerLevels == null ? null : this.markerLevels.get(itemId);
        }

        public WeightedRandomChestContent[] getLootTable(int level) {
            if (this.lootTables == null || level < 0 || level >= this.lootTables.length) {
                return null;
            }
            return this.lootTables[level];
        }

        public int getMinRoll(int level) {
            if (this.minRolls == null || level < 0 || level >= this.minRolls.length) {
                return 0;
            }
            return this.minRolls[level];
        }

        public int getMaxRoll(int level) {
            if (this.maxRolls == null || level < 0 || level >= this.maxRolls.length) {
                return 0;
            }
            return this.maxRolls[level];
        }

        public float[] getArtifactChances(int level) {
            if (this.artifactChances == null || level < 0 || level >= this.artifactChances.length) {
                return null;
            }
            return this.artifactChances[level];
        }

        public String getEntityReplacementForLevel(int level) {
            if (this.entityLevelTargets == null || level < 0 || level >= this.entityLevelTargets.length) {
                return null;
            }
            return this.entityLevelTargets[level];
        }

        public boolean isForceIronSwordForSkeletonReplacements() {
            return this.forceIronSwordForSkeletonReplacements;
        }
    }
}
