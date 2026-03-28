// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.worldgen;

import moddedmite.rustedironcore.api.event.events.BiomeDecorationRegisterEvent;
import moddedmite.rustedironcore.api.world.Dimension;

public final class SchematicWorldgenRegistration {
    private static final Dimension TARGET_DIMENSION = Dimension.OVERWORLD;
    private static final int WEIGHT_10 = 1;
    private static final int CHANCE_10 = 40;

    private SchematicWorldgenRegistration() {
    }

    public static void registerExample(BiomeDecorationRegisterEvent event) {
        event.register(TARGET_DIMENSION, new Test1SchematicWorldGenerator(), WEIGHT_10)
                .setChance(CHANCE_10)
                .setSurface();
    }
}
