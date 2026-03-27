// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica;

import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import com.github.lunatrius.schematica.world.storage.Schematic;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.AxisAlignedBB;
import net.minecraft.Block;
import net.minecraft.Entity;
import net.minecraft.EntityList;
import net.minecraft.EntityLivingBase;
import net.minecraft.EntityPlayer;
import net.minecraft.IInventory;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagDouble;
import net.minecraft.NBTTagList;
import net.minecraft.TileEntity;
import net.minecraft.World;
import net.xiaoyu233.fml.reload.event.HandleChatCommandEvent;

public class SchematicaEventListener {
    private static final long MAX_SAVE_VOLUME = 8_000_000L;
    private static final long MAX_PASTE_VOLUME = 8_000_000L;
    private static final int MIN_WORLD_Y = 0;
    private static final int MAX_WORLD_Y = 255;

    @Subscribe
    public void onCommand(HandleChatCommandEvent event) {
        String command = event.getCommand().trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            return;
        }
        command = normalizeCommandAlias(command);

        String lower = command.toLowerCase(Locale.ROOT);
        boolean clientWorld = isClientWorld(event);

        if ("schematica help".equals(lower)) {
            event.setExecuteSuccess(true);
            sendHelp(event);
            return;
        }

        if ("schematica sel status".equals(lower)) {
            event.setExecuteSuccess(true);
            if (SchematicaRuntime.hasSelectionPos1) {
                event.getPlayer().addChatMessage("Selection Pos1: [" + SchematicaRuntime.selectionPos1X + "," + SchematicaRuntime.selectionPos1Y + "," + SchematicaRuntime.selectionPos1Z + "]");
            } else {
                event.getPlayer().addChatMessage("Selection Pos1: <unset>");
            }
            if (SchematicaRuntime.hasSelectionPos2) {
                event.getPlayer().addChatMessage("Selection Pos2: [" + SchematicaRuntime.selectionPos2X + "," + SchematicaRuntime.selectionPos2Y + "," + SchematicaRuntime.selectionPos2Z + "]");
            } else {
                event.getPlayer().addChatMessage("Selection Pos2: <unset>");
            }
            if (SchematicaRuntime.hasSelection()) {
                int minX = Math.min(SchematicaRuntime.selectionPos1X, SchematicaRuntime.selectionPos2X);
                int minY = Math.min(SchematicaRuntime.selectionPos1Y, SchematicaRuntime.selectionPos2Y);
                int minZ = Math.min(SchematicaRuntime.selectionPos1Z, SchematicaRuntime.selectionPos2Z);
                int maxX = Math.max(SchematicaRuntime.selectionPos1X, SchematicaRuntime.selectionPos2X);
                int maxY = Math.max(SchematicaRuntime.selectionPos1Y, SchematicaRuntime.selectionPos2Y);
                int maxZ = Math.max(SchematicaRuntime.selectionPos1Z, SchematicaRuntime.selectionPos2Z);
                int width = maxX - minX + 1;
                int height = maxY - minY + 1;
                int length = maxZ - minZ + 1;
                long volume = (long) width * height * length;
                event.getPlayer().addChatMessage("Selection size: " + width + "x" + height + "x" + length + " (" + volume + " blocks)");
            }
            return;
        }

        if ("schematica sel clear".equals(lower)) {
            event.setExecuteSuccess(true);
            SchematicaRuntime.clearSelection();
            event.getPlayer().addChatMessage("Selection cleared.");
            return;
        }

        if ("schematica list".equals(lower)) {
            event.setExecuteSuccess(true);
            File[] files = getSchematicDir().listFiles(new FileFilterSchematic(false));
            int count = files == null ? 0 : files.length;
            event.getPlayer().addChatMessage("Schematica files: " + count);
            return;
        }

        if ("schematica load".equals(lower) || lower.startsWith("schematica load ")) {
            event.setExecuteSuccess(!clientWorld);
            String rawName = command.length() > "schematica load".length()
                    ? command.substring("schematica load".length()).trim()
                    : "";
            if (rawName.isEmpty()) {
                event.getPlayer().addChatMessage("Usage: /schematica load <name>");
                return;
            }

            File file = resolveSchematicFile(rawName);
            if (file == null || !file.exists()) {
                event.getPlayer().addChatMessage("Schematic not found: " + normalizeFilename(rawName));
                return;
            }

            ISchematic schematic = SchematicFormat.readFromFile(file);
            if (schematic == null) {
                event.getPlayer().addChatMessage("Failed to load schematic: " + file.getName());
                return;
            }

            int originX = event.getPlayer().getBlockPosX();
            int originY = event.getPlayer().getBlockPosY();
            int originZ = event.getPlayer().getBlockPosZ();
            SchematicaRuntime.setLoadedSchematic(schematic, originX, originY, originZ, file.getName());
            event.getPlayer().addChatMessage("Loaded " + file.getName() + " (" + schematic.getWidth() + "x" + schematic.getHeight() + "x" + schematic.getLength() + ") at [" + originX + "," + originY + "," + originZ + "]");
            return;
        }

        if ("schematica unload".equals(lower)) {
            event.setExecuteSuccess(!clientWorld);
            SchematicaRuntime.clearLoadedSchematic();
            event.getPlayer().addChatMessage("Schematica projection unloaded.");
            return;
        }

        if ("schematica status".equals(lower)) {
            event.setExecuteSuccess(!clientWorld);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded. Use /schematica load <name>.");
                return;
            }

