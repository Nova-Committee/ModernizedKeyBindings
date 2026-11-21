package cn.novacommittee.mkb.mixin;

import cn.novacommittee.mkb.HotbarBindings;
import cn.novacommittee.mkb.InputKeyCodes;
import cn.novacommittee.mkb.api.IKeyBinding;
import cn.novacommittee.mkb.api.KeyModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiContainer.class)
public abstract class ContainerMixin {
    @ModifyVariable(method = "keyTyped", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int mkb$resolveCharacterKey(int code, char character) {
        return InputKeyCodes.fromLegacyEvent(code, character);
    }

    @ModifyVariable(method = "checkHotbarKeys", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int mkb$resolveHotbarSlot(int code) {
        HotbarBindings settings = (HotbarBindings) Minecraft.getMinecraft().gameSettings;
        KeyBinding[] hotbar = settings.mkb$getHotbarBindings();
        int plain = Keyboard.KEY_NONE;
        for (int slot = 0; slot < hotbar.length; slot++) {
            IKeyBinding binding = (IKeyBinding) hotbar[slot];
            if (binding.isActiveAndMatches(code)) {
                if (binding.getKeyModifier() != KeyModifier.NONE) {
                    return Keyboard.KEY_1 + slot;
                }
                if (plain == Keyboard.KEY_NONE) {
                    plain = Keyboard.KEY_1 + slot;
                }
            }
        }
        return plain;
    }

    @Redirect(method = {"keyTyped", "mouseClicked", "mouseMovedOrUp"},
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/settings/KeyBinding;keyCode:I"))
    private int mkb$matchShortcut(KeyBinding binding) {
        return ((IKeyBinding) binding).isActiveAndMatches(binding.keyCode) ? binding.keyCode : Integer.MIN_VALUE;
    }
}
