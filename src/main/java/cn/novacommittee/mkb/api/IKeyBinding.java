package cn.novacommittee.mkb.api;

import net.minecraft.client.settings.KeyBinding;

public interface IKeyBinding {
    IKeyConflictContext getKeyConflictContext();
    void setKeyConflictContext(IKeyConflictContext context);
    KeyModifier getKeyModifier();
    KeyModifier getKeyModifierDefault();
    int getDefaultKeyCode();
    String getKeyCategory();
    void setKeyCategory(String translationKey);
    void setKeyModifierAndCode(KeyModifier modifier, int code);
    void setInitialKeyModifierAndCode(KeyModifier modifier, int code);
    void setToDefault();
    boolean isActiveAndMatches(int code);
    boolean conflicts(KeyBinding other);
}
