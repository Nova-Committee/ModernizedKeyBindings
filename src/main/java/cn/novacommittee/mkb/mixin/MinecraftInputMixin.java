package cn.novacommittee.mkb.mixin;

import cn.novacommittee.mkb.HotbarBindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Minecraft.class)
public abstract class MinecraftInputMixin {
    @Shadow public GameSettings gameSettings;
    @Shadow public GuiScreen currentScreen;
    @Shadow public EntityClientPlayerMP thePlayer;

    @Redirect(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/settings/KeyBinding;onTick(I)V"))
    private void mkb$clickKey(int code) {
        KeyBinding.onTick(code);
        if (gameSettings == null) {
            return;
        }
        KeyBinding[] hotbar = ((HotbarBindings) gameSettings).mkb$getHotbarBindings();
        for (int slot = 0; slot < hotbar.length; slot++) {
            if (hotbar[slot].isPressed() && currentScreen == null && thePlayer != null) {
                thePlayer.inventory.currentItem = slot;
            }
        }
    }

    @Redirect(method = "runTick",
            slice = @Slice(
                    from = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                            target = "Lnet/minecraft/client/settings/GameSettings;smoothCamera:Z")),
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;getEventKey()I", ordinal = 0, remap = false))
    private int mkb$disableFixedHotbarKeys() {
        return Keyboard.KEY_NONE;
    }
}
