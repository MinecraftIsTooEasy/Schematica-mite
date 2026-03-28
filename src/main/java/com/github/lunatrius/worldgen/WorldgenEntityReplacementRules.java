// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.worldgen;

import java.util.Locale;
import net.minecraft.NBTTagCompound;

public final class WorldgenEntityReplacementRules {
    private static final int LEVEL_NONE = 0;
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;
    private static final int LEVEL_3 = 3;

    private WorldgenEntityReplacementRules() {
    }

    public static ReplacementDecision resolve(String structureKey, NBTTagCompound sourceTag) {
        if (sourceTag == null) {
            return new ReplacementDecision(null, null, LEVEL_NONE, null, false);
        }

        String sourceId = sourceTag.getString("id");
        if (sourceId == null || sourceId.trim().isEmpty()) {
            return new ReplacementDecision(sourceId, sourceId, LEVEL_NONE, null, false);
        }

        int level = detectLevel(sourceTag, sourceId);
        if (level == LEVEL_NONE) {
            return new ReplacementDecision(sourceId, sourceId, LEVEL_NONE, null, false);
        }

        WorldgenStructureProfiles.StructureProfile profile = WorldgenStructureProfiles.findProfile(structureKey);
        if (profile == null) {
            return new ReplacementDecision(sourceId, sourceId, level, null, false);
        }

        String replacement = profile.getEntityReplacementForLevel(level);
        if (replacement == null || replacement.trim().isEmpty()) {
            return new ReplacementDecision(sourceId, sourceId, level, profile.getProfileId(), false);
        }

        boolean forceIronSwordForSkeleton =
                profile.isForceIronSwordForSkeletonReplacements()
                && "skeleton".equals(normalizeId(replacement))
                && !normalizeId(sourceId).equals(normalizeId(replacement));

        return new ReplacementDecision(sourceId, replacement, level, profile.getProfileId(), forceIronSwordForSkeleton);
    }

    public static String resolveEntityId(String structureKey, NBTTagCompound sourceTag) {
        return resolve(structureKey, sourceTag).replacementId;
    }

    private static int detectLevel(NBTTagCompound sourceTag, String sourceId) {
        int taggedLevel = readTaggedLevel(sourceTag);
        if (taggedLevel != LEVEL_NONE) {
            return taggedLevel;
        }

        int markerLevel = parseMarkerLevel(sourceId);
        if (markerLevel != LEVEL_NONE) {
            return markerLevel;
        }

        String normalized = normalizeId(sourceId);
        if ("schematica".equals(normalized) || "mudman".equals(normalized) || "mud_man".equals(normalized)) {
            return levelByHealth(sourceTag);
        }

        return LEVEL_NONE;
    }

    private static int readTaggedLevel(NBTTagCompound sourceTag) {
        if (sourceTag == null) {
            return LEVEL_NONE;
        }

        if (sourceTag.hasKey("schematica_level")) {
            return clampLevel(sourceTag.getInteger("schematica_level"));
        }

        if (sourceTag.hasKey("level")) {
            return clampLevel(sourceTag.getInteger("level"));
        }

        return LEVEL_NONE;
    }

    private static int parseMarkerLevel(String rawId) {
        String normalized = normalizeId(rawId);
        if ("1".equals(normalized) || "mudman1".equals(normalized) || "mud_man_1".equals(normalized) || "mud_man1".equals(normalized)) {
            return LEVEL_1;
        }
        if ("2".equals(normalized) || "mudman2".equals(normalized) || "mud_man_2".equals(normalized) || "mud_man2".equals(normalized)) {
            return LEVEL_2;
        }
        if ("3".equals(normalized) || "mudman3".equals(normalized) || "mud_man_3".equals(normalized) || "mud_man3".equals(normalized)) {
            return LEVEL_3;
        }
        return LEVEL_NONE;
    }

    private static int levelByHealth(NBTTagCompound sourceTag) {
        float health = sourceTag.getFloat("HealF");
        if (health <= 0.0F) {
            health = sourceTag.getFloat("Health");
        }
        if (health <= 0.0F) {
            return LEVEL_NONE;
        }
        if (health <= 24.0F) {
            return LEVEL_1;
        }
        if (health <= 48.0F) {
            return LEVEL_2;
        }
        return LEVEL_3;
    }

    private static int clampLevel(int level) {
        if (level < LEVEL_1 || level > LEVEL_3) {
            return LEVEL_NONE;
        }
        return level;
    }

    private static String normalizeId(String sourceId) {
        if (sourceId == null) {
            return "";
        }
        String normalized = sourceId.trim().toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            normalized = normalized.substring(colon + 1);
        }
        return normalized;
    }

    public static final class ReplacementDecision {
        private final String sourceId;
        private final String replacementId;
        private final int detectedLevel;
        private final String matchedRuleId;
        private final boolean forceSkeletonIronSword;

        private ReplacementDecision(String sourceId, String replacementId, int detectedLevel, String matchedRuleId, boolean forceSkeletonIronSword) {
            this.sourceId = sourceId;
            this.replacementId = replacementId;
            this.detectedLevel = detectedLevel;
            this.matchedRuleId = matchedRuleId;
            this.forceSkeletonIronSword = forceSkeletonIronSword;
        }

        public String getSourceId() {
            return this.sourceId;
        }

        public String getReplacementId() {
            return this.replacementId;
        }

        public int getDetectedLevel() {
            return this.detectedLevel;
        }

        public String getMatchedRuleId() {
            return this.matchedRuleId;
        }

        public boolean isForceSkeletonIronSword() {
            return this.forceSkeletonIronSword;
        }
    }
}
