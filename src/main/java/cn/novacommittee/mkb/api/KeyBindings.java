package cn.novacommittee.mkb.api;

import net.minecraft.client.settings.KeyBinding;

public final class KeyBindings {
    private KeyBindings() {
    }

    /** Pass the returned binding to a Forge 1.6.4 KeyBindingRegistry.KeyHandler. */
    public static KeyBinding create(String description, IKeyConflictContext context, KeyModifier modifier, int code) {
        return create(description, context, modifier, code, "key.categories.misc");
    }

    public static KeyBinding create(String description, IKeyConflictContext context, KeyModifier modifier, int code,
                                    String categoryTranslationKey) {
        KeyBinding binding = new KeyBinding(description, code);
        configure(binding, context, categoryTranslationKey);
        ((IKeyBinding) binding).setInitialKeyModifierAndCode(modifier, code);
        return binding;
    }

    /** Adds metadata to an existing binding without changing its current or default shortcut. */
    public static Configuration configure(KeyBinding binding) {
        if (binding == null) {
            throw new NullPointerException("binding");
        }
        return new Configuration((IKeyBinding) binding);
    }

    /** Sets both metadata properties on an existing binding. */
    public static void configure(KeyBinding binding, IKeyConflictContext context, String categoryTranslationKey) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        IKeyBinding settings = (IKeyBinding) binding;
        settings.setKeyCategory(categoryTranslationKey);
        settings.setKeyConflictContext(context);
    }

    public static final class Configuration {
        private final IKeyBinding binding;

        private Configuration(IKeyBinding binding) {
            this.binding = binding;
        }

        public Configuration category(String translationKey) {
            binding.setKeyCategory(translationKey);
            return this;
        }

        public Configuration conflictContext(IKeyConflictContext context) {
            binding.setKeyConflictContext(context);
            return this;
        }
    }
}
