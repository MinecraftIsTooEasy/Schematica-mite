// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.worldgen;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import java.io.InputStream;
import java.util.Random;
import net.minecraft.Block;
import net.minecraft.CompressedStreamTools;
import net.minecraft.NBTTagCompound;
import net.minecraft.TileEntity;
import net.minecraft.World;
import net.minecraft.WorldGenerator;

public class SchematicStructureGenerator extends WorldGenerator {
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;

    private final String resourcePath;
    private ISchematic schematic;
    private boolean loadAttempted;

    public SchematicStructureGenerator(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        if (!ensureSchematicLoaded() || this.schematic == null) {
            return false;
        }

        int width = this.schematic.getWidth();
        int height = this.schematic.getHeight();
        int length = this.schematic.getLength();
        if (width <= 0 || height <= 0 || length <= 0) {
            return false;
        }

        int originX = x - width / 2;
        int originZ = z - length / 2;
        int originY = y > 0 ? y : world.getHeightValue(x, z);
        if (originY < MIN_WORLD_Y) {
            originY = MIN_WORLD_Y;
        }
        if (originY + height - 1 > MAX_WORLD_Y) {
            return false;
        }

        for (int sx = 0; sx < width; ++sx) {
            for (int sy = 0; sy < height; ++sy) {
                for (int sz = 0; sz < length; ++sz) {
                    Block block = this.schematic.getBlock(sx, sy, sz);
                    if (block == null || block.blockID == 0) {
                        continue;
                    }

                    int wx = originX + sx;
                    int wy = originY + sy;
                    int wz = originZ + sz;
                    if (wy < MIN_WORLD_Y || wy > MAX_WORLD_Y) {
                        continue;
                    }

                    int metadata = this.schematic.getBlockMetadata(sx, sy, sz);
                    world.setBlock(wx, wy, wz, block.blockID, metadata, 2);
                }
            }
        }

        for (TileEntity tileEntity : this.schematic.getTileEntities()) {
            if (tileEntity == null) {
                continue;
            }

            Block sourceBlock = this.schematic.getBlock(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            if (sourceBlock == null || sourceBlock.blockID == 0) {
                continue;
            }

            int wx = originX + tileEntity.xCoord;
            int wy = originY + tileEntity.yCoord;
            int wz = originZ + tileEntity.zCoord;
            if (wy < MIN_WORLD_Y || wy > MAX_WORLD_Y) {
                continue;
            }
            if (world.getBlockId(wx, wy, wz) != sourceBlock.blockID) {
                continue;
            }

            TileEntity copied = copyTileEntity(tileEntity, wx, wy, wz);
            if (copied != null) {
                world.setBlockTileEntity(wx, wy, wz, copied);
            }
        }

        return true;
    }

    private boolean ensureSchematicLoaded() {
        if (this.schematic != null) {
            return true;
        }
        if (this.loadAttempted) {
            return false;
        }
        this.loadAttempted = true;

        this.schematic = loadFromResource(this.resourcePath);
        if (this.schematic == null) {
            Reference.logger.warn("Failed to load structure schematic from {}", this.resourcePath);
            return false;
        }

        Reference.logger.info("Loaded worldgen schematic {} ({}x{}x{})",
                this.resourcePath,
                this.schematic.getWidth(),
                this.schematic.getHeight(),
                this.schematic.getLength());
        return true;
    }

    private static ISchematic loadFromResource(String resourcePath) {
        try (InputStream input = SchematicStructureGenerator.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                return null;
            }
            NBTTagCompound tagCompound = CompressedStreamTools.readCompressed(input);
            String format = tagCompound.getString("Materials");
            SchematicFormat schematicFormat = SchematicFormat.FORMATS.get(format);
            if (schematicFormat == null) {
                Reference.logger.warn("Unsupported schematic format {} for {}", format, resourcePath);
                return null;
            }
            return schematicFormat.readFromNBT(tagCompound);
        } catch (Exception e) {
            Reference.logger.error("Failed to load schematic resource {}", resourcePath, e);
            return null;
        }
    }

    private static TileEntity copyTileEntity(TileEntity tileEntity, int x, int y, int z) {
        try {
            NBTTagCompound tag = NBTHelper.writeTileEntityToCompound(tileEntity);
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);
            return NBTHelper.readTileEntityFromCompound(tag);
        } catch (Exception ignored) {
            return null;
        }
    }
}
