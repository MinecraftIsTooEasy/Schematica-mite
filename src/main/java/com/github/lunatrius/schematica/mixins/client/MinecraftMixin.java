// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.mixins.client;

import com.github.lunatrius.schematica.SchematicaRuntime;
import com.github.lunatrius.schematica.client.gui.GuiSchematicaControl;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.GuiScreen;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.Minecraft;
import net.minecraft.RaycastCollision;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public GuiScreen currentScreen;

    @Shadow
    public boolean inGameHasFocus;

    @Shadow
    public EntityClientPlayerMP thePlayer;

    @Shadow
    public RaycastCollision objectMouseOver;

    @Shadow
    public abstract void displayGuiScreen(GuiScreen guiScreen);

    @Unique
    private boolean schematica$menuKeyDown;
    @Unique
    private boolean schematica$selectionClickDown;

    @Inject(method = "runTick", at = @At("RETURN"))
    private void schematica$onRunTick(CallbackInfo ci) {
        boolean pressed = Keyboard.isKeyDown(Keyboard.KEY_M);
        if (pressed
                && !this.schematica$menuKeyDown
                && this.currentScreen == null
                && this.inGameHasFocus
                && this.thePlayer != null) {
            this.displayGuiScreen(new GuiSchematicaControl());
        }
        this.schematica$menuKeyDown = pressed;

        schematica$handleSelectionStickClick();
    }

    @Unique
    private void schematica$handleSelectionStickClick() {
        boolean rightDown = Mouse.isButtonDown(1);
        if (!rightDown) {
            this.schematica$selectionClickDown = false;
            return;
        }
        if (this.schematica$selectionClickDown) {
            return;
        }
        this.schematica$selectionClickDown = true;

        if (this.currentScreen != null || !this.inGameHasFocus || this.thePlayer == null || this.objectMouseOver == null || !this.objectMouseOver.isBlock()) {
            return;
        }

        ItemStack held = this.thePlayer.getHeldItemStack();
        if (held == null || held.getItem() != Item.stick) {
            return;
        }

        int x = this.objectMouseOver.block_hit_x;
        int y = this.objectMouseOver.block_hit_y;
        int z = this.objectMouseOver.block_hit_z;

        if (this.thePlayer.isSneaking()) {
            SchematicaRuntime.setSelectionPos2(x, y, z);
            this.thePlayer.addChatMessage("Schematica Pos2 set: [" + x + "," + y + "," + z + "] (Shift+RightClick)");
        } else {
            SchematicaRuntime.setSelectionPos1(x, y, z);
            this.thePlayer.addChatMessage("Schematica Pos1 set: [" + x + "," + y + "," + z + "] (RightClick)");
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
            this.thePlayer.addChatMessage("Selection ready: [" + minX + "," + minY + "," + minZ + "] -> [" + maxX + "," + maxY + "," + maxZ + "]  "
                    + width + "x" + height + "x" + length + " (" + volume + " blocks)");
            this.thePlayer.addChatMessage("Use: /schematica create <name>");
        }
    }
}
