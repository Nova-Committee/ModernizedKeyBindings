package cn.novacommittee.mkb.mixin;

import cn.novacommittee.mkb.HotbarBindings;
import cn.novacommittee.mkb.InputKeyCodes;
import cn.novacommittee.mkb.api.IKeyBinding;
import cn.novacommittee.mkb.api.KeyConflictContext;
import cn.novacommittee.mkb.api.KeyBindings;
import cn.novacommittee.mkb.api.KeyModifier;
import java.util.Arrays;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public abstract class GameSettingsMixin implements HotbarBindings {
    @Shadow public KeyBinding[] keyBindings;
    @Shadow public KeyBinding keyBindInventory;
    @Shadow public KeyBinding keyBindDrop;
    @Shadow public KeyBinding keyBindPickBlock;
    @Shadow public static String getKeyDisplayString(int code) {
        throw new AssertionError();
    }

    @Unique private KeyModifier mkb$loadedModifier = KeyModifier.NONE;
    @Unique private KeyBinding[] mkb$hotbarBindings;

    @Redirect(method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/GameSettings;loadOptions()V"))
    private void mkb$loadWithHotbar(GameSettings settings) {
        mkb$addHotbarBindings();
        settings.loadOptions();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mkb$setVanillaContexts(CallbackInfo ci) {
        mkb$addHotbarBindings();
        for (KeyBinding binding : keyBindings) {
            IKeyBinding settings = (IKeyBinding) binding;
            String description = binding.keyDescription;
            if ("key.forward".equals(description) || "key.back".equals(description)
                    || "key.left".equals(description) || "key.right".equals(description)
                    || "key.jump".equals(description) || "key.sneak".equals(description)) {
                settings.setKeyCategory("modernizedkb.controls.movement");
            } else if ("key.attack".equals(description) || "key.use".equals(description)) {
                settings.setKeyCategory("modernizedkb.controls.gameplay");
            } else if (binding == keyBindInventory || binding == keyBindDrop || binding == keyBindPickBlock) {
                settings.setKeyCategory("modernizedkb.controls.inventory");
            } else if ("key.chat".equals(description) || "key.playerlist".equals(description)
                    || "key.command".equals(description)) {
                settings.setKeyCategory("modernizedkb.controls.multiplayer");
            }
            if (binding != keyBindInventory && binding != keyBindDrop && binding != keyBindPickBlock
                    && (description == null || !description.startsWith("key.hotbar."))) {
                settings.setKeyConflictContext(KeyConflictContext.IN_GAME);
            }
        }
    }

    @Unique
    private void mkb$addHotbarBindings() {
        if (mkb$hotbarBindings != null) {
            return;
        }
        mkb$hotbarBindings = new KeyBinding[9];
        int previousLength = keyBindings.length;
        keyBindings = Arrays.copyOf(keyBindings, previousLength + mkb$hotbarBindings.length);
        for (int slot = 0; slot < mkb$hotbarBindings.length; slot++) {
            KeyBinding binding = KeyBindings.create("key.hotbar." + (slot + 1), KeyConflictContext.UNIVERSAL,
                    KeyModifier.NONE, Keyboard.KEY_1 + slot, "modernizedkb.controls.inventory");
            mkb$hotbarBindings[slot] = binding;
            keyBindings[previousLength + slot] = binding;
        }
    }

    @Override
    public KeyBinding[] mkb$getHotbarBindings() {
        return mkb$hotbarBindings;
    }

    @Redirect(method = "loadOptions", at = @At(value = "INVOKE",
            target = "Ljava/lang/String;split(Ljava/lang/String;)[Ljava/lang/String;"))
    private String[] mkb$readModifier(String line, String separator) {
        String[] parts = line.split(separator);
        mkb$loadedModifier = parts.length > 2 && parts[0].startsWith("key_")
                ? KeyModifier.fromName(parts[2]) : KeyModifier.NONE;
        return parts;
    }

    @Redirect(method = "loadOptions", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/client/settings/KeyBinding;keyCode:I"))
    private void mkb$loadBinding(KeyBinding binding, int code) {
        ((IKeyBinding) binding).setKeyModifierAndCode(mkb$loadedModifier, code);
    }

    @Unique private KeyModifier mkb$savedModifier = KeyModifier.NONE;

    @Redirect(method = "saveOptions", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/settings/KeyBinding;keyCode:I"))
    private int mkb$readBinding(KeyBinding binding) {
        mkb$savedModifier = ((IKeyBinding) binding).getKeyModifier();
        return binding.keyCode;
    }

    @ModifyArg(method = "saveOptions",
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/KeyBinding;keyCode:I")),
            at = @At(value = "INVOKE", target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V"), index = 0)
    private String mkb$saveModifier(String line) {
        return mkb$savedModifier == KeyModifier.NONE ? line : line + ":" + mkb$savedModifier.name();
    }

    @Inject(method = "getOptionDisplayString", at = @At("HEAD"), cancellable = true)
    private void mkb$displayCombo(int index, CallbackInfoReturnable<String> cir) {
        KeyBinding binding = keyBindings[index];
        cir.setReturnValue(((IKeyBinding) binding).getKeyModifier().format(binding.keyCode == 0 ? "--" : getKeyDisplayString(binding.keyCode)));
    }

    @Inject(method = "getKeyDisplayString", at = @At("HEAD"), cancellable = true)
    private static void mkb$safeKeyName(int code, CallbackInfoReturnable<String> cir) {
        if (code >= 0) {
            cir.setReturnValue(InputKeyCodes.keyboardName(code));
        }
    }

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void mkb$checkModifier(KeyBinding binding, CallbackInfoReturnable<Boolean> cir) {
        if (!((IKeyBinding) binding).isActiveAndMatches(binding.keyCode)) {
            cir.setReturnValue(false);
        } else if (binding.keyCode >= Keyboard.KEYBOARD_SIZE) {
            cir.setReturnValue(InputKeyCodes.isCharacter(binding.keyCode) && binding.pressed);
        }
    }
}
