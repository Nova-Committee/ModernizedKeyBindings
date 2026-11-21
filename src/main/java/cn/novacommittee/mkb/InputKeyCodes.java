package cn.novacommittee.mkb;

import java.util.Locale;
import org.lwjgl.input.Keyboard;

public final class InputKeyCodes {
    private static final boolean LEGACY_CHARACTER_INPUT = Boolean.parseBoolean(System.getProperty(
            "mkb.legacyCharacterKeys", Boolean.toString(System.getProperty("os.name", "").startsWith("Linux"))));

    private InputKeyCodes() {
    }

    public static boolean usesLegacyCharacterInput() {
        return LEGACY_CHARACTER_INPUT;
    }

    public static int fromLegacyEvent(int key, char character) {
        return LEGACY_CHARACTER_INPUT ? fromEvent(key, character) : key;
    }

    public static int fromEvent(int key, char character) {
        if (key != Keyboard.KEY_NONE) {
            return key;
        }
        if (character == 0 || Character.isISOControl(character) || Character.isSurrogate(character)) {
            return Keyboard.KEY_NONE;
        }
        return Keyboard.KEYBOARD_SIZE + character;
    }

    public static boolean isCharacter(int code) {
        return code > Keyboard.KEYBOARD_SIZE && code <= Keyboard.KEYBOARD_SIZE + Character.MAX_VALUE
                && fromEvent(Keyboard.KEY_NONE, (char) (code - Keyboard.KEYBOARD_SIZE)) == code;
    }

    public static String keyboardName(int code) {
        if (isCharacter(code)) {
            return String.valueOf((char) (code - Keyboard.KEYBOARD_SIZE)).toUpperCase(Locale.ROOT);
        }
        if (code >= 0 && code < Keyboard.KEYBOARD_SIZE) {
            String name = Keyboard.getKeyName(code);
            if (name != null) {
                return name;
            }
        }
        return Integer.toString(code);
    }
}
