package cn.novacommittee.mkb.mixin;

import cn.novacommittee.mkb.InputKeyCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class LegacyCharacterInputMixin {
    @Redirect(method = "runTick", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", ordinal = 0, remap = false))
    private int mkb$resolveStateKey() {
        int code = InputKeyCodes.fromEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter());
        if (code == Keyboard.KEY_NONE && !Keyboard.getEventKeyState()) {
            // LWJGL 2 can release a character key without including its character.
            for (Object entry : KeyBinding.keybindArray) {
                KeyBinding binding = (KeyBinding) entry;
                if (InputKeyCodes.isCharacter(binding.keyCode)) {
                    binding.pressed = false;
                }
            }
        }
        return code;
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", ordinal = 1, remap = false))
    private int mkb$resolveClickKey() {
        return InputKeyCodes.fromEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter());
    }
}
