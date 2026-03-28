// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.item;

import com.github.lunatrius.schematica.reference.Reference;
import net.minecraft.CreativeTabs;
import net.minecraft.Entity;
import net.minecraft.EntityPlayer;
import net.minecraft.Item;
import net.minecraft.Material;
import net.minecraft.RaycastCollision;
import net.minecraft.World;

public class ItemMudManSpawnEgg extends Item {
    private final EntityFactory entityFactory;

    public ItemMudManSpawnEgg(int id, String unlocalizedName, EntityFactory entityFactory) {
        super(id, Material.cloth, "spawn_egg");
        this.entityFactory = entityFactory;
        this.setMaxStackSize(64);
        this.setCreativeTab(CreativeTabs.tabMisc);
        this.setUnlocalizedName(unlocalizedName);
    }

    @Override
    public boolean onItemRightClick(EntityPlayer player, float partialTick, boolean ctrlIsDown) {
        if (player == null) {
            return true;
        }

        if (!player.onServer()) {
            return true;
        }

        RaycastCollision collision = player.getSelectedObject(5.0F, true);
        if (collision == null || !collision.isBlock()) {
            return true;
        }

        World world = player.worldObj;
        if (world == null) {
            return true;
        }

        Entity entity = createEntity(world);
        if (entity == null) {
            return true;
        }

        int x = collision.block_hit_x;
        int y = collision.block_hit_y;
        int z = collision.block_hit_z;
        switch (collision.face_hit) {
            case BOTTOM:
                --y;
                break;
            case TOP:
                ++y;
                break;
            case NORTH:
                --z;
                break;
            case SOUTH:
                ++z;
                break;
            case WEST:
                --x;
                break;
            case EAST:
                ++x;
                break;
            default:
                break;
        }

        entity.setPosition(x + 0.5D, y, z + 0.5D);
        if (world.spawnEntityInWorld(entity)) {
            player.convertOneOfHeldItem(null);
        }
        return true;
    }

    private Entity createEntity(World world) {
        try {
            return this.entityFactory == null ? null : this.entityFactory.create(world);
        } catch (Exception e) {
            Reference.logger.error("Failed to create spawn egg entity instance for {}", this.getUnlocalizedName(), e);
            return null;
        }
    }

    public interface EntityFactory {
        Entity create(World world);
    }
}
