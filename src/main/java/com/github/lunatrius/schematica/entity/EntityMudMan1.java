// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.entity;

import net.minecraft.World;

public class EntityMudMan1 extends EntityMudManBase {
    public EntityMudMan1(World world) {
        super(world);
    }

    @Override
    protected double getConfiguredMaxHealth() {
        return 20.0D;
    }
}
