package cn.novacommittee.mkb.mixin;

import cn.novacommittee.mkb.api.IKeyBinding;
import cn.novacommittee.mkb.api.IKeyConflictContext;
import cn.novacommittee.mkb.api.KeyConflictContext;
import cn.novacommittee.mkb.api.KeyModifier;
import java.util.List;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements IKeyBinding {
    @Shadow public static List<KeyBinding> keybindArray;
    @Shadow public int keyCode;
    @Shadow public int pressTime;
    @Shadow public boolean pressed;

    @Unique private int mkb$defaultCode;
    @Unique private String mkb$category = "key.categories.misc";
    @Unique private KeyModifier mkb$defaultModifier = KeyModifier.NONE;
    @Unique private KeyModifier mkb$modifier = KeyModifier.NONE;
    @Unique private IKeyConflictContext mkb$context = KeyConflictContext.UNIVERSAL;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mkb$rememberDefault(String description, int code, CallbackInfo ci) {
        mkb$defaultCode = code;
    }

    @Override
    public IKeyConflictContext getKeyConflictContext() {
        return mkb$context;
    }

    @Override
    public void setKeyConflictContext(IKeyConflictContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        mkb$context = context;
    }

    @Override
    public KeyModifier getKeyModifier() {
        return mkb$modifier;
    }

    @Override
    public KeyModifier getKeyModifierDefault() {
        return mkb$defaultModifier;
    }

    @Override
    public int getDefaultKeyCode() {
        return mkb$defaultCode;
    }

    @Override
    public String getKeyCategory() {
        return mkb$category;
    }

    @Override
    public void setKeyCategory(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            throw new IllegalArgumentException("A category translation key is required");
        }
        mkb$category = translationKey;
    }

    @Override
    public void setKeyModifierAndCode(KeyModifier modifier, int code) {
        if (modifier == null) {
            throw new NullPointerException("modifier");
        }
        mkb$modifier = code == 0 || modifier.matches(code) ? KeyModifier.NONE : modifier;
        keyCode = code;
        pressed = false;
        pressTime = 0;
    }

    @Override
    public void setInitialKeyModifierAndCode(KeyModifier modifier, int code) {
        setKeyModifierAndCode(modifier, code);
        mkb$defaultModifier = mkb$modifier;
        mkb$defaultCode = code;
    }

    @Override
    public void setToDefault() {
        setKeyModifierAndCode(mkb$defaultModifier, mkb$defaultCode);
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    @Override
    public boolean isActiveAndMatches(int code) {
        return code != 0 && code == keyCode && mkb$context.isActive()
                && (mkb$modifier == KeyModifier.NONE
                    ? mkb$context.conflicts(KeyConflictContext.IN_GAME) || !KeyModifier.anyHeld()
                    : mkb$modifier.isHeld());
    }

    @Override
    public boolean conflicts(KeyBinding other) {
        if (other == (Object) this || keyCode == 0 || other.keyCode == 0) {
            return false;
        }
        IKeyBinding binding = (IKeyBinding) other;
        IKeyConflictContext context = binding.getKeyConflictContext();
        if (!mkb$context.conflicts(context) && !context.conflicts(mkb$context)) {
            return false;
        }

        KeyModifier modifier = binding.getKeyModifier();
        if (mkb$modifier.matches(other.keyCode) || modifier.matches(keyCode)) {
            return true;
        }
        return keyCode == other.keyCode && (mkb$modifier.overlaps(modifier)
                || ((mkb$context.conflicts(KeyConflictContext.IN_GAME) || context.conflicts(KeyConflictContext.IN_GAME))
                    && (mkb$modifier == KeyModifier.NONE || modifier == KeyModifier.NONE)));
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private static void mkb$onTick(int code, CallbackInfo ci) {
        ci.cancel();
        mkb$dispatch(code, true);
    }

    @Inject(method = "setKeyBindState", at = @At("HEAD"), cancellable = true)
    private static void mkb$setState(int code, boolean down, CallbackInfo ci) {
        ci.cancel();
        if (code == 0) {
            return;
        }
        if (down) {
            mkb$dispatch(code, false);
            return;
        }
        for (KeyBinding binding : keybindArray) {
            KeyModifier modifier = ((IKeyBinding) binding).getKeyModifier();
            if (binding.keyCode == code || (binding.pressed && modifier.matches(code) && !modifier.isHeld())) {
                binding.pressed = false;
            }
        }
    }

    @Unique
    private static void mkb$dispatch(int code, boolean click) {
        if (code == 0) {
            return;
        }
        boolean modified = false;
        for (KeyBinding binding : keybindArray) {
            IKeyBinding settings = (IKeyBinding) binding;
            if (settings.getKeyModifier() != KeyModifier.NONE && settings.isActiveAndMatches(code)) {
                mkb$activate(binding, click);
                modified = true;
            }
        }
        if (!modified) {
            for (KeyBinding binding : keybindArray) {
                IKeyBinding settings = (IKeyBinding) binding;
                if (settings.getKeyModifier() == KeyModifier.NONE && settings.isActiveAndMatches(code)) {
                    mkb$activate(binding, click);
                }
            }
        }
    }

    @Unique
    private static void mkb$activate(KeyBinding binding, boolean click) {
        if (click) {
            ++binding.pressTime;
        } else {
            binding.pressed = true;
        }
    }
}
