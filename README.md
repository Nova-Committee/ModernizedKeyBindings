# Modernized KeyBindings

Modernized KeyBindings adds modifier shortcuts, searchable controls, and context-aware key conflicts to Minecraft 1.6.4 on Forge. Bind actions to combinations like **Ctrl + G** or **Shift + Mouse Button 4**, and see which other actions share your shortcut.

## Install

1. Use Minecraft **1.6.4** with Forge **9.11.1.1345**.
2. [Build Modernized KeyBindings from source](#build-from-source).
3. Place the built JAR and [Retrospectum Mixin 0.2.0](https://www.curseforge.com/minecraft/mc-mods/retrospectummixin/files/9469146) in the client's `mods` folder.

## Use it in game

Open **Options > Controls**. Select a binding, then press and release a key, mouse button, or combination. Press and release a modifier on its own to bind it by itself. Press **Escape** while editing to clear the binding.

- **Find an action.** Search by action name, current key or combination, or category. Use Tab or the arrow keys to move between controls, then Enter or Space to activate a focused button.
- **Check a conflict.** Hover over a red binding to see which actions may conflict and how they are bound. Actions can share a key across contexts, such as gameplay and an inventory screen, when their contexts do not overlap.
- **Share a shortcut.** All matching bindings can receive the same input. An active combination like **Shift + G** takes priority over a plain **G** binding.
- **Rebind the hotbar.** The nine hotbar slots appear under **Inventory** and default to **1–9**. On Linux, keys that report only a character can also be assigned.
- **Restore defaults.** Select **Reset** next to a binding or **Reset All** to restore its original key and modifier.

## For mod developers

Create bindings with `KeyBindings.create` from `cn.novacommittee.mkb.api`, then register them in a Forge 1.6.4 `KeyBindingRegistry.KeyHandler`. You can also configure an existing binding before or after registering its handler.

```java
KeyBinding shortcut = KeyBindings.create(
        "key.example.openPanel", KeyConflictContext.IN_GAME, KeyModifier.SHIFT, Keyboard.KEY_G);

KeyBindings.configure(existingBinding)
        .category("key.categories.example")
        .conflictContext(KeyConflictContext.GUI);
```

Use `GUI`, `IN_GAME`, or `UNIVERSAL` to control where a binding is active. Implement `IKeyConflictContext` for a custom context. Within a custom screen's input handler, call `IKeyBinding.isActiveAndMatches` with the incoming key code. Add the binding and category translation keys to your language files.

### Build from source

Set `JAVA_HOME` to JDK 17 and provide JDK 7 and JDK 8 via `JAVA7_HOME` and `JAVA8_HOME`. The game runs on Java 7 or 8.

- Windows: `.\gradlew.bat build`
- Linux: `./gradlew build`

The installable file is `build/libs/modernizedkb-retrospectum-1.6.4-0.1.0.jar`. Use the `runClient` task to launch a development client.

## Support and credits

Report issues on [GitHub](https://github.com/Nova-Committee/ModernizedKeyBindings/issues). The Forge 1.6.4 implementation was built with [Retrospectum Mod Template](https://github.com/DesideriumMC/RetrospectumModTemplate). The modifier and conflict behavior follows [NeoForge](https://github.com/neoforged/NeoForge/tree/0da72d30889eb4d37eebbb70c84b83701829a6b5).

Modernized KeyBindings is licensed under the [GNU Lesser General Public License v3.0](LICENSE), which incorporates the [GNU General Public License v3.0](LICENSE-GPL-3.0). The Retrospectum template carries its own [MIT notice](LICENSE-RETROSPECTUM).
