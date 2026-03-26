// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.mixins.client;

import com.github.lunatrius.schematica.client.gui.GuiSchematicaControl;
import java.util.Locale;
import net.minecraft.EntityClientPlayerMP;
import net.minecraft.Minecraft;
import net.xiaoyu233.fml.reload.event.HandleChatCommandEvent;
import net.xiaoyu233.fml.reload.event.MITEEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityClientPlayerMP.class)
public abstract class EntityClientPlayerMPMixin {
    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void schematica$handleClientCommand(String command, boolean permissionOverride, CallbackInfo ci) {
        if (schematica$handleLocalSchematicaCommand(command)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean schematica$handleLocalSchematicaCommand(String command) {
        if (command == null) {
            return false;
        }

        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }

        if (trimmed.isEmpty()) {
            return false;
        }
        trimmed = schematica$normalizeCommandAlias(trimmed);

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!(lower.equals("schematica_list")
                || lower.equals("schematica_load")
                || lower.startsWith("schematica_load ")
                || lower.equals("schematica_save")
                || lower.startsWith("schematica_save ")
                || lower.equals("schematica_create")
                || lower.startsWith("schematica_create ")
                || lower.equals("schematica_sel_status")
                || lower.equals("schematica_sel_clear")
                || lower.equals("schematica_unload")
                || lower.equals("schematica_move")
                || lower.startsWith("schematica_move ")
                || lower.equals("schematica_nudge")
                || lower.startsWith("schematica_nudge ")
                || lower.equals("schematica_rotate")
                || lower.startsWith("schematica_rotate ")
                || lower.equals("schematica_mirror")
                || lower.startsWith("schematica_mirror ")
                || lower.equals("schematica_origin_here")
                || lower.equals("schematica_status")
                || lower.equals("schematica_help")
                || lower.equals("schematica_paste")
                || lower.startsWith("schematica_paste ")
                || lower.equals("schematica_menu")
                || lower.equals("schematica_undo"))) {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        if ("schematica_menu".equals(lower)) {
            mc.displayGuiScreen(new GuiSchematicaControl());
            return true;
        }

        HandleChatCommandEvent event = new HandleChatCommandEvent(mc.thePlayer, trimmed, mc.thePlayer, mc.theWorld);
        MITEEvents.MITE_EVENT_BUS.post(event);
        return event.isExecuteSuccess();
    }

    @Unique
    private static String schematica$normalizeCommandAlias(String command) {
        String trimmed = command == null ? "" : command.trim();
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
            return "schematica_help";
        }

        String sub = parts[1].toLowerCase(Locale.ROOT);
        if ("help".equals(sub)) {
            return "schematica_help";
        }
        if ("list".equals(sub)) {
            return "schematica_list";
        }
        if ("load".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_load", parts, 2);
        }
        if ("unload".equals(sub)) {
            return "schematica_unload";
        }
        if ("status".equals(sub)) {
            return "schematica_status";
        }
        if ("origin".equals(sub) && parts.length >= 3 && "here".equalsIgnoreCase(parts[2])) {
            return "schematica_origin_here";
        }
        if ("move".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_move", parts, 2);
        }
        if ("nudge".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_nudge", parts, 2);
        }
        if ("rotate".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_rotate", parts, 2);
        }
        if ("mirror".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_mirror", parts, 2);
        }
        if ("paste".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_paste", parts, 2);
        }
        if ("undo".equals(sub)) {
            return "schematica_undo";
        }
        if ("save".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_save", parts, 2);
        }
        if ("create".equals(sub)) {
            return schematica$composeAliasedCommand("schematica_create", parts, 2);
        }
        if ("sel".equals(sub) && parts.length >= 3) {
            if ("status".equalsIgnoreCase(parts[2])) {
                return "schematica_sel_status";
            }
            if ("clear".equalsIgnoreCase(parts[2])) {
                return "schematica_sel_clear";
            }
        }
        if ("menu".equals(sub)) {
            return "schematica_menu";
        }
        return trimmed;
    }

    @Unique
    private static String schematica$composeAliasedCommand(String prefix, String[] parts, int argStartIndex) {
        if (parts.length <= argStartIndex) {
            return prefix;
        }
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = argStartIndex; i < parts.length; ++i) {
            builder.append(' ').append(parts[i]);
        }
        return builder.toString();
    }
}
