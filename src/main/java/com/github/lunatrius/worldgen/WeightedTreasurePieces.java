// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.worldgen;

import java.util.Random;
import net.minecraft.ItemStack;
import net.minecraft.WeightedRandomChestContent;

/**
 * Loot API facade for schematic marker chests.
 * Backed by {@link WorldgenStructureProfiles}.
 */
public final class WeightedTreasurePieces {
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 6;
    static final WeightedRandomChestContent[] EMPTY = new WeightedRandomChestContent[0];

    private WeightedTreasurePieces() {
    }

    public static int getLevelForMarker(ItemStack markerStack) {
        return getLevelForMarker(null, markerStack);
    }

    public static int getLevelForMarker(String structureKey, ItemStack markerStack) {
        if (markerStack == null || markerStack.getItem() == null) {
            return 0;
        }

        WorldgenStructureProfiles.StructureProfile profile = WorldgenStructureProfiles.resolveProfile(structureKey);
        Integer level = profile.getMarkerLevel(markerStack.itemID);
        return level == null ? 0 : level;
    }

    public static WeightedRandomChestContent[] getContentsForLevel(int level) {
        return getContentsForLevel(null, level);
    }

    public static WeightedRandomChestContent[] getContentsForLevel(String structureKey, int level) {
        if (!isValidLevel(level)) {
            return EMPTY;
        }

        WorldgenStructureProfiles.StructureProfile profile = WorldgenStructureProfiles.resolveProfile(structureKey);
        WeightedRandomChestContent[] table = profile.getLootTable(level);
        return table == null ? EMPTY : table;
    }

    public static int getRollCount(Random random, int level) {
        return getRollCount(null, random, level);
    }

    public static int getRollCount(String structureKey, Random random, int level) {
        if (!isValidLevel(level)) {
            return 0;
        }

        WorldgenStructureProfiles.StructureProfile profile = WorldgenStructureProfiles.resolveProfile(structureKey);
        int min = profile.getMinRoll(level);
        int max = profile.getMaxRoll(level);
        if (max < min) {
            max = min;
        }
        if (random == null || max == min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    public static float[] getArtifactChances(int level) {
        return getArtifactChances(null, level);
    }

    public static float[] getArtifactChances(String structureKey, int level) {
        if (!isValidLevel(level)) {
            return null;
        }

        WorldgenStructureProfiles.StructureProfile profile = WorldgenStructureProfiles.resolveProfile(structureKey);
        return profile.getArtifactChances(level);
    }

    private static boolean isValidLevel(int level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }
}
