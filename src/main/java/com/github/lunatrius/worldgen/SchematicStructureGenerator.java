// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.worldgen;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import com.github.lunatrius.schematica.world.storage.Schematic;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import net.minecraft.Block;
import net.minecraft.CompressedStreamTools;
import net.minecraft.Entity;
import net.minecraft.EntityPlayer;
import net.minecraft.EntitySkeleton;
import net.minecraft.IInventory;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagDouble;
import net.minecraft.NBTTagList;
import net.minecraft.TileEntity;
import net.minecraft.WeightedRandomChestContent;
import net.minecraft.World;
import net.minecraft.WorldGenerator;

public class SchematicStructureGenerator extends WorldGenerator {
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;
    private static final boolean DEBUG_ENTITY_REPLACEMENT = Boolean.parseBoolean(
            System.getProperty("schematica.debug.entityReplacement", "false")
    );

    private final String resourcePath;
    private ISchematic schematic;
    private boolean loadAttempted;

    public SchematicStructureGenerator(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public boolean generate(World world, Random random, int x, int y, int z) {
        if (world == null || world.isRemote) {
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

        int lootChestGenerated = 0;
        int lootChestFailed = 0;
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

            int lootLevel = detectLootChestLevel(this.resourcePath, sourceBlock, tileEntity);
            if (!applyTileEntityData(world, tileEntity, wx, wy, wz)) {
                if (lootLevel > 0) {
                    ++lootChestFailed;
                }
                continue;
            }

            if (lootLevel > 0) {
                if (populateLootChest(this.resourcePath, world, random, wx, wy, wz, lootLevel)) {
                    ++lootChestGenerated;
                } else {
                    ++lootChestFailed;
                }
            }
        }

        if (lootChestFailed > 0) {
            Reference.logger.warn("Worldgen schematic {} loot chest result: generated={}, failed={}, origin=[{},{},{}]",
                    this.resourcePath,
                    lootChestGenerated,
                    lootChestFailed,
                    originX,
                    originY,
                    originZ);
        } else if (lootChestGenerated > 0) {
            Reference.logger.info("Worldgen schematic {} generated {} marker loot chests at [{},{},{}]",
                    this.resourcePath,
                    lootChestGenerated,
                    originX,
                    originY,
                    originZ);
        }

        EntitySpawnResult entitySpawnResult = spawnEntities(world, originX, originY, originZ);
        if (entitySpawnResult.failed > 0) {
            Reference.logger.warn("Worldgen schematic {} entity spawn result: spawned={}, replaced={}, skipped={}, failed={}, origin=[{},{},{}]",
                    this.resourcePath,
                    entitySpawnResult.spawned,
                    entitySpawnResult.replaced,
                    entitySpawnResult.skipped,
                    entitySpawnResult.failed,
                    originX,
                    originY,
                    originZ);
        } else if (entitySpawnResult.spawned > 0 || entitySpawnResult.replaced > 0 || entitySpawnResult.skipped > 0) {
            Reference.logger.info("Worldgen schematic {} entity result: spawned={}, replaced={}, skipped={} at [{},{},{}]",
                    this.resourcePath,
                    entitySpawnResult.spawned,
                    entitySpawnResult.replaced,
                    entitySpawnResult.skipped,
                    originX,
                    originY,
                    originZ);
        }

        return true;
    }

    private EntitySpawnResult spawnEntities(World world, int originX, int originY, int originZ) {
        EntitySpawnResult result = new EntitySpawnResult();
        if (world == null || this.schematic == null) {
            return result;
        }

        if (this.schematic instanceof Schematic) {
            List<NBTTagCompound> entityTags = ((Schematic)this.schematic).getEntityTags();
            if (!entityTags.isEmpty()) {
                for (NBTTagCompound sourceTag : entityTags) {
                    if (sourceTag == null) {
                        ++result.failed;
                        continue;
                    }

                    NBTTagCompound worldTag = (NBTTagCompound)sourceTag.copy();
                    EntityReplacementResult replacement = applyEntityReplacement(worldTag, this.resourcePath);
                    if (replacement.skip) {
                        ++result.skipped;
                        continue;
                    }
                    if (!replacement.valid) {
                        ++result.failed;
                        continue;
                    }
                    if (replacement.replaced) {
                        ++result.replaced;
                    }

                    offsetEntityPosition(worldTag, originX, originY, originZ);
                    Entity copied = NBTHelper.readEntityFromCompound(worldTag, world);
                    if (copied == null || copied instanceof EntityPlayer) {
                        ++result.failed;
                        continue;
                    }
                    if (copied.posY < MIN_WORLD_Y || copied.posY > MAX_WORLD_Y) {
                        ++result.failed;
                        continue;
                    }

                    postProcessReplacedEntity(copied, replacement);
                    if (world.spawnEntityInWorld(copied)) {
                        ++result.spawned;
                    } else {
                        ++result.failed;
                    }
                }
                return result;
            }
        }

        for (Entity sourceEntity : this.schematic.getEntities()) {
            NBTTagCompound sourceTag = NBTHelper.writeEntityToCompound(sourceEntity);
            if (sourceTag == null) {
                ++result.failed;
                continue;
            }

            EntityReplacementResult replacement = applyEntityReplacement(sourceTag, this.resourcePath);
            if (replacement.skip) {
                ++result.skipped;
                continue;
            }
            if (!replacement.valid) {
                ++result.failed;
                continue;
            }
            if (replacement.replaced) {
                ++result.replaced;
            }

            offsetEntityPosition(sourceTag, originX, originY, originZ);
            Entity copied = NBTHelper.readEntityFromCompound(sourceTag, world);
            if (copied == null || copied instanceof EntityPlayer) {
                ++result.failed;
                continue;
            }
            if (copied.posY < MIN_WORLD_Y || copied.posY > MAX_WORLD_Y) {
                ++result.failed;
                continue;
            }

            postProcessReplacedEntity(copied, replacement);
            if (world.spawnEntityInWorld(copied)) {
                ++result.spawned;
            } else {
                ++result.failed;
            }
        }
        return result;
    }

    private static EntityReplacementResult applyEntityReplacement(NBTTagCompound tag, String structureKey) {
        if (tag == null) {
            return EntityReplacementResult.invalid();
        }

        WorldgenEntityReplacementRules.ReplacementDecision decision = WorldgenEntityReplacementRules.resolve(structureKey, tag);
        String sourceId = decision.getSourceId();
        String replacementId = decision.getReplacementId();
        if (replacementId == null) {
            return EntityReplacementResult.skip();
        }

        String normalizedReplacementId = replacementId.trim();
        if (normalizedReplacementId.isEmpty()) {
            return EntityReplacementResult.invalid();
        }

        boolean replaced = sourceId == null || !normalizedReplacementId.equals(sourceId);
        if (replaced) {
            tag.setString("id", normalizedReplacementId);
        }

        if (DEBUG_ENTITY_REPLACEMENT && decision.getDetectedLevel() > 0) {
            Reference.logger.info(
                    "Entity replacement debug: structure={}, rule={}, level={}, sourceId={}, targetId={}, replaced={}, forceSkeletonIronSword={}",
                    structureKey,
                    decision.getMatchedRuleId(),
                    decision.getDetectedLevel(),
                    sourceId,
                    normalizedReplacementId,
                    replaced,
                    decision.isForceSkeletonIronSword()
            );
        }

        return EntityReplacementResult.valid(replaced, normalizedReplacementId, decision.isForceSkeletonIronSword());
    }

    private static void postProcessReplacedEntity(Entity entity, EntityReplacementResult replacement) {
        if (entity == null || replacement == null || replacement.replacementId == null) {
            return;
        }

        if (!replacement.forceSkeletonIronSword) {
            return;
        }

        if (entity instanceof EntitySkeleton) {
            ((EntitySkeleton)entity).setCurrentItemOrArmor(0, new ItemStack(Item.swordIron));
        }
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

    private static boolean applyTileEntityData(World world, TileEntity source, int x, int y, int z) {
        if (world == null || source == null) {
            return false;
        }

        try {
            NBTTagCompound tag = NBTHelper.writeTileEntityToCompound(source);
            if (tag == null) {
                return false;
            }
            tag.setInteger("x", x);
            tag.setInteger("y", y);
            tag.setInteger("z", z);

            TileEntity existing = world.getBlockTileEntity(x, y, z);
            if (existing != null) {
                existing.readFromNBT(tag);
                existing.updateContainingBlockInfo();
                if (existing instanceof IInventory) {
                    ((IInventory)existing).onInventoryChanged();
                }
                return true;
            }

            TileEntity recreated = NBTHelper.readTileEntityFromCompound(tag);
            if (recreated == null) {
                return false;
            }
            world.setBlockTileEntity(x, y, z, recreated);
            TileEntity placed = world.getBlockTileEntity(x, y, z);
            if (placed instanceof IInventory) {
                ((IInventory)placed).onInventoryChanged();
            }
            return placed != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int detectLootChestLevel(String structureKey, Block sourceBlock, TileEntity sourceTileEntity) {
        if (!isLootChestBlock(sourceBlock) || !(sourceTileEntity instanceof IInventory)) {
            return 0;
        }

        IInventory inventory = (IInventory)sourceTileEntity;
        ItemStack markerStack = null;
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize <= 0) {
                continue;
            }
            if (markerStack != null) {
                return 0;
            }
            markerStack = stack;
        }

        return WeightedTreasurePieces.getLevelForMarker(structureKey, markerStack);
    }

    private static boolean isLootChestBlock(Block block) {
        if (block == null) {
            return false;
        }

        int blockId = block.blockID;
        return blockId == Block.chest.blockID
                || blockId == Block.chestTrapped.blockID
                || blockId == Block.chestCopper.blockID
                || blockId == Block.chestSilver.blockID
                || blockId == Block.chestGold.blockID
                || blockId == Block.chestIron.blockID
                || blockId == Block.chestMithril.blockID
                || blockId == Block.chestAdamantium.blockID
                || blockId == Block.chestAncientMetal.blockID;
    }

    private static boolean populateLootChest(String structureKey, World world, Random random, int x, int y, int z, int level) {
        if (world == null || level <= 0) {
            return false;
        }

        TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        if (!(tileEntity instanceof IInventory)) {
            return false;
        }

        IInventory inventory = (IInventory)tileEntity;
        clearInventory(inventory);

        WeightedRandomChestContent[] contents = WeightedTreasurePieces.getContentsForLevel(structureKey, level);
        int rollCount = WeightedTreasurePieces.getRollCount(structureKey, random, level);
        if (contents.length > 0 && rollCount > 0 && random != null) {
            WeightedRandomChestContent.generateChestContents(
                    world,
                    y,
                    random,
                    contents,
                    inventory,
                    rollCount,
                    WeightedTreasurePieces.getArtifactChances(structureKey, level)
            );
        }
        inventory.onInventoryChanged();
        return true;
    }

    private static void clearInventory(IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            inventory.setInventorySlotContents(slot, null);
        }
    }

    private static void offsetEntityPosition(NBTTagCompound tag, int offsetX, int offsetY, int offsetZ) {
        NBTTagList pos = tag.getTagList("Pos");
        if (pos != null && pos.tagCount() >= 3
                && pos.tagAt(0) instanceof NBTTagDouble
                && pos.tagAt(1) instanceof NBTTagDouble
                && pos.tagAt(2) instanceof NBTTagDouble) {
            NBTTagList translated = new NBTTagList();
            translated.appendTag(new NBTTagDouble(null, ((NBTTagDouble)pos.tagAt(0)).data + offsetX));
            translated.appendTag(new NBTTagDouble(null, ((NBTTagDouble)pos.tagAt(1)).data + offsetY));
            translated.appendTag(new NBTTagDouble(null, ((NBTTagDouble)pos.tagAt(2)).data + offsetZ));
            tag.setTag("Pos", translated);
        }

        if (tag.hasKey("Riding")) {
            NBTTagCompound riding = tag.getCompoundTag("Riding");
            offsetEntityPosition(riding, offsetX, offsetY, offsetZ);
            tag.setTag("Riding", riding);
        }
    }

    private static final class EntitySpawnResult {
        private int spawned;
        private int failed;
        private int replaced;
        private int skipped;
    }

    private static final class EntityReplacementResult {
        private final boolean valid;
        private final boolean skip;
        private final boolean replaced;
        private final String replacementId;
        private final boolean forceSkeletonIronSword;

        private EntityReplacementResult(boolean valid, boolean skip, boolean replaced, String replacementId, boolean forceSkeletonIronSword) {
            this.valid = valid;
            this.skip = skip;
            this.replaced = replaced;
            this.replacementId = replacementId;
            this.forceSkeletonIronSword = forceSkeletonIronSword;
        }

        private static EntityReplacementResult valid(boolean replaced, String replacementId, boolean forceSkeletonIronSword) {
            return new EntityReplacementResult(true, false, replaced, replacementId, forceSkeletonIronSword);
        }

        private static EntityReplacementResult invalid() {
            return new EntityReplacementResult(false, false, false, null, false);
        }

        private static EntityReplacementResult skip() {
            return new EntityReplacementResult(false, true, false, null, false);
        }
    }
}
