// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.entity;

import net.minecraft.World;

public class EntityMudMan2 extends EntityMudManBase {
    public EntityMudMan2(World world) {
        super(world);
    }

    @Override
    protected double getConfiguredMaxHealth() {
        return 40.0D;
    }
}
