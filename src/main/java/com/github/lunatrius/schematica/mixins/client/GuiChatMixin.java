// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.mixins.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.GuiChat;
import net.minecraft.GuiTextField;
import net.minecraft.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class GuiChatMixin {
    @Shadow
    private boolean field_73897_d;

    @Shadow
    private int field_73903_n;

    @Shadow
    private List field_73904_o;

    @Shadow
    protected GuiTextField inputField;

    @Unique
    private boolean schematica$localCompletionActive;

    @Inject(method = "completePlayerName", at = @At("HEAD"), cancellable = true)
    private void schematica$completeLocalCommand(CallbackInfo ci) {
        if (schematica$handleLocalCompletion()) {
            ci.cancel();
            return;
        }
        this.schematica$localCompletionActive = false;
    }

    @Unique
    private boolean schematica$handleLocalCompletion() {
        if (this.inputField == null) {
            return false;
        }

        String fullText = this.inputField.getText();
        if (fullText == null) {
            return false;
        }

        int cursor = this.inputField.getCursorPosition();
        if (cursor < 0 || cursor > fullText.length()) {
            cursor = fullText.length();
        }

        String beforeCursor = fullText.substring(0, cursor);
        if (!schematica$isSchematicaCommandContext(beforeCursor)) {
            return false;
        }

        if (this.field_73897_d && !this.schematica$localCompletionActive) {
            this.field_73897_d = false;
        }

        if (this.field_73897_d) {
            this.inputField.deleteFromCursor(this.inputField.func_73798_a(-1, this.inputField.getCursorPosition(), true) - this.inputField.getCursorPosition());
            if (this.field_73904_o.isEmpty()) {
                this.field_73897_d = false;
                this.schematica$localCompletionActive = false;
                return true;
            }
            if (this.field_73903_n >= this.field_73904_o.size()) {
                this.field_73903_n = 0;
            }
        } else {
            int wordStart = this.inputField.func_73798_a(-1, this.inputField.getCursorPosition(), false);
            this.field_73904_o.clear();
            this.field_73903_n = 0;

            String currentPrefix = this.inputField.getText().substring(wordStart, this.inputField.getCursorPosition()).toLowerCase(Locale.ROOT);
            if (currentPrefix.startsWith("/")) {
                currentPrefix = currentPrefix.substring(1);
            }

            List<String> suggestions = schematica$collectSuggestions(beforeCursor, currentPrefix);
            if (suggestions.isEmpty()) {
                this.field_73897_d = false;
                this.schematica$localCompletionActive = false;
                return true;
            }

            this.field_73904_o.addAll(suggestions);
            this.field_73897_d = true;
            this.schematica$localCompletionActive = true;
            int deleteStart = wordStart;
            String text = this.inputField.getText();
            if (deleteStart == 0 && text != null && text.startsWith("/")) {
                deleteStart = 1;
            }
            this.inputField.deleteFromCursor(deleteStart - this.inputField.getCursorPosition());
        }

        if (this.field_73904_o.size() > 1) {
            StringBuilder builder = new StringBuilder();
            for (Object suggestionObj : this.field_73904_o) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(suggestionObj);
            }

            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null) {
                mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(builder.toString(), 1);
            }
        }

        this.inputField.writeText((String) this.field_73904_o.get(this.field_73903_n++));
        if (this.field_73904_o.size() == 1) {
            // Unique completion: finish this round so next TAB can enter the next completion context.
            this.field_73897_d = false;
            this.schematica$localCompletionActive = false;
        }
        return true;
    }

    @Unique
    private boolean schematica$isSchematicaCommandContext(String beforeCursor) {
        if (beforeCursor == null || beforeCursor.isEmpty() || !beforeCursor.startsWith("/")) {
            return false;
        }

        String commandText = beforeCursor.substring(1);
        if (commandText.isEmpty()) {
            return false;
        }

        String trimmed = commandText.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0) {
            return false;
        }

        String root = tokens[0].toLowerCase(Locale.ROOT);
        return "schematica".startsWith(root) || "schematica".equals(root);
    }

    @Unique
    private List<String> schematica$collectSuggestions(String beforeCursor, String currentPrefixLower) {
        List<String> suggestions = new ArrayList<String>();
        String safePrefix = currentPrefixLower == null ? "" : currentPrefixLower;
        if (!beforeCursor.startsWith("/")) {
            return suggestions;
        }
        String commandText = beforeCursor.substring(1);

        boolean endsWithSpace = commandText.endsWith(" ");
        String trimmed = commandText.trim();
        if (trimmed.isEmpty()) {
            return suggestions;
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0) {
            return suggestions;
        }

        int tokenIndex = endsWithSpace ? tokens.length : tokens.length - 1;
        if (tokenIndex < 0) {
            return suggestions;
        }

        if (tokenIndex == 0) {
            if ("schematica".startsWith(safePrefix)) {
                suggestions.add("schematica ");
            }
            return suggestions;
        }

        if (!"schematica".equalsIgnoreCase(tokens[0])) {
            return suggestions;
        }

        if (tokenIndex == 1) {
            schematica$addSuggestion(suggestions, safePrefix, "help", false);
            schematica$addSuggestion(suggestions, safePrefix, "list", false);
            schematica$addSuggestion(suggestions, safePrefix, "load", true);
            schematica$addSuggestion(suggestions, safePrefix, "unload", false);
            schematica$addSuggestion(suggestions, safePrefix, "status", false);
            schematica$addSuggestion(suggestions, safePrefix, "origin", true);
            schematica$addSuggestion(suggestions, safePrefix, "move", true);
            schematica$addSuggestion(suggestions, safePrefix, "nudge", true);
            schematica$addSuggestion(suggestions, safePrefix, "rotate", true);
            schematica$addSuggestion(suggestions, safePrefix, "mirror", true);
            schematica$addSuggestion(suggestions, safePrefix, "paste", true);
            schematica$addSuggestion(suggestions, safePrefix, "undo", false);
            schematica$addSuggestion(suggestions, safePrefix, "save", true);
            schematica$addSuggestion(suggestions, safePrefix, "create", true);
            schematica$addSuggestion(suggestions, safePrefix, "sel", true);
            schematica$addSuggestion(suggestions, safePrefix, "menu", false);
            return suggestions;
        }

        String sub = tokens[1].toLowerCase(Locale.ROOT);
        if (tokenIndex == 2) {
            if ("origin".equals(sub)) {
                schematica$addSuggestion(suggestions, safePrefix, "here", false);
            } else if ("sel".equals(sub)) {
                schematica$addSuggestion(suggestions, safePrefix, "status", false);
                schematica$addSuggestion(suggestions, safePrefix, "clear", false);
            } else if ("rotate".equals(sub)) {
                schematica$addSuggestion(suggestions, safePrefix, "90", false);
                schematica$addSuggestion(suggestions, safePrefix, "180", false);
                schematica$addSuggestion(suggestions, safePrefix, "270", false);
            } else if ("mirror".equals(sub)) {
                schematica$addSuggestion(suggestions, safePrefix, "x", false);
                schematica$addSuggestion(suggestions, safePrefix, "z", false);
            } else if ("paste".equals(sub)) {
                schematica$addSuggestion(suggestions, safePrefix, "replace", false);
                schematica$addSuggestion(suggestions, safePrefix, "solid", false);
                schematica$addSuggestion(suggestions, safePrefix, "nonair", false);
            }
        }

        return suggestions;
    }

    @Unique
    private static void schematica$addSuggestion(List<String> suggestions, String currentPrefixLower, String candidate, boolean appendSpace) {
        String lower = candidate.toLowerCase(Locale.ROOT);
        if (!lower.startsWith(currentPrefixLower)) {
            return;
        }
        suggestions.add(appendSpace ? candidate + " " : candidate);
    }
}
