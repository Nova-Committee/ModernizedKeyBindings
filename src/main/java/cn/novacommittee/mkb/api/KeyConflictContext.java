package cn.novacommittee.mkb.api;

import net.minecraft.client.Minecraft;

public enum KeyConflictContext implements IKeyConflictContext {
    UNIVERSAL, GUI, IN_GAME;

    @Override
    public boolean isActive() {
        Minecraft client = Minecraft.getMinecraft();
        return this == UNIVERSAL || (client != null && (client.currentScreen != null) == (this == GUI));
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return this == UNIVERSAL || this == other;
    }
}
