// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica;

import com.github.lunatrius.schematica.client.render.RenderMudMan;
import com.github.lunatrius.schematica.entity.EntityMudMan1;
import com.github.lunatrius.schematica.entity.EntityMudMan2;
import com.github.lunatrius.schematica.entity.EntityMudMan3;
import com.github.lunatrius.schematica.item.SchematicaItems;
import com.github.lunatrius.schematica.reference.Reference;
import com.google.common.eventbus.Subscribe;
import net.minecraft.ModelBiped;
import net.xiaoyu233.fml.reload.event.EntityRegisterEvent;
import net.xiaoyu233.fml.reload.event.EntityRendererRegistryEvent;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public class SchematicaRegistrationListener {
    @Subscribe
    public void onItemRegister(ItemRegistryEvent event) {
        SchematicaItems.registerItems(event);
    }

    @Subscribe
    public void onEntityRegister(EntityRegisterEvent event) {
        event.register(EntityMudMan1.class, "MudMan1", Reference.MODID, IdUtil.getNextEntityID());
        event.register(EntityMudMan2.class, "MudMan2", Reference.MODID, IdUtil.getNextEntityID());
        event.register(EntityMudMan3.class, "MudMan3", Reference.MODID, IdUtil.getNextEntityID());
    }

    @Subscribe
    public void onEntityRendererRegistry(EntityRendererRegistryEvent event) {
        RenderMudMan renderer = new RenderMudMan(new ModelBiped(), 0.5F);
        event.register(EntityMudMan1.class, renderer);
        event.register(EntityMudMan2.class, renderer);
        event.register(EntityMudMan3.class, renderer);
    }
}
