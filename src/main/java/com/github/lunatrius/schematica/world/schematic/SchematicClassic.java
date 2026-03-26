/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.NBTTagCompound
 */
package com.github.lunatrius.schematica.world.schematic;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import net.minecraft.NBTTagCompound;

public class SchematicClassic
extends SchematicFormat {
    @Override
    public ISchematic readFromNBT(NBTTagCompound tagCompound) {
        throw new UnsupportedOperationException("Classic format is not implemented in this port");
    }

    @Override
    public boolean writeToNBT(NBTTagCompound tagCompound, ISchematic schematic) {
        throw new UnsupportedOperationException("Classic format is not implemented in this port");
    }
}
