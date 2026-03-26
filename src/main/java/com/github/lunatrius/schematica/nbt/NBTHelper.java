// Based on Schematica by Lunatrius (https://github.com/Lunatrius/Schematica)
// Licensed under the MIT License. See LICENSE file for details.
package com.github.lunatrius.schematica.nbt;

import net.minecraft.Entity;
import net.minecraft.EntityList;
import net.minecraft.NBTTagCompound;
import net.minecraft.TileEntity;
import net.minecraft.World;

public final class NBTHelper {
    private NBTHelper() {
    }

    public static NBTTagCompound writeTileEntityToCompound(TileEntity tileEntity) {
        NBTTagCompound tileEntityCompound = new NBTTagCompound();
        tileEntity.writeToNBT(tileEntityCompound);
        return tileEntityCompound;
    }

    public static TileEntity readTileEntityFromCompound(NBTTagCompound tileEntityCompound) {
        return TileEntity.createAndLoadEntity(tileEntityCompound);
    }

    public static NBTTagCompound writeEntityToCompound(Entity entity) {
        if (entity == null) {
            return null;
        }
        NBTTagCompound entityCompound = new NBTTagCompound();
        return entity.writeToNBTOptional(entityCompound) ? entityCompound : null;
    }

    public static Entity readEntityFromCompound(NBTTagCompound entityCompound, World world) {
        return EntityList.createEntityFromNBT(entityCompound, world);
    }
}
