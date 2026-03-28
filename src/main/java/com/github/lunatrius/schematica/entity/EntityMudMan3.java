// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.entity;

import net.minecraft.World;

public class EntityMudMan3 extends EntityMudManBase {
    public EntityMudMan3(World world) {
        super(world);
    }

    @Override
    protected double getConfiguredMaxHealth() {
        return 64.0D;
    }
}
