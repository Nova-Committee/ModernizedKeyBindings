package cn.novacommittee.mkb.mixin;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = KeyBindingRegistry.KeyHandler.class, remap = false)
public abstract class ForgeKeyHandlerMixin {
    @Unique private KeyBinding mkb$currentBinding;

    @Redirect(method = "keyTick", remap = false,
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/settings/KeyBinding;keyCode:I", remap = true))
    private int mkb$rememberBinding(KeyBinding binding) {
        mkb$currentBinding = binding;
        return binding.keyCode;
    }

    @Redirect(method = "keyTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isKeyDown(I)Z"))
    private boolean mkb$pollCharacterKey(int code) {
        return code >= Keyboard.KEYBOARD_SIZE
                ? GameSettings.isKeyDown(mkb$currentBinding) : Keyboard.isKeyDown(code);
    }
}
