package cn.novacommittee.mkb.api;

import org.lwjgl.input.Keyboard;

public enum KeyModifier {
    NONE(0, 0, ""),
    SHIFT(Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT, "Shift"),
    CONTROL(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, "Ctrl"),
    ALT(Keyboard.KEY_LMENU, Keyboard.KEY_RMENU, "Alt"),
    CONTROL_OR_COMMAND(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, "Ctrl");

    private static final boolean MAC = System.getProperty("os.name", "").startsWith("Mac");
    private static final KeyModifier[] MODIFIERS = MAC
            ? new KeyModifier[] { SHIFT, CONTROL_OR_COMMAND, CONTROL, ALT }
            : new KeyModifier[] { SHIFT, CONTROL, ALT };

    private final int left;
    private final int right;
    private final String label;

    KeyModifier(int left, int right, String label) {
        this.left = left;
        this.right = right;
        this.label = label;
    }

    private int leftKey() {
        return this == CONTROL_OR_COMMAND && MAC ? Keyboard.KEY_LMETA : left;
    }

    private int rightKey() {
        return this == CONTROL_OR_COMMAND && MAC ? Keyboard.KEY_RMETA : right;
    }

    public boolean matches(int code) {
        return this != NONE && (code == leftKey() || code == rightKey());
    }

    public boolean isHeld() {
        return this != NONE && Keyboard.isCreated()
                && (Keyboard.isKeyDown(leftKey()) || Keyboard.isKeyDown(rightKey()));
    }

    public boolean overlaps(KeyModifier other) {
        return this == other || matches(other.leftKey());
    }

    public static boolean anyHeld() {
        return capture(0) != NONE;
    }

    public static KeyModifier capture(int code) {
        for (KeyModifier modifier : MODIFIERS) {
            if (!modifier.matches(code) && modifier.isHeld()) {
                return modifier;
            }
        }
        return NONE;
    }

    public static KeyModifier capture(int code, boolean[] keysDown) {
        for (KeyModifier modifier : MODIFIERS) {
            if (!modifier.matches(code) && modifier.isHeld(keysDown)) {
                return modifier;
            }
        }
        return NONE;
    }

    public boolean isHeld(boolean[] keysDown) {
        return this != NONE && (keysDown[leftKey()] || keysDown[rightKey()]);
    }

    public String format(String keyName) {
        if (this == NONE) {
            return keyName;
        }
        return (this == CONTROL_OR_COMMAND && MAC ? "Cmd" : label) + " + " + keyName;
    }

    public static KeyModifier fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
