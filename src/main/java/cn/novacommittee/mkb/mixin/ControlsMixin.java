package cn.novacommittee.mkb.mixin;

import cn.novacommittee.mkb.InputKeyCodes;
import cn.novacommittee.mkb.api.IKeyBinding;
import cn.novacommittee.mkb.api.KeyModifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiControls.class)
public abstract class ControlsMixin extends GuiScreen {
    @Shadow private GameSettings options;
    @Shadow private GuiScreen parentScreen;

    @Unique private static final int mkb$ROW_HEIGHT = 22;
    @Unique private static final int mkb$TOP = 43;
    @Unique private static final int mkb$RESET_WIDTH = 48;
    @Unique private static final int mkb$BIND_WIDTH = 90;

    @Unique private final List<Integer> mkb$rows = new ArrayList<Integer>();
    @Unique private final Map<Integer, String> mkb$headers = new LinkedHashMap<Integer, String>();
    @Unique private final boolean[] mkb$keysDown = new boolean[Keyboard.KEYBOARD_SIZE];
    @Unique private int mkb$firstRow;
    @Unique private boolean mkb$draggingScrollbar;
    @Unique private int mkb$focus = -1;
    @Unique private int mkb$editing = -1;
    @Unique private int mkb$pendingKey;
    @Unique private boolean mkb$pendingCharacterDown;
    @Unique private int mkb$pendingModifierKey;
    @Unique private KeyModifier mkb$pendingModifier = KeyModifier.NONE;
    @Unique private GuiButton mkb$resetAll;
    @Unique private GuiButton mkb$done;
    @Unique private GuiTextField mkb$search;

    @Inject(method = "initGui", at = @At("HEAD"), cancellable = true)
    private void mkb$init(CallbackInfo ci) {
        ci.cancel();
        String query = mkb$search == null ? "" : mkb$search.getText();
        boolean searchFocused = mkb$search != null && mkb$search.isFocused();
        int right = mkb$listRight();
        mkb$search = new GuiTextField(fontRenderer, width - right, 19, right * 2 - width, 18);
        mkb$search.setMaxStringLength(128);
        mkb$search.setText(query);
        mkb$search.setFocused(searchFocused);
        mkb$refreshRows();
        mkb$draggingScrollbar = false;
        if (mkb$focus >= mkb$rows.size() * 2 + 2) {
            mkb$focus = -1;
        }
        mkb$clearCapture();
        int footerWidth = Math.min(148, (width - 12) / 2);
        mkb$resetAll = new GuiButton(201, width / 2 - footerWidth - 2, height - 26, footerWidth, 20,
                I18n.getString("modernizedkb.controls.resetAll"));
        mkb$done = new GuiButton(200, width / 2 + 2, height - 26, footerWidth, 20, I18n.getString("gui.done"));
    }

