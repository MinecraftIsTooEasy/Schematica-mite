// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.item;

import com.github.lunatrius.schematica.entity.EntityMudMan1;
import com.github.lunatrius.schematica.entity.EntityMudMan2;
import com.github.lunatrius.schematica.entity.EntityMudMan3;
import com.github.lunatrius.schematica.reference.Reference;
import net.minecraft.CreativeTabs;
import net.minecraft.Entity;
import net.minecraft.Item;
import net.minecraft.World;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;
import net.xiaoyu233.fml.reload.utils.IdUtil;

public final class SchematicaItems {
    public static final Item MUD_MAN_1_SPAWN_EGG = new ItemMudManSpawnEgg(
            IdUtil.getNextItemID(),
            "mud_man_1_spawn_egg",
            new ItemMudManSpawnEgg.EntityFactory() {
                @Override
                public Entity create(World world) {
                    return new EntityMudMan1(world);
                }
            }
    );
    public static final Item MUD_MAN_2_SPAWN_EGG = new ItemMudManSpawnEgg(
            IdUtil.getNextItemID(),
            "mud_man_2_spawn_egg",
            new ItemMudManSpawnEgg.EntityFactory() {
                @Override
                public Entity create(World world) {
                    return new EntityMudMan2(world);
                }
            }
    );
    public static final Item MUD_MAN_3_SPAWN_EGG = new ItemMudManSpawnEgg(
            IdUtil.getNextItemID(),
            "mud_man_3_spawn_egg",
            new ItemMudManSpawnEgg.EntityFactory() {
                @Override
                public Entity create(World world) {
                    return new EntityMudMan3(world);
                }
            }
    );

    private SchematicaItems() {
    }

    public static void registerItems(ItemRegistryEvent event) {
        event.register(Reference.MODID, "mud_man_1_spawn_egg", MUD_MAN_1_SPAWN_EGG, CreativeTabs.tabMisc);
        event.register(Reference.MODID, "mud_man_2_spawn_egg", MUD_MAN_2_SPAWN_EGG, CreativeTabs.tabMisc);
        event.register(Reference.MODID, "mud_man_3_spawn_egg", MUD_MAN_3_SPAWN_EGG, CreativeTabs.tabMisc);
    }
}
