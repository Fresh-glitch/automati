# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Automati is a Minecraft Forge mod (Minecraft 26.2, Forge 65.0.3, Java 25) adding
industrial machines powered by an energy unit called the Erg. Mod id: `automati`.
The version is declared in two places that must stay in sync: `build.gradle`
(`version = '...'`) and `src/main/resources/META-INF/mods.toml`.

## Commands

```
./gradlew build                 # build the mod jar (build/libs/)
./gradlew runClient             # launch a dev client with the mod loaded
./gradlew runServer             # dev server (--nogui)
./gradlew runGameTestServer     # run game tests (namespace: automati)
./gradlew runData               # datagen into src/generated/resources
./gradlew processResources      # rebuild assets, then F3+T in-game (assets) or /reload (data)
```

There is no unit-test suite; verification is done in-game via `runClient` or game tests.
The owner tests changes in-game before releases.

## Architecture

All Java code lives in one flat package: `src/main/java/io/github/freshglitch/automati/`.

- **`Automati.java` is the registration hub.** Every block, item, block entity,
  menu, sound, recipe type, and creative tab is registered here via
  `DeferredRegister`/`RegistryObject`. New content starts here.
- **Machines follow a four-class pattern built on four base classes** —
  `AbstractMachineBlock` (LIT state, ticking dispatch, right-click opens menu),
  `AbstractErgBlockEntity` (energy buffer + save/load, port type, goggles HUD
  sync, `distributeEnergy()` for producers/conduits), `AbstractErgMenu`
  (open-race fix, split 32-bit energy getter, stillValid, player inventory),
  `AbstractErgScreen` (panel + standard Erg gauge + tooltip). A new machine:
  1. `FooBlock extends AbstractMachineBlock` — override `newBlockEntity`, add
     personality (facing, `animateTick`, hazards)
  2. `FooBlockEntity extends AbstractErgBlockEntity` — pass capacity/rate and
     an `ErgPort` (RECEIVE_ONLY consumer / EXTRACT_ONLY producer / OPEN
     conduit) to super; override `serverTick`, call
     `maybeSyncEnergyToClients()` in it
  3. `FooMenu extends AbstractErgMenu` — pass the energy data-slot index to
     super; add machine slots + `addPlayerInventory()`; implement
     `quickMoveStack` and `getCapacity()`
  4. `FooScreen extends AbstractErgScreen<FooMenu>` — pass the GUI texture to
     super; draw extras in `extractMachine()`; register the screen in
     `Automati.ClientModEvents.onClientSetup`
  Crusher is the fullest reference example; the erg cable shows a
  non-machine `AbstractErgBlockEntity` (no menu, custom block shape).
- **Energy**: `ErgStorage` extends Forge's `EnergyStorage`, exposed through the
  standard Forge energy capability so machines interoperate. Generators push to
  neighbours each tick; block entities expose capabilities via `LazyOptional`.
- **Recipes are data-driven**: `CrusherRecipe` extends vanilla `SingleItemRecipe`
  (stonecutter-style). Recipe JSONs live in `data/automati/recipe/crushing/`;
  adding a crushing recipe is JSON-only, no Java.
- **Resources**: client-side under `assets/automati/` (models, blockstates,
  textures, lang, sounds), server/data under `data/automati/` (recipes, loot
  tables, damage types) plus vanilla tag overrides under `data/minecraft/tags/`.

## Minecraft 26.2 API drift — do not guess symbols

26.2/Forge 65 diverges from the 1.21-era APIs that dominate documentation and
training data. When the compiler rejects a symbol, inspect the real API with javap
instead of guessing:

```
& "C:\Users\Fresh\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2\bin\javap.exe" -cp "C:\Users\Fresh\.gradle\caches\minecraftforge\forgegradle\mavenizer\caches\forge\net\minecraftforge\forge\26.2-65.0.3\official\26.2\recompiled.jar" <fully.qualified.Class>
```

Known 26.2 changes already applied in this codebase:
- `ResourceLocation` → `Identifier`; `level.isClientSide()` is a method (field is private)
- BlockEntity save/load uses `ValueOutput`/`ValueInput` with codecs (e.g. `ItemStack.OPTIONAL_CODEC`)
- `new BlockEntityType<>(factory, Set.of(...))` — the Builder is gone
- Menus open via `serverPlayer.openMenu(provider, pos)` — NetworkHooks is gone
- Screens use `GuiGraphicsExtractor` + `extractBackground`/`extractRenderState`,
  `RenderPipelines.GUI_TEXTURED` blits, `setTooltipForNextFrame`
- Drop-contents-on-break hook is `BlockEntity.preRemoveSideEffects`

Gotchas:
- `ContainerData` slots are 16-bit — split 32-bit energy values across two slots
- Menus need a ~5-tick delayed `broadcastFullState()` after opening (initial sync
  races the open packet)