    @Unique
    private void mkb$refreshRows() {
        mkb$rows.clear();
        mkb$headers.clear();
        String query = mkb$search.getText().trim().toLowerCase(Locale.ROOT);
        Map<String, List<Integer>> groups = new LinkedHashMap<String, List<Integer>>();
        for (int i = 0; i < options.keyBindings.length; i++) {
            String category = ((IKeyBinding) options.keyBindings[i]).getKeyCategory();
            if (!query.isEmpty() && !options.getKeyBindingDescription(i).toLowerCase(Locale.ROOT).contains(query)
                    && !options.getOptionDisplayString(i).toLowerCase(Locale.ROOT).contains(query)
                    && !I18n.getString(category).toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            List<Integer> bindings = groups.get(category);
            if (bindings == null) {
                bindings = new ArrayList<Integer>();
                groups.put(category, bindings);
            }
            bindings.add(i);
        }
        for (Map.Entry<String, List<Integer>> group : groups.entrySet()) {
            mkb$headers.put(mkb$rows.size(), I18n.getString(group.getKey()));
            mkb$rows.add(-1);
            mkb$rows.addAll(group.getValue());
        }
        mkb$firstRow = Math.min(mkb$firstRow, Math.max(0, mkb$rows.size() - mkb$visibleRows()));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        mkb$search.updateCursorCounter();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && mkb$editing < 0) {
            mkb$firstRow = Math.max(0, Math.min(mkb$rows.size() - mkb$visibleRows(),
                    mkb$firstRow + (wheel > 0 ? -1 : 1)));
            mkb$focus = -1;
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mkb$click(int x, int y, int button, CallbackInfo ci) {
        ci.cancel();
        if (mkb$editing >= 0) {
            int code = button - 100;
            mkb$saveBinding(code, KeyModifier.capture(code, mkb$keysDown));
            return;
        }
        mkb$search.mouseClicked(x, y, button);
        if (mkb$search.isFocused()) {
            mkb$focus = -1;
            return;
        }
        if (button != 0) {
            return;
        }
        int right = mkb$listRight();
        if (x >= right + 2 && x < right + 6 && y >= mkb$TOP
                && y < mkb$TOP + mkb$visibleRows() * mkb$ROW_HEIGHT && mkb$rows.size() > mkb$visibleRows()) {
            mkb$draggingScrollbar = true;
            mkb$scrollTo(y);
            return;
        }
        int row = mkb$rowAt(y);
        if (row >= 0 && mkb$rows.get(row) >= 0 && y < mkb$rowY(row) + 20) {
            if (x >= right - mkb$RESET_WIDTH && x < right) {
                mkb$activate(row * 2 + 1);
            } else if (x >= right - mkb$RESET_WIDTH - 4 - mkb$BIND_WIDTH && x < right - mkb$RESET_WIDTH - 4) {
                mkb$activate(row * 2);
            }
        } else if (mkb$resetAll.mousePressed(mc, x, y)) {
            mkb$activate(mkb$rows.size() * 2);
        } else if (mkb$done.mousePressed(mc, x, y)) {
            mkb$activate(mkb$rows.size() * 2 + 1);
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (button == 0 && mkb$draggingScrollbar) {
            mkb$scrollTo(y);
        }
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        super.mouseMovedOrUp(x, y, button);
        if (button == 0) {
            mkb$draggingScrollbar = false;
        }
    }

    @Unique
    private void mkb$scrollTo(int y) {
        int visible = mkb$visibleRows();
        int track = visible * mkb$ROW_HEIGHT;
        int thumb = mkb$thumbHeight(visible);
        int travel = track - thumb;
        int position = Math.max(0, Math.min(travel, y - mkb$TOP - thumb / 2));
        if (travel > 0) {
            mkb$firstRow = Math.round((float) position * (mkb$rows.size() - visible) / travel);
        }
        mkb$focus = -1;
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void mkb$navigate(char character, int code, CallbackInfo ci) {
        ci.cancel();
        if (mkb$search.isFocused() && code != Keyboard.KEY_TAB
                && code != Keyboard.KEY_ESCAPE && code != Keyboard.KEY_UP && code != Keyboard.KEY_DOWN) {
            String previous = mkb$search.getText();
            mkb$search.textboxKeyTyped(character, code);
            if (!previous.equals(mkb$search.getText())) {
                mkb$firstRow = 0;
                mkb$refreshRows();
            }
            return;
        }
        switch (code) {
            case Keyboard.KEY_TAB:
                mkb$moveFocus(isShiftKeyDown() ? -1 : 1, false);
                break;
            case Keyboard.KEY_UP:
                mkb$moveFocus(-1, true);
                break;
            case Keyboard.KEY_DOWN:
                mkb$moveFocus(1, true);
                break;
            case Keyboard.KEY_LEFT:
            case Keyboard.KEY_RIGHT:
                if (mkb$focus >= 0 && mkb$canFocus(mkb$focus ^ 1)) {
                    mkb$focus ^= 1;
                }
                break;
            case Keyboard.KEY_SPACE:
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (!Keyboard.isRepeatEvent() && mkb$focus >= 0) {
                    mkb$activate(mkb$focus);
                }
                break;
            case Keyboard.KEY_ESCAPE:
                if (mkb$search.isFocused()) {
                    mkb$search.setFocused(false);
                } else {
                    mc.displayGuiScreen(parentScreen);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void handleKeyboardInput() {
        if (mkb$editing < 0) {
            super.handleKeyboardInput();
            return;
        }
        int code = Keyboard.getEventKey();
        if (Keyboard.isRepeatEvent()) {
            return;
        }
        boolean down = Keyboard.getEventKeyState();
        if (code == Keyboard.KEY_NONE && InputKeyCodes.usesLegacyCharacterInput()) {
            if (down) {
                code = InputKeyCodes.fromEvent(code, Keyboard.getEventCharacter());
                if (code == Keyboard.KEY_NONE) {
                    return;
                }
            } else if (InputKeyCodes.isCharacter(mkb$pendingKey)) {
                code = mkb$pendingKey;
            }
        }
        if (code > Keyboard.KEY_NONE && code < mkb$keysDown.length) {
            mkb$keysDown[code] = down;
        } else if (InputKeyCodes.isCharacter(code)) {
            mkb$pendingCharacterDown = down;
        } else {
            return;
        }
        if (down) {
            if (mkb$isModifier(code) && mkb$pendingModifierKey == 0) {
                mkb$pendingModifierKey = code;
                if (mkb$pendingKey != 0) {
                    mkb$pendingModifier = KeyModifier.capture(mkb$pendingKey, mkb$keysDown);
                }
            } else {
                mkb$pendingKey = code;
                mkb$pendingModifier = KeyModifier.capture(code, mkb$keysDown);
            }
        } else if (code == Keyboard.KEY_ESCAPE && mkb$pendingKey == code) {
            mkb$saveBinding(0, KeyModifier.NONE);
        } else if (mkb$pendingKey != 0 && !mkb$isPendingKeyDown()
                && !mkb$pendingModifier.isHeld(mkb$keysDown)) {
            mkb$saveBinding(mkb$pendingKey, mkb$pendingModifier);
        } else if (mkb$pendingKey == 0 && code == mkb$pendingModifierKey) {
            mkb$saveBinding(code, KeyModifier.NONE);
        }
    }

    @Unique
    private boolean mkb$isPendingKeyDown() {
        return InputKeyCodes.isCharacter(mkb$pendingKey) ? mkb$pendingCharacterDown : mkb$keysDown[mkb$pendingKey];
    }

    @Unique
    private static boolean mkb$isModifier(int code) {
        return KeyModifier.SHIFT.matches(code) || KeyModifier.CONTROL.matches(code)
                || KeyModifier.ALT.matches(code) || KeyModifier.CONTROL_OR_COMMAND.matches(code);
    }

    @Unique
    private void mkb$moveFocus(int direction, boolean vertical) {
        int count = mkb$rows.size() * 2 + 2;
        boolean fromSearch = mkb$search.isFocused();
        if (!vertical && !fromSearch && mkb$focus < 0) {
            mkb$search.setFocused(true);
            return;
        }
        mkb$search.setFocused(false);
        int start = mkb$focus < 0 ? (direction > 0 ? count - 1 : 0) : mkb$focus;
        int step = vertical && mkb$focus >= 0 ? 2 : 1;
        for (int n = 1; n <= count; n++) {
            int next = (start + direction * step * n + count * 2) % count;
            if (!vertical && !fromSearch && (start + direction * n < 0 || start + direction * n >= count)) {
                mkb$focus = -1;
                mkb$search.setFocused(true);
                break;
            }
            if (mkb$canFocus(next)) {
                mkb$focus = next;
                int row = next / 2;
                if (row < mkb$rows.size()) {
                    mkb$firstRow = Math.min(mkb$firstRow, row);
                    mkb$firstRow = Math.max(mkb$firstRow, row - mkb$visibleRows() + 1);
                }
                break;
            }
        }
    }

    @Unique
    private boolean mkb$canFocus(int focus) {
        if (focus / 2 == mkb$rows.size()) {
            return focus % 2 == 1 || mkb$hasChanges();
        }
        int index = mkb$rows.get(focus / 2);
        return index >= 0 && (focus % 2 == 0 || mkb$isChanged(options.keyBindings[index]));
    }

    @Unique
    private void mkb$activate(int focus) {
        if (!mkb$canFocus(focus)) {
            return;
        }
        mkb$focus = focus;
        int row = focus / 2;
        if (row == mkb$rows.size()) {
            if (focus % 2 == 1) {
                mc.displayGuiScreen(parentScreen);
            } else {
                for (KeyBinding binding : options.keyBindings) {
                    mkb$reset(binding);
                }
                mkb$saveOptions();
                if (mkb$focus >= 0) {
                    mkb$focus++;
                }
            }
            return;
        }
        int index = mkb$rows.get(row);
        if (focus % 2 == 1) {
            mkb$reset(options.keyBindings[index]);
            mkb$saveOptions();
            if (mkb$focus >= 0) {
                mkb$focus--;
            }
            return;
        }
        mkb$clearCapture();
        mkb$editing = index;
        for (int code = 1; code < mkb$keysDown.length; code++) {
            mkb$keysDown[code] = Keyboard.isKeyDown(code);
        }
    }

    @Unique
    private static boolean mkb$isChanged(KeyBinding binding) {
        IKeyBinding settings = (IKeyBinding) binding;
        return binding.keyCode != settings.getDefaultKeyCode()
                || settings.getKeyModifier() != settings.getKeyModifierDefault();
    }

    @Unique
    private boolean mkb$hasChanges() {
        for (KeyBinding binding : options.keyBindings) {
            if (mkb$isChanged(binding)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static void mkb$reset(KeyBinding binding) {
        IKeyBinding settings = (IKeyBinding) binding;
        settings.setKeyModifierAndCode(settings.getKeyModifierDefault(), settings.getDefaultKeyCode());
    }

    @Unique
    private void mkb$clearCapture() {
        mkb$editing = -1;
        mkb$pendingKey = 0;
        mkb$pendingCharacterDown = false;
        mkb$pendingModifierKey = 0;
        mkb$pendingModifier = KeyModifier.NONE;
    }

    @Unique
    private void mkb$saveBinding(int code, KeyModifier modifier) {
        ((IKeyBinding) options.keyBindings[mkb$editing]).setKeyModifierAndCode(modifier, code);
        mkb$clearCapture();
        mkb$saveOptions();
    }

    @Unique
    private void mkb$saveOptions() {
        KeyBinding.resetKeyBindingArrayAndHash();
        options.saveOptions();
        if (!mkb$search.getText().trim().isEmpty()) {
            mkb$refreshRows();
            mkb$focus = -1;
        }
    }

    @Unique
    private int mkb$visibleRows() {
        return Math.max(1, (height - 30 - mkb$TOP) / mkb$ROW_HEIGHT);
    }

    @Unique
    private int mkb$listRight() {
        return width / 2 + Math.min(148, width / 2 - 6);
    }

    @Unique
    private int mkb$rowAt(int y) {
        int offset = y - mkb$TOP;
        int row = mkb$firstRow + offset / mkb$ROW_HEIGHT;
        return offset >= 0 && row < mkb$rows.size() && row < mkb$firstRow + mkb$visibleRows() ? row : -1;
    }

    @Unique
    private int mkb$rowY(int row) {
        return mkb$TOP + (row - mkb$firstRow) * mkb$ROW_HEIGHT;
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void mkb$draw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ci.cancel();
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.getString("controls.title"), width / 2, 5, 0xFFFFFF);
        int right = mkb$listRight();
        int left = width - right;
        mkb$search.drawTextBox();
        if (!mkb$search.isFocused() && mkb$search.getText().isEmpty()) {
            drawString(fontRenderer, I18n.getString("modernizedkb.controls.search"), left + 4, 24, 0xA0A0A0);
        }
        if (mkb$rows.isEmpty()) {
            drawCenteredString(fontRenderer, I18n.getString("modernizedkb.controls.noResults"), width / 2, mkb$TOP + 8, 0xA0A0A0);
        }
        int bindX = right - mkb$RESET_WIDTH - 4 - mkb$BIND_WIDTH;
        int hovered = mkb$rowAt(mouseY);
        List<String> tooltip = null;
        int end = Math.min(mkb$rows.size(), mkb$firstRow + mkb$visibleRows());
        for (int row = mkb$firstRow; row < end; row++) {
            int y = mkb$rowY(row);
            int index = mkb$rows.get(row);
            if (index < 0) {
                drawCenteredString(fontRenderer, mkb$headers.get(row), width / 2, y + 6, 0xFFFFFF);
                continue;
            }
            drawString(fontRenderer, fontRenderer.trimStringToWidth(options.getKeyBindingDescription(index),
                    Math.max(0, bindX - left - 6)), left, y + 6, 0xFFFFFF);
            boolean showTooltip = mkb$editing < 0 && hovered == row
                    && mouseY < y + 20 && mouseX >= bindX && mouseX < bindX + mkb$BIND_WIDTH;
            List<String> conflicts = mkb$conflicts(index, showTooltip);
            String label = options.getOptionDisplayString(index);
            if (index == mkb$editing) {
                label = "\u00a7e> " + label + " <";
            } else if (conflicts != null) {
                label = "\u00a7c" + label;
                if (showTooltip) {
                    tooltip = conflicts;
                }
            }
            GuiButton change = new GuiButton(0, bindX, y, mkb$BIND_WIDTH, 20, label);
            GuiButton reset = new GuiButton(0, right - mkb$RESET_WIDTH, y, mkb$RESET_WIDTH, 20,
                    I18n.getString("modernizedkb.controls.reset"));
            reset.enabled = mkb$isChanged(options.keyBindings[index]);
            mkb$drawButton(change, row * 2, mouseX, mouseY);
            mkb$drawButton(reset, row * 2 + 1, mouseX, mouseY);
        }
        mkb$drawScrollbar(right, end - mkb$firstRow);
        mkb$resetAll.enabled = mkb$hasChanges();
        mkb$drawButton(mkb$resetAll, mkb$rows.size() * 2, mouseX, mouseY);
        mkb$drawButton(mkb$done, mkb$rows.size() * 2 + 1, mouseX, mouseY);
        if (tooltip != null) {
            mkb$drawConflictTooltip(tooltip, mouseX, mouseY);
        }
    }

    @Unique
    private void mkb$drawButton(GuiButton button, int focus, int mouseX, int mouseY) {
        button.drawButton(mc, mouseX, mouseY);
        if (mkb$focus == focus) {
            drawRect(button.xPosition, button.yPosition, button.xPosition + 2, button.yPosition + 20, 0xFFFFFF55);
        }
    }

    @Unique
    private void mkb$drawScrollbar(int right, int visible) {
        if (visible < mkb$rows.size()) {
            int track = visible * mkb$ROW_HEIGHT;
            int thumb = mkb$thumbHeight(visible);
            int y = mkb$TOP + (track - thumb) * mkb$firstRow / (mkb$rows.size() - visible);
            drawRect(right + 2, mkb$TOP, right + 5, mkb$TOP + track, 0xFF202020);
            drawRect(right + 2, y, right + 5, y + thumb, 0xFFAAAAAA);
        }
    }

    @Unique
    private int mkb$thumbHeight(int visible) {
        return Math.max(8, visible * mkb$ROW_HEIGHT * visible / mkb$rows.size());
    }

    @Unique
    private List<String> mkb$conflicts(int index, boolean detailed) {
        IKeyBinding binding = (IKeyBinding) options.keyBindings[index];
        List<String> lines = null;
        for (int other = 0; other < options.keyBindings.length; other++) {
            if (!binding.conflicts(options.keyBindings[other])) {
                continue;
            }
            if (!detailed) {
                return java.util.Collections.emptyList();
            }
            if (lines == null) {
                lines = new ArrayList<String>();
                lines.add(I18n.getString("modernizedkb.controls.conflict"));
            }
            String name = options.getKeyBindingDescription(other);
            String key = ": " + options.getOptionDisplayString(other);
            int limit = width - 8;
            if (fontRenderer.getStringWidth(name + key) > limit) {
                int nameWidth = Math.max(0, limit - fontRenderer.getStringWidth("..." + key));
                name = fontRenderer.trimStringToWidth(name, nameWidth) + "...";
            }
            lines.add(name + key);
        }
        return lines;
    }

    @Unique
    private void mkb$drawConflictTooltip(List<String> lines, int mouseX, int mouseY) {
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, fontRenderer.getStringWidth(line));
        }
        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int visible = Math.min(lines.size(), Math.max(1, (height - 8) / lineHeight));
        int x = mouseX + 12;
        if (x + textWidth + 4 > width) {
            x = Math.max(4, mouseX - textWidth - 12);
        }
        int y = Math.max(4, Math.min(mouseY - 12, height - visible * lineHeight - 4));
        drawGradientRect(x - 4, y - 4, x + textWidth + 4, y + visible * lineHeight + 2, 0xF0100010, 0xF0100010);
        for (int line = 0; line < visible; line++) {
            String text = line > 0 && line == visible - 1 && visible < lines.size() ? "..." : lines.get(line);
            fontRenderer.drawStringWithShadow(text, x, y + line * lineHeight, line == 0 ? 0xFFFF55 : 0xFFFFFF);
        }
    }
}