            ISchematic schematic = SchematicaRuntime.loadedSchematic;
            String name = SchematicaRuntime.loadedSchematicName == null ? "<unknown>" : SchematicaRuntime.loadedSchematicName;
            long volume = (long) schematic.getWidth() * schematic.getHeight() * schematic.getLength();
            event.getPlayer().addChatMessage(
                    "Loaded " + name
                            + " (" + schematic.getWidth() + "x" + schematic.getHeight() + "x" + schematic.getLength() + ", blocks=" + volume + ")"
                            + " origin [" + SchematicaRuntime.originX + "," + SchematicaRuntime.originY + "," + SchematicaRuntime.originZ + "]"
                            + ", undo=" + (SchematicaRuntime.hasUndoSnapshot() ? "ready" : "empty"));
            return;
        }

        if ("schematica origin here".equals(lower)) {
            event.setExecuteSuccess(!clientWorld);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded.");
                return;
            }

            int originX = event.getPlayer().getBlockPosX();
            int originY = event.getPlayer().getBlockPosY();
            int originZ = event.getPlayer().getBlockPosZ();
            SchematicaRuntime.setOrigin(originX, originY, originZ);
            event.getPlayer().addChatMessage("Projection origin set to player position: [" + originX + "," + originY + "," + originZ + "]");
            return;
        }

        if ("schematica move".equals(lower) || lower.startsWith("schematica move ")) {
            event.setExecuteSuccess(!clientWorld);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded.");
                return;
            }

            String[] parts = command.split("\\s+");
            if (parts.length != 4) {
                event.getPlayer().addChatMessage("Usage: /schematica move <x> <y> <z>");
                return;
            }

            int originX;
            int originY;
            int originZ;
            try {
                originX = Integer.parseInt(parts[1]);
                originY = Integer.parseInt(parts[2]);
                originZ = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                event.getPlayer().addChatMessage("Coordinates must be integers.");
                return;
            }

            SchematicaRuntime.setOrigin(originX, originY, originZ);
            event.getPlayer().addChatMessage("Projection moved to [" + originX + "," + originY + "," + originZ + "]");
            return;
        }

        if ("schematica nudge".equals(lower) || lower.startsWith("schematica nudge ")) {
            event.setExecuteSuccess(!clientWorld);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded.");
                return;
            }

            String[] parts = command.split("\\s+");
            if (parts.length != 4) {
                event.getPlayer().addChatMessage("Usage: /schematica nudge <dx> <dy> <dz>");
                return;
            }

            int dx;
            int dy;
            int dz;
            try {
                dx = Integer.parseInt(parts[1]);
                dy = Integer.parseInt(parts[2]);
                dz = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                event.getPlayer().addChatMessage("Offsets must be integers.");
                return;
            }

            int originX = SchematicaRuntime.originX + dx;
            int originY = SchematicaRuntime.originY + dy;
            int originZ = SchematicaRuntime.originZ + dz;
            SchematicaRuntime.setOrigin(originX, originY, originZ);
            event.getPlayer().addChatMessage("Projection nudged by [" + dx + "," + dy + "," + dz + "] to [" + originX + "," + originY + "," + originZ + "]");
            return;
        }

        if ("schematica rotate".equals(lower) || lower.startsWith("schematica rotate ")) {
            event.setExecuteSuccess(!clientWorld);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded.");
                return;
            }

            String[] parts = command.split("\\s+");
            if (parts.length != 2) {
                event.getPlayer().addChatMessage("Usage: /schematica rotate <90|180|270>");
                return;
            }

            int angle;
            try {
                angle = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                event.getPlayer().addChatMessage("Angle must be one of: 90, 180, 270.");
                return;
            }
            if (!(angle == 90 || angle == 180 || angle == 270)) {
                event.getPlayer().addChatMessage("Angle must be one of: 90, 180, 270.");
                return;
            }

            ISchematic rotated = rotateSchematic(SchematicaRuntime.loadedSchematic, angle);
            if (rotated == null) {
                event.getPlayer().addChatMessage("Failed to rotate schematic.");
                return;
            }

            SchematicaRuntime.setLoadedSchematic(
                    rotated,
                    SchematicaRuntime.originX,
                    SchematicaRuntime.originY,
                    SchematicaRuntime.originZ,
                    SchematicaRuntime.loadedSchematicName);
            event.getPlayer().addChatMessage("Rotated projection " + angle + " degrees. New size: " + rotated.getWidth() + "x" + rotated.getHeight() + "x" + rotated.getLength());
            return;
        }

        if ("schematica mirror".equals(lower) || lower.startsWith("schematica mirror ")) {
            event.setExecuteSuccess(!clientWorld);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded.");
                return;
            }

            String[] parts = command.split("\\s+");
            if (parts.length != 2) {
                event.getPlayer().addChatMessage("Usage: /schematica mirror <x|z>");
                return;
            }

            String axis = parts[1].toLowerCase(Locale.ROOT);
            if (!"x".equals(axis) && !"z".equals(axis)) {
                event.getPlayer().addChatMessage("Mirror axis must be x or z.");
                return;
            }

            ISchematic mirrored = mirrorSchematic(SchematicaRuntime.loadedSchematic, axis);
            if (mirrored == null) {
                event.getPlayer().addChatMessage("Failed to mirror schematic.");
                return;
            }

            SchematicaRuntime.setLoadedSchematic(
                    mirrored,
                    SchematicaRuntime.originX,
                    SchematicaRuntime.originY,
                    SchematicaRuntime.originZ,
                    SchematicaRuntime.loadedSchematicName);
            event.getPlayer().addChatMessage("Mirrored projection on " + axis + " axis.");
            return;
        }

        if ("schematica paste".equals(lower) || lower.startsWith("schematica paste ")) {
            if (clientWorld) {
                event.setExecuteSuccess(false);
                return;
            }
            event.setExecuteSuccess(true);
            if (!SchematicaRuntime.hasLoadedSchematic()) {
                event.getPlayer().addChatMessage("No schematic loaded.");
                return;
            }
            if (event.getWorld() == null) {
                event.getPlayer().addChatMessage("No world available.");
                return;
            }

            PasteMode mode = parsePasteMode(command);
            if (mode == null) {
                event.getPlayer().addChatMessage("Usage: /schematica paste [replace|solid|nonair]");
                return;
            }

            ISchematic schematic = SchematicaRuntime.loadedSchematic;
            long volume = (long) schematic.getWidth() * schematic.getHeight() * schematic.getLength();
            if (volume <= 0L || volume > MAX_PASTE_VOLUME) {
                event.getPlayer().addChatMessage("Schematic too large to paste. Max blocks: " + MAX_PASTE_VOLUME);
                return;
            }
            String boundsError = validateRegionBounds(
                    SchematicaRuntime.originX,
                    SchematicaRuntime.originY,
                    SchematicaRuntime.originZ,
                    schematic.getWidth(),
                    schematic.getHeight(),
                    schematic.getLength());
            if (boundsError != null) {
                event.getPlayer().addChatMessage("Cannot paste: " + boundsError);
                return;
            }

            Schematic undo = captureWorldRegion(
                    event.getWorld(),
                    SchematicaRuntime.originX,
                    SchematicaRuntime.originY,
                    SchematicaRuntime.originZ,
                    schematic.getWidth(),
                    schematic.getHeight(),
                    schematic.getLength(),
                    copyIcon(schematic),
                    true);
            if (undo == null) {
                event.getPlayer().addChatMessage("Failed to capture undo snapshot.");
                return;
            }

            SchematicaRuntime.setUndoSnapshot(
                    undo,
                    SchematicaRuntime.originX,
                    SchematicaRuntime.originY,
                    SchematicaRuntime.originZ,
                    SchematicaRuntime.loadedSchematicName);

            PasteResult result = pasteSchematic(
                    event.getWorld(),
                    schematic,
                    SchematicaRuntime.originX,
                    SchematicaRuntime.originY,
                    SchematicaRuntime.originZ,
                    mode,
                    hasEntityState(schematic));

            String name = SchematicaRuntime.loadedSchematicName == null ? "<unknown>" : SchematicaRuntime.loadedSchematicName;
            event.getPlayer().addChatMessage("Pasted " + name + " mode=" + mode.id + ": placed=" + result.placed + ", cleared=" + result.cleared + ", tileEntities=" + result.tileEntities + ", entities=" + result.entitiesSpawned + ", entityCleared=" + result.entitiesCleared + ", unchanged=" + result.unchanged + (result.failed > 0 ? ", failed=" + result.failed : "") + (result.tileEntityFailed > 0 ? ", tileEntityFailed=" + result.tileEntityFailed : "") + (result.entityFailed > 0 ? ", entityFailed=" + result.entityFailed : ""));
            return;
        }

        if ("schematica undo".equals(lower)) {
            if (clientWorld) {
                event.setExecuteSuccess(false);
                return;
            }
            event.setExecuteSuccess(true);
            if (!SchematicaRuntime.hasUndoSnapshot()) {
                event.getPlayer().addChatMessage("No undo snapshot.");
                return;
            }
            if (event.getWorld() == null) {
                event.getPlayer().addChatMessage("No world available.");
                return;
            }

            ISchematic undoSchematic = SchematicaRuntime.lastUndoSchematic;
            String undoBoundsError = validateRegionBounds(
                    SchematicaRuntime.lastUndoOriginX,
                    SchematicaRuntime.lastUndoOriginY,
                    SchematicaRuntime.lastUndoOriginZ,
                    undoSchematic.getWidth(),
                    undoSchematic.getHeight(),
                    undoSchematic.getLength());
            if (undoBoundsError != null) {
                event.getPlayer().addChatMessage("Cannot undo: " + undoBoundsError);
                return;
            }

            PasteResult result = pasteSchematic(
                    event.getWorld(),
                    undoSchematic,
                    SchematicaRuntime.lastUndoOriginX,
                    SchematicaRuntime.lastUndoOriginY,
                    SchematicaRuntime.lastUndoOriginZ,
                    PasteMode.REPLACE,
                    true);
            String label = SchematicaRuntime.lastUndoLabel == null ? "<unknown>" : SchematicaRuntime.lastUndoLabel;
            SchematicaRuntime.clearUndoSnapshot();
            event.getPlayer().addChatMessage("Undo restored " + label + ": placed=" + result.placed + ", cleared=" + result.cleared + ", tileEntities=" + result.tileEntities + ", entities=" + result.entitiesSpawned + ", entityCleared=" + result.entitiesCleared + ", unchanged=" + result.unchanged + (result.failed > 0 ? ", failed=" + result.failed : "") + (result.tileEntityFailed > 0 ? ", tileEntityFailed=" + result.tileEntityFailed : "") + (result.entityFailed > 0 ? ", entityFailed=" + result.entityFailed : ""));
            return;
        }

        if ("schematica save".equals(lower) || lower.startsWith("schematica save ")) {
            if (clientWorld) {
                event.setExecuteSuccess(false);
                return;
            }
            event.setExecuteSuccess(true);
            String[] parts = command.split("\\s+");
            if (parts.length < 8) {
                event.getPlayer().addChatMessage("Usage: /schematica save <x1> <y1> <z1> <x2> <y2> <z2> <name>");
                return;
            }

            int x1;
            int y1;
            int z1;
            int x2;
            int y2;
            int z2;
            try {
                x1 = Integer.parseInt(parts[1]);
                y1 = Integer.parseInt(parts[2]);
                z1 = Integer.parseInt(parts[3]);
                x2 = Integer.parseInt(parts[4]);
                y2 = Integer.parseInt(parts[5]);
                z2 = Integer.parseInt(parts[6]);
            } catch (NumberFormatException e) {
                event.getPlayer().addChatMessage("Coordinates must be integers.");
                return;
            }

            String rawName = String.join(" ", Arrays.copyOfRange(parts, 7, parts.length)).trim();
            File outFile = resolveSchematicFile(rawName);
            if (outFile == null) {
                event.getPlayer().addChatMessage("Invalid schematic name.");
                return;
            }

            int minX = Math.min(x1, x2);
            int minY = Math.min(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxX = Math.max(x1, x2);
            int maxY = Math.max(y1, y2);
            int maxZ = Math.max(z1, z2);

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int length = maxZ - minZ + 1;
            long volume = (long) width * height * length;
            if (volume <= 0L || volume > MAX_SAVE_VOLUME) {
                event.getPlayer().addChatMessage("Selection too large. Max blocks: " + MAX_SAVE_VOLUME);
                return;
            }
            if (event.getWorld() == null) {
                event.getPlayer().addChatMessage("No world available.");
                return;
            }

            ItemStack icon = event.getPlayer().getHeldItemStack();
            if (icon == null) {
                icon = new ItemStack((Block) Block.stone);
            } else {
                icon = icon.copy();
            }

            Schematic schematic = captureWorldRegion(event.getWorld(), minX, minY, minZ, width, height, length, icon, true);
            if (schematic == null) {
                event.getPlayer().addChatMessage("Failed to capture world region.");
                return;
            }

            boolean saved = SchematicFormat.writeToFile(outFile, schematic);
            if (!saved) {
                event.getPlayer().addChatMessage("Failed to save schematic: " + outFile.getName());
                return;
            }

            int entityCount = getEntityStateCount(schematic);
            event.getPlayer().addChatMessage("Saved " + outFile.getName() + " (" + width + "x" + height + "x" + length + ", entities=" + entityCount + ")");
            return;
        }

        if ("schematica create".equals(lower) || lower.startsWith("schematica create ")) {
            if (clientWorld) {
                event.setExecuteSuccess(false);
                return;
            }
            event.setExecuteSuccess(true);
            String rawName = command.length() > "schematica create".length()
                    ? command.substring("schematica create".length()).trim()
                    : "";
            if (rawName.isEmpty()) {
                event.getPlayer().addChatMessage("Usage: /schematica create <name>");
                return;
            }
            if (!SchematicaRuntime.hasSelection()) {
                event.getPlayer().addChatMessage("Selection incomplete. Use stick: RightClick=Pos1, Shift+RightClick=Pos2.");
                return;
            }
            if (event.getWorld() == null) {
                event.getPlayer().addChatMessage("No world available.");
                return;
            }

            int minX = Math.min(SchematicaRuntime.selectionPos1X, SchematicaRuntime.selectionPos2X);
            int minY = Math.min(SchematicaRuntime.selectionPos1Y, SchematicaRuntime.selectionPos2Y);
            int minZ = Math.min(SchematicaRuntime.selectionPos1Z, SchematicaRuntime.selectionPos2Z);
            int maxX = Math.max(SchematicaRuntime.selectionPos1X, SchematicaRuntime.selectionPos2X);
            int maxY = Math.max(SchematicaRuntime.selectionPos1Y, SchematicaRuntime.selectionPos2Y);
            int maxZ = Math.max(SchematicaRuntime.selectionPos1Z, SchematicaRuntime.selectionPos2Z);

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int length = maxZ - minZ + 1;
            long volume = (long) width * height * length;
            if (volume <= 0L || volume > MAX_SAVE_VOLUME) {
                event.getPlayer().addChatMessage("Selection too large. Max blocks: " + MAX_SAVE_VOLUME);
                return;
            }

            File outFile = resolveSchematicFile(rawName);
            if (outFile == null) {
                event.getPlayer().addChatMessage("Invalid schematic name.");
                return;
            }

            ItemStack icon = event.getPlayer().getHeldItemStack();
            if (icon == null) {
                icon = new ItemStack((Block) Block.stone);
            } else {
                icon = icon.copy();
            }

            Schematic schematic = captureWorldRegion(event.getWorld(), minX, minY, minZ, width, height, length, icon, true);
            if (schematic == null) {
                event.getPlayer().addChatMessage("Failed to capture world region.");
                return;
            }

            if (!SchematicFormat.writeToFile(outFile, schematic)) {
                event.getPlayer().addChatMessage("Failed to save schematic: " + outFile.getName());
                return;
            }

            SchematicaRuntime.setLoadedSchematic(schematic, minX, minY, minZ, outFile.getName());
            int entityCount = getEntityStateCount(schematic);
            event.getPlayer().addChatMessage("Created and loaded projection " + outFile.getName() + " (" + width + "x" + height + "x" + length + ", entities=" + entityCount + ") at [" + minX + "," + minY + "," + minZ + "]");
            return;
        }
    }

    private boolean isClientWorld(HandleChatCommandEvent event) {
        World world = event.getWorld();
        return world != null && world.isRemote;
    }

    private void sendHelp(HandleChatCommandEvent event) {
        event.getPlayer().addChatMessage("Schematica commands:");
        event.getPlayer().addChatMessage("/schematica help | /schematica list");
        event.getPlayer().addChatMessage("/schematica load <name> | /schematica unload | /schematica status");
        event.getPlayer().addChatMessage("/schematica origin here | /schematica move <x> <y> <z> | /schematica nudge <dx> <dy> <dz>");
        event.getPlayer().addChatMessage("/schematica rotate <90|180|270> | /schematica mirror <x|z>");
        event.getPlayer().addChatMessage("/schematica paste [replace|solid|nonair] | /schematica undo");
        event.getPlayer().addChatMessage("/schematica save <x1> <y1> <z1> <x2> <y2> <z2> <name>");
        event.getPlayer().addChatMessage("/schematica create <name> (from stick selection)");
        event.getPlayer().addChatMessage("/schematica sel status | /schematica sel clear");
    }

    private String normalizeCommandAlias(String command) {
        if (command == null) {
            return "";
        }

        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) {
            return trimmed;
        }

        if (!"schematica".equalsIgnoreCase(parts[0])) {
            return trimmed;
        }
        if (parts.length == 1) {
            return "schematica help";
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if ("help".equals(sub)) {
            return "schematica help";
        }
        if ("list".equals(sub)) {
            return "schematica list";
        }
        if ("load".equals(sub)) {
            return composeAliasedCommand("schematica load", parts, 2);
        }
        if ("unload".equals(sub)) {
            return "schematica unload";
        }
        if ("status".equals(sub)) {
            return "schematica status";
        }
        if ("origin".equals(sub) && parts.length >= 3 && "here".equalsIgnoreCase(parts[2])) {
            return "schematica origin here";
        }
        if ("move".equals(sub)) {
            return composeAliasedCommand("schematica move", parts, 2);
        }
        if ("nudge".equals(sub)) {
            return composeAliasedCommand("schematica nudge", parts, 2);
        }
        if ("rotate".equals(sub)) {
            return composeAliasedCommand("schematica rotate", parts, 2);
        }
        if ("mirror".equals(sub)) {
            return composeAliasedCommand("schematica mirror", parts, 2);
        }
        if ("paste".equals(sub)) {
            return composeAliasedCommand("schematica paste", parts, 2);
        }
        if ("undo".equals(sub)) {
            return "schematica undo";
        }
        if ("save".equals(sub)) {
            return composeAliasedCommand("schematica save", parts, 2);
        }
        if ("create".equals(sub)) {
            return composeAliasedCommand("schematica create", parts, 2);
        }
        if ("sel".equals(sub) && parts.length >= 3) {
            if ("status".equalsIgnoreCase(parts[2])) {
                return "schematica sel status";
            }
            if ("clear".equalsIgnoreCase(parts[2])) {
                return "schematica sel clear";
            }
        }
        if ("menu".equals(sub)) {
            return "schematica menu";
        }
        return trimmed;
    }

    private String composeAliasedCommand(String prefix, String[] parts, int argStartIndex) {
        if (parts.length <= argStartIndex) {
            return prefix;
        }
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = argStartIndex; i < parts.length; ++i) {
            builder.append(' ').append(parts[i]);
        }
        return builder.toString();
    }

    private String validateRegionBounds(int originX, int originY, int originZ, int width, int height, int length) {
        if (width <= 0 || height <= 0 || length <= 0) {
            return "invalid region size.";
        }

        long minX = originX;
        long minY = originY;
        long minZ = originZ;
        long maxX = originX + (long) width - 1L;
        long maxY = originY + (long) height - 1L;
        long maxZ = originZ + (long) length - 1L;

        if (minY < MIN_WORLD_Y || maxY > MAX_WORLD_Y) {
            return "Y range " + minY + ".." + maxY + " is outside world bounds " + MIN_WORLD_Y + ".." + MAX_WORLD_Y + ".";
        }
        if (maxX > Integer.MAX_VALUE || maxZ > Integer.MAX_VALUE || minX < Integer.MIN_VALUE || minZ < Integer.MIN_VALUE) {
            return "X/Z range overflow: [" + minX + "," + minZ + "]..[" + maxX + "," + maxZ + "].";
        }
        return null;
    }

    private PasteMode parsePasteMode(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length <= 2) {
            return PasteMode.REPLACE;
        }
        if (parts.length != 3) {
            return null;
        }

        String mode = parts[2].toLowerCase(Locale.ROOT);
        if ("replace".equals(mode)) {
            return PasteMode.REPLACE;
        }
        if ("solid".equals(mode) || "nonair".equals(mode)) {
            return PasteMode.SOLID;
        }
        return null;
    }

    private PasteResult pasteSchematic(World world, ISchematic schematic, int originX, int originY, int originZ, PasteMode mode, boolean applyEntityState) {
        PasteResult result = new PasteResult();
        for (int x = 0; x < schematic.getWidth(); ++x) {
            for (int y = 0; y < schematic.getHeight(); ++y) {
                for (int z = 0; z < schematic.getLength(); ++z) {
                    int wx = originX + x;
                    int wy = originY + y;
                    int wz = originZ + z;

                    Block block = schematic.getBlock(x, y, z);
                    if (block == null || block.blockID == 0) {
                        if (mode != PasteMode.REPLACE) {
                            continue;
                        }

                        boolean success = world.setBlockToAir(wx, wy, wz, 2);
                        if (!success && world.getBlockId(wx, wy, wz) == 0) {
                            ++result.unchanged;
                            continue;
                        }
                        if (success) {
                            ++result.cleared;
                        } else {
                            ++result.failed;
                        }
                        continue;
                    }

                    int metadata = schematic.getBlockMetadata(x, y, z);
                    boolean success = world.setBlock(wx, wy, wz, block.blockID, metadata, 2);
                    if (!success) {
                        int existingId = world.getBlockId(wx, wy, wz);
                        int existingMeta = world.getBlockMetadata(wx, wy, wz);
                        if (existingId == block.blockID && existingMeta == (metadata & 0xF)) {
                            ++result.unchanged;
                            continue;
                        }
                    }

                    if (success) {
                        ++result.placed;
                    } else {
                        ++result.failed;
                    }
                }
            }
        }

        for (TileEntity tileEntity : schematic.getTileEntities()) {
            if (tileEntity == null) {
                continue;
            }
            Block sourceBlock = schematic.getBlock(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            if (sourceBlock == null || sourceBlock.blockID == 0) {
                continue;
            }

            int wx = originX + tileEntity.xCoord;
            int wy = originY + tileEntity.yCoord;
            int wz = originZ + tileEntity.zCoord;
            if (world.getBlockId(wx, wy, wz) != sourceBlock.blockID) {
                continue;
            }

            if (applyTileEntityData(world, tileEntity, wx, wy, wz)) {
                ++result.tileEntities;
            } else {
                ++result.tileEntityFailed;
            }
        }

        if (applyEntityState) {
            replaceEntitiesInRegion(world, schematic, originX, originY, originZ, result);
        }

        return result;
    }

    private Schematic captureWorldRegion(World world, int originX, int originY, int originZ, int width, int height, int length, ItemStack icon, boolean includeEntities) {
        if (world == null || width <= 0 || height <= 0 || length <= 0) {
            return null;
        }

        ItemStack safeIcon = icon == null ? new ItemStack((Block) Block.stone) : icon.copy();
        Schematic schematic = new Schematic(safeIcon, width, height, length);
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                for (int z = 0; z < length; ++z) {
                    int wx = originX + x;
                    int wy = originY + y;
                    int wz = originZ + z;

                    Block block = world.getBlock(wx, wy, wz);
                    int metadata = world.getBlockMetadata(wx, wy, wz);
                    schematic.setBlock(x, y, z, block, metadata);

                    TileEntity tileEntity = world.getBlockTileEntity(wx, wy, wz);
                    if (tileEntity == null) {
                        continue;
                    }

                    TileEntity copied = copyTileEntity(tileEntity, x, y, z);
                    if (copied != null) {
                        schematic.setTileEntity(x, y, z, copied);
                    }
                }
            }
        }

        if (includeEntities) {
            captureEntitiesInRegion(world, schematic, originX, originY, originZ, width, height, length);
        }

        return schematic;
    }

    private void captureEntitiesInRegion(World world, Schematic schematic, int originX, int originY, int originZ, int width, int height, int length) {
        AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(
                originX,
                originY,
                originZ,
                originX + (double) width,
                originY + (double) height,
                originZ + (double) length);
        List entities = getEntitiesInBounds(world, bounds);
        int scanned = 0;
        int eligible = 0;
        int captured = 0;
        int failed = 0;
        for (Object object : entities) {
            ++scanned;
            if (!(object instanceof Entity)) {
                continue;
            }

            Entity entity = (Entity) object;
            if (entity instanceof EntityPlayer || !(entity instanceof EntityLivingBase)) {
                continue;
            }
            ++eligible;

            NBTTagCompound relativeTag = copyEntityTagRelative(entity, originX, originY, originZ);
            if (relativeTag != null) {
                schematic.addEntityTag(relativeTag);
                ++captured;
            } else {
                ++failed;
                if (failed <= 3) {
                    Reference.logger.warn("Schematica entity capture failed for class={}, id={}", entity.getClass().getName(), EntityList.getEntityString(entity));
                }
            }
        }
        Reference.logger.info("Schematica entity capture: scanned={}, eligible={}, captured={}, failed={}, region=[{},{},{}] size={}x{}x{}",
                scanned, eligible, captured, failed, originX, originY, originZ, width, height, length);
    }

    private NBTTagCompound copyEntityTagRelative(Entity entity, int originX, int originY, int originZ) {
        if (entity == null || entity.isDead) {
            return null;
        }

        try {
            NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
            if (tag == null) {
                return null;
            }
            offsetEntityPosition(tag, -originX, -originY, -originZ);
            return tag;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Entity copyEntityAtWorldPosition(Entity entity, World world, int originX, int originY, int originZ) {
        if (entity == null) {
            return null;
        }

        try {
            NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
            if (tag == null) {
                return null;
            }

            offsetEntityPosition(tag, originX, originY, originZ);
            return NBTHelper.readEntityFromCompound(tag, world);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void offsetEntityPosition(NBTTagCompound tag, int offsetX, int offsetY, int offsetZ) {
        NBTTagList pos = tag.getTagList("Pos");
        if (pos != null && pos.tagCount() >= 3
                && pos.tagAt(0) instanceof NBTTagDouble
                && pos.tagAt(1) instanceof NBTTagDouble
                && pos.tagAt(2) instanceof NBTTagDouble) {
            NBTTagList translated = new NBTTagList();
            translated.appendTag(new NBTTagDouble(null, ((NBTTagDouble) pos.tagAt(0)).data + offsetX));
            translated.appendTag(new NBTTagDouble(null, ((NBTTagDouble) pos.tagAt(1)).data + offsetY));
            translated.appendTag(new NBTTagDouble(null, ((NBTTagDouble) pos.tagAt(2)).data + offsetZ));
            tag.setTag("Pos", translated);
        }

        if (tag.hasKey("Riding")) {
            NBTTagCompound riding = tag.getCompoundTag("Riding");
            offsetEntityPosition(riding, offsetX, offsetY, offsetZ);
            tag.setTag("Riding", riding);
        }
    }

    private void replaceEntitiesInRegion(World world, ISchematic schematic, int originX, int originY, int originZ, PasteResult result) {
        AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(
                originX,
                originY,
                originZ,
                originX + (double) schematic.getWidth(),
                originY + (double) schematic.getHeight(),
                originZ + (double) schematic.getLength());

        List existing = getEntitiesInBounds(world, bounds);
        for (Object object : existing) {
            if (!(object instanceof Entity)) {
                continue;
            }

            Entity entity = (Entity) object;
            if (entity instanceof EntityPlayer || !(entity instanceof EntityLivingBase)) {
                continue;
            }
            world.removeEntity(entity);
            ++result.entitiesCleared;
        }

        if (schematic instanceof Schematic) {
            List<NBTTagCompound> entityTags = ((Schematic)schematic).getEntityTags();
            if (!entityTags.isEmpty()) {
                for (NBTTagCompound sourceTag : entityTags) {
                    if (sourceTag == null) {
                        ++result.entityFailed;
                        continue;
                    }

                    NBTTagCompound worldTag = (NBTTagCompound)sourceTag.copy();
                    offsetEntityPosition(worldTag, originX, originY, originZ);
                    Entity copied = NBTHelper.readEntityFromCompound(worldTag, world);
                    if (copied == null) {
                        ++result.entityFailed;
                        continue;
                    }

                    if (world.spawnEntityInWorld(copied)) {
                        ++result.entitiesSpawned;
                    } else {
                        ++result.entityFailed;
                    }
                }
                return;
            }
        }

        for (Entity entity : schematic.getEntities()) {
            Entity copied = copyEntityAtWorldPosition(entity, world, originX, originY, originZ);
            if (copied == null) {
                ++result.entityFailed;
                continue;
            }

            if (world.spawnEntityInWorld(copied)) {
                ++result.entitiesSpawned;
            } else {
                ++result.entityFailed;
            }
        }
    }

    private List getEntitiesInBounds(World world, AxisAlignedBB bounds) {
        List queried = world.getEntitiesWithinAABB(Entity.class, bounds);
        if (queried != null && !queried.isEmpty()) {
            return queried;
        }

        List loaded = world.getLoadedEntityList();
        ArrayList<Entity> fallback = new ArrayList<Entity>();
        if (loaded == null || loaded.isEmpty()) {
            return fallback;
        }

        for (Object object : loaded) {
            if (!(object instanceof Entity)) {
                continue;
            }

            Entity entity = (Entity) object;
            if (entity.boundingBox != null) {
                if (entity.boundingBox.intersectsWith(bounds)) {
                    fallback.add(entity);
                }
                continue;
            }

            if (entity.posX >= bounds.minX && entity.posX < bounds.maxX
                    && entity.posY >= bounds.minY && entity.posY < bounds.maxY
                    && entity.posZ >= bounds.minZ && entity.posZ < bounds.maxZ) {
                fallback.add(entity);
            }
        }
        return fallback;
    }

    private Entity copyEntityRotated(Entity entity, int sourceWidth, int sourceLength, int angle) {
        if (entity == null) {
            return null;
        }

        try {
            World world = entity.worldObj;
            if (world == null) {
                return null;
            }

            NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
            if (tag == null) {
                return null;
            }

            rotateEntityPosition(tag, sourceWidth, sourceLength, angle);
            return NBTHelper.readEntityFromCompound(tag, world);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Entity copyEntityMirrored(Entity entity, int sourceWidth, int sourceLength, String axis) {
        if (entity == null) {
            return null;
        }

        try {
            World world = entity.worldObj;
            if (world == null) {
                return null;
            }

            NBTTagCompound tag = NBTHelper.writeEntityToCompound(entity);
            if (tag == null) {
                return null;
            }

            mirrorEntityPosition(tag, sourceWidth, sourceLength, axis);
            return NBTHelper.readEntityFromCompound(tag, world);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void rotateEntityPosition(NBTTagCompound tag, int sourceWidth, int sourceLength, int angle) {
        NBTTagList pos = tag.getTagList("Pos");
        if (pos != null && pos.tagCount() >= 3
                && pos.tagAt(0) instanceof NBTTagDouble
                && pos.tagAt(1) instanceof NBTTagDouble
                && pos.tagAt(2) instanceof NBTTagDouble) {
            double x = ((NBTTagDouble) pos.tagAt(0)).data;
            double y = ((NBTTagDouble) pos.tagAt(1)).data;
            double z = ((NBTTagDouble) pos.tagAt(2)).data;
            double nx;
            double nz;
            if (angle == 90) {
                nx = sourceLength - z;
                nz = x;
            } else if (angle == 180) {
                nx = sourceWidth - x;
                nz = sourceLength - z;
            } else {
                nx = z;
                nz = sourceWidth - x;
            }

            NBTTagList transformed = new NBTTagList();
            transformed.appendTag(new NBTTagDouble(null, nx));
            transformed.appendTag(new NBTTagDouble(null, y));
            transformed.appendTag(new NBTTagDouble(null, nz));
            tag.setTag("Pos", transformed);
        }

        if (tag.hasKey("Riding")) {
            NBTTagCompound riding = tag.getCompoundTag("Riding");
            rotateEntityPosition(riding, sourceWidth, sourceLength, angle);
            tag.setTag("Riding", riding);
        }
    }

    private void mirrorEntityPosition(NBTTagCompound tag, int sourceWidth, int sourceLength, String axis) {
        NBTTagList pos = tag.getTagList("Pos");
        if (pos != null && pos.tagCount() >= 3
                && pos.tagAt(0) instanceof NBTTagDouble
                && pos.tagAt(1) instanceof NBTTagDouble
                && pos.tagAt(2) instanceof NBTTagDouble) {
            double x = ((NBTTagDouble) pos.tagAt(0)).data;
            double y = ((NBTTagDouble) pos.tagAt(1)).data;
            double z = ((NBTTagDouble) pos.tagAt(2)).data;
            double nx = "x".equals(axis) ? sourceWidth - x : x;
            double nz = "z".equals(axis) ? sourceLength - z : z;

            NBTTagList transformed = new NBTTagList();
            transformed.appendTag(new NBTTagDouble(null, nx));
            transformed.appendTag(new NBTTagDouble(null, y));
            transformed.appendTag(new NBTTagDouble(null, nz));
            tag.setTag("Pos", transformed);
        }

        if (tag.hasKey("Riding")) {
            NBTTagCompound riding = tag.getCompoundTag("Riding");
            mirrorEntityPosition(riding, sourceWidth, sourceLength, axis);
            tag.setTag("Riding", riding);
        }
    }

    private ISchematic rotateSchematic(ISchematic source, int angle) {
        int newWidth = angle == 180 ? source.getWidth() : source.getLength();
        int newLength = angle == 180 ? source.getLength() : source.getWidth();
        int height = source.getHeight();

        Schematic rotated = new Schematic(copyIcon(source), newWidth, height, newLength);
        for (int x = 0; x < source.getWidth(); ++x) {
            for (int y = 0; y < source.getHeight(); ++y) {
                for (int z = 0; z < source.getLength(); ++z) {
                    int nx;
                    int nz;
                    if (angle == 90) {
                        nx = source.getLength() - 1 - z;
                        nz = x;
                    } else if (angle == 180) {
                        nx = source.getWidth() - 1 - x;
                        nz = source.getLength() - 1 - z;
                    } else {
                        nx = z;
                        nz = source.getWidth() - 1 - x;
                    }

                    rotated.setBlock(nx, y, nz, source.getBlock(x, y, z), source.getBlockMetadata(x, y, z));
                }
            }
        }

        for (TileEntity tileEntity : source.getTileEntities()) {
            if (tileEntity == null) {
                continue;
            }
            int nx;
            int nz;
            if (angle == 90) {
                nx = source.getLength() - 1 - tileEntity.zCoord;
                nz = tileEntity.xCoord;
            } else if (angle == 180) {
                nx = source.getWidth() - 1 - tileEntity.xCoord;
                nz = source.getLength() - 1 - tileEntity.zCoord;
            } else {
                nx = tileEntity.zCoord;
                nz = source.getWidth() - 1 - tileEntity.xCoord;
            }

            TileEntity copied = copyTileEntity(tileEntity, nx, tileEntity.yCoord, nz);
            if (copied != null) {
                rotated.setTileEntity(nx, tileEntity.yCoord, nz, copied);
            }
        }

        if (source instanceof Schematic) {
            for (NBTTagCompound sourceTag : ((Schematic)source).getEntityTags()) {
                if (sourceTag == null) {
                    continue;
                }
                NBTTagCompound transformed = (NBTTagCompound)sourceTag.copy();
                rotateEntityPosition(transformed, source.getWidth(), source.getLength(), angle);
                rotated.addEntityTag(transformed);
            }
        }

        for (Entity entity : source.getEntities()) {
            Entity copied = copyEntityRotated(entity, source.getWidth(), source.getLength(), angle);
            if (copied != null) {
                rotated.addEntity(copied);
            }
        }

        return rotated;
    }

    private ISchematic mirrorSchematic(ISchematic source, String axis) {
        Schematic mirrored = new Schematic(copyIcon(source), source.getWidth(), source.getHeight(), source.getLength());
        for (int x = 0; x < source.getWidth(); ++x) {
            for (int y = 0; y < source.getHeight(); ++y) {
                for (int z = 0; z < source.getLength(); ++z) {
                    int nx = "x".equals(axis) ? source.getWidth() - 1 - x : x;
                    int nz = "z".equals(axis) ? source.getLength() - 1 - z : z;
                    mirrored.setBlock(nx, y, nz, source.getBlock(x, y, z), source.getBlockMetadata(x, y, z));
                }
            }
        }

        for (TileEntity tileEntity : source.getTileEntities()) {
            if (tileEntity == null) {
                continue;
            }
            int nx = "x".equals(axis) ? source.getWidth() - 1 - tileEntity.xCoord : tileEntity.xCoord;
            int nz = "z".equals(axis) ? source.getLength() - 1 - tileEntity.zCoord : tileEntity.zCoord;
            TileEntity copied = copyTileEntity(tileEntity, nx, tileEntity.yCoord, nz);
            if (copied != null) {
                mirrored.setTileEntity(nx, tileEntity.yCoord, nz, copied);
            }
        }

        if (source instanceof Schematic) {
            for (NBTTagCompound sourceTag : ((Schematic)source).getEntityTags()) {
                if (sourceTag == null) {
                    continue;
                }
                NBTTagCompound transformed = (NBTTagCompound)sourceTag.copy();
                mirrorEntityPosition(transformed, source.getWidth(), source.getLength(), axis);
                mirrored.addEntityTag(transformed);
            }
        }

        for (Entity entity : source.getEntities()) {
            Entity copied = copyEntityMirrored(entity, source.getWidth(), source.getLength(), axis);
            if (copied != null) {
                mirrored.addEntity(copied);
            }
        }

        return mirrored;
    }

    private TileEntity copyTileEntity(TileEntity tileEntity, int x, int y, int z) {
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

    private boolean applyTileEntityData(World world, TileEntity source, int x, int y, int z) {
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

    private ItemStack copyIcon(ISchematic schematic) {
        ItemStack icon = schematic.getIcon();
        if (icon == null) {
            return new ItemStack((Block) Block.stone);
        }
        return icon.copy();
    }

    private boolean hasEntityState(ISchematic schematic) {
        return getEntityStateCount(schematic) > 0;
    }

    private int getEntityStateCount(ISchematic schematic) {
        if (schematic instanceof Schematic) {
            int tagCount = ((Schematic)schematic).getEntityTags().size();
            if (tagCount > 0) {
                return tagCount;
            }
        }
        return schematic.getEntities().size();
    }

    private File getSchematicDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "schematics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File resolveSchematicFile(String rawName) {
        String filename = normalizeFilename(rawName);
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        File dir = getSchematicDir();
        File file = new File(dir, filename);
        try {
            String dirPath = dir.getCanonicalPath() + File.separator;
            String filePath = file.getCanonicalPath();
            if (!filePath.startsWith(dirPath)) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return file;
    }

    private String normalizeFilename(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String base = trimmed.toLowerCase(Locale.ROOT).endsWith(".schematic")
                ? trimmed.substring(0, trimmed.length() - ".schematic".length())
                : trimmed;
        base = base.replaceAll("[\\\\/:*?\"<>|]+", "_").replaceAll("\\s+", "_");
        if (base.isEmpty()) {
            return null;
        }
        return base + ".schematic";
    }

    private enum PasteMode {
        REPLACE("replace"),
        SOLID("solid");

        private final String id;

        PasteMode(String id) {
            this.id = id;
        }
    }

    private static final class PasteResult {
        private int placed;
        private int cleared;
        private int tileEntities;
        private int tileEntityFailed;
        private int entitiesSpawned;
        private int entitiesCleared;
        private int unchanged;
        private int failed;
        private int entityFailed;
    }
}

