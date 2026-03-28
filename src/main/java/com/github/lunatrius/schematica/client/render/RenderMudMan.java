// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.client.render;

import net.minecraft.Entity;
import net.minecraft.ModelBase;
import net.minecraft.RenderLiving;
import net.minecraft.ResourceLocation;

public class RenderMudMan extends RenderLiving {
    public RenderMudMan(ModelBase model, float shadowSize) {
        super(model, shadowSize);
    }

    @Override
    protected void setTextures() {
        this.setTexture(0, "minecraft:textures/entity/zombie/zombie");
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.textures[0];
    }
}
