package io.github.freshglitch.automati;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Automati.MODID)
public final class Automati {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "automati";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "automati" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "automati" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "automati" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes (the "brains" of machines like the coal generator)
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold MenuTypes (server<->client container GUIs)
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    // Create a Deferred Register to hold SoundEvents
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    // Deferred Registers for the data-driven crushing recipe system
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    public static final DeferredRegister<RecipeBookCategory> RECIPE_BOOK_CATEGORIES = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, MODID);

    // The crushing recipe type: recipes live in data/automati/recipe/crushing/
    public static final RegistryObject<RecipeType<CrusherRecipe>> CRUSHING_RECIPE_TYPE =
        RECIPE_TYPES.register("crushing", () -> RecipeType.simple(Identifier.fromNamespaceAndPath(MODID, "crushing")));
    public static final RegistryObject<RecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
        RECIPE_SERIALIZERS.register("crushing", () -> new RecipeSerializer<>(CrusherRecipe.MAP_CODEC, CrusherRecipe.STREAM_CODEC));
    public static final RegistryObject<RecipeBookCategory> CRUSHING_CATEGORY =
        RECIPE_BOOK_CATEGORIES.register("crushing", RecipeBookCategory::new);

    // Damage dealt by standing on active crusher blades (defined in data/automati/damage_type/)
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> CRUSHER_DAMAGE =
        net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MODID, "crusher"));

    // The mechanical hum the coal generator makes while running
    public static final RegistryObject<SoundEvent> COAL_GENERATOR_LOOP = SOUNDS.register("coal_generator_loop",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "coal_generator_loop")));
    // The sputtering clunk when the generator chokes on a full ash bin
    public static final RegistryObject<SoundEvent> COAL_GENERATOR_CLOG = SOUNDS.register("coal_generator_clog",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "coal_generator_clog")));

    // Ash: the combustion byproduct left behind by the coal generator
    public static final RegistryObject<Item> ASH = ITEMS.register("ash",
        () -> new Item(new Item.Properties().setId(ITEMS.key("ash")))
    );
    // The Bucking Iron: hand-crushing tool, the sweat-powered tier zero of ore
    // processing (full crusher yield 50% of the time, half rounded up otherwise)
    public static final RegistryObject<Item> BUCKING_IRON = ITEMS.register("bucking_iron",
        () -> new BuckingIronItem(new Item.Properties()
            .setId(ITEMS.key("bucking_iron"))
            .durability(192)
        )
    );

    // Iron Stick: all-metal tool rod, the bucking iron's handle
    public static final RegistryObject<Item> IRON_STICK = ITEMS.register("iron_stick",
        () -> new Item(new Item.Properties().setId(ITEMS.key("iron_stick")))
    );

    // Engineer's Wrench: toggles conduit connections — severs auto-links,
    // splices policy-declined ones (copper head, iron stick handle)
    public static final RegistryObject<Item> ENGINEERS_WRENCH = ITEMS.register("engineers_wrench",
        () -> new WrenchItem(new Item.Properties().setId(ITEMS.key("engineers_wrench")).stacksTo(1))
    );

    // Engineer's Goggles: worn on the head, they reveal the Erg charge of any
    // machine or cable you look at
    public static final RegistryObject<Item> ENGINEERS_GOGGLES = ITEMS.register("engineers_goggles",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("engineers_goggles"))
            .stacksTo(1)
            .component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
                net.minecraft.world.item.equipment.Equippable.builder(net.minecraft.world.entity.EquipmentSlot.HEAD)
                    .setCameraOverlay(Identifier.fromNamespaceAndPath(MODID, "misc/goggles_blur"))
                    .setAsset(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                        Identifier.fromNamespaceAndPath(MODID, "goggles")))
                    .build())
        )
    );

    // Metal dusts: pulverized ore from the crusher, smeltable into ingots
    public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust",
        () -> new Item(new Item.Properties().setId(ITEMS.key("iron_dust")))
    );
    public static final RegistryObject<Item> COPPER_DUST = ITEMS.register("copper_dust",
        () -> new Item(new Item.Properties().setId(ITEMS.key("copper_dust")))
    );
    public static final RegistryObject<Item> GOLD_DUST = ITEMS.register("gold_dust",
        () -> new Item(new Item.Properties().setId(ITEMS.key("gold_dust")))
    );

    // One machine = one registration call. Bundles the four registry entries
    // every machine needs (block, block item, block entity type, menu type)
    // with the standard machine properties (metal, strength 5/6, needs the
    // right tool); per-machine personality (light levels etc.) goes through
    // the props customizer. Conduits (cable/duct) don't fit this mold — no
    // menu — and register manually below.
    public record Machine<BE extends net.minecraft.world.level.block.entity.BlockEntity,
                          M extends net.minecraft.world.inventory.AbstractContainerMenu>(
        RegistryObject<Block> block,
        RegistryObject<Item> item,
        RegistryObject<BlockEntityType<BE>> blockEntity,
        RegistryObject<MenuType<M>> menu) {
    }

    private static <BE extends net.minecraft.world.level.block.entity.BlockEntity,
                    M extends net.minecraft.world.inventory.AbstractContainerMenu>
    Machine<BE, M> registerMachine(String name,
                                   java.util.function.Function<BlockBehaviour.Properties, ? extends Block> blockFactory,
                                   java.util.function.UnaryOperator<BlockBehaviour.Properties> props,
                                   BlockEntityType.BlockEntitySupplier<BE> blockEntityFactory,
                                   net.minecraftforge.network.IContainerFactory<M> menuFactory) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> blockFactory.apply(
            props.apply(BlockBehaviour.Properties.of()
                .setId(BLOCKS.key(name))
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(5.0F, 6.0F)
                .requiresCorrectToolForDrops())));
        RegistryObject<Item> item = ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().setId(ITEMS.key(name)).useBlockDescriptionPrefix()));
        RegistryObject<BlockEntityType<BE>> blockEntity = BLOCK_ENTITIES.register(name,
            () -> new BlockEntityType<>(blockEntityFactory, java.util.Set.of(block.get())));
        RegistryObject<MenuType<M>> menu = MENUS.register(name,
            () -> IForgeMenuType.create(menuFactory));
        return new Machine<>(block, item, blockEntity, menu);
    }

    // Creates a new Block with the id "automati:factory_block", combining the namespace and path
    public static final RegistryObject<Block> FACTORY_BLOCK = BLOCKS.register("factory_block",
        () -> new FactoryBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("factory_block"))
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(5.0F, 6.0F)
            .requiresCorrectToolForDrops()
        )
    );
    // Creates a new BlockItem with the id "automati:factory_block", combining the namespace and path
    public static final RegistryObject<Item> FACTORY_BLOCK_ITEM = ITEMS.register("factory_block",
        () -> new BlockItem(FACTORY_BLOCK.get(), new Item.Properties().setId(ITEMS.key("factory_block")).useBlockDescriptionPrefix())
    );

    // The coal generator: Automati's first machine, burns coal/charcoal into Ergs
    public static final Machine<CoalGeneratorBlockEntity, CoalGeneratorMenu> COAL_GENERATOR_MACHINE =
        registerMachine("coal_generator", CoalGeneratorBlock::new,
            p -> p.lightLevel(state -> state.getValue(CoalGeneratorBlock.LIT) ? 13 : 0),
            CoalGeneratorBlockEntity::new, CoalGeneratorMenu::new);
    public static final RegistryObject<Block> COAL_GENERATOR = COAL_GENERATOR_MACHINE.block();
    public static final RegistryObject<Item> COAL_GENERATOR_ITEM = COAL_GENERATOR_MACHINE.item();
    public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BLOCK_ENTITY = COAL_GENERATOR_MACHINE.blockEntity();
    public static final RegistryObject<MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU = COAL_GENERATOR_MACHINE.menu();

    // The load bank: a configurable energy sink for testing generators under load
    public static final Machine<LoadBankBlockEntity, LoadBankMenu> LOAD_BANK_MACHINE =
        registerMachine("load_bank", LoadBankBlock::new,
            p -> p.lightLevel(state -> state.getValue(LoadBankBlock.LIT) ? 7 : 0),
            LoadBankBlockEntity::new, LoadBankMenu::new);
    public static final RegistryObject<Block> LOAD_BANK = LOAD_BANK_MACHINE.block();
    public static final RegistryObject<Item> LOAD_BANK_ITEM = LOAD_BANK_MACHINE.item();
    public static final RegistryObject<BlockEntityType<LoadBankBlockEntity>> LOAD_BANK_BLOCK_ENTITY = LOAD_BANK_MACHINE.blockEntity();
    public static final RegistryObject<MenuType<LoadBankMenu>> LOAD_BANK_MENU = LOAD_BANK_MACHINE.menu();

    // The crusher: a shredder that grinds ores into doubled raw metal, powered by Ergs
    public static final Machine<CrusherBlockEntity, CrusherMenu> CRUSHER_MACHINE =
        registerMachine("crusher", CrusherBlock::new,
            java.util.function.UnaryOperator.identity(),
            CrusherBlockEntity::new, CrusherMenu::new);
    public static final RegistryObject<Block> CRUSHER = CRUSHER_MACHINE.block();
    public static final RegistryObject<Item> CRUSHER_ITEM = CRUSHER_MACHINE.item();
    public static final RegistryObject<BlockEntityType<CrusherBlockEntity>> CRUSHER_BLOCK_ENTITY = CRUSHER_MACHINE.blockEntity();
    public static final RegistryObject<MenuType<CrusherMenu>> CRUSHER_MENU = CRUSHER_MACHINE.menu();
    // The grinding racket the crusher makes while its rollers spin
    public static final RegistryObject<SoundEvent> CRUSHER_LOOP = SOUNDS.register("crusher_loop",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "crusher_loop")));

    // The electric furnace: vanilla smelting on Erg power — 2x speed, 2x the
    // smelts per coal once the coal has been through a generator
    public static final Machine<ElectricFurnaceBlockEntity, ElectricFurnaceMenu> ELECTRIC_FURNACE_MACHINE =
        registerMachine("electric_furnace", ElectricFurnaceBlock::new,
            p -> p.lightLevel(state -> state.getValue(ElectricFurnaceBlock.LIT) ? 10 : 0),
            ElectricFurnaceBlockEntity::new, ElectricFurnaceMenu::new);
    public static final RegistryObject<Block> ELECTRIC_FURNACE = ELECTRIC_FURNACE_MACHINE.block();
    public static final RegistryObject<Item> ELECTRIC_FURNACE_ITEM = ELECTRIC_FURNACE_MACHINE.item();
    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BLOCK_ENTITY = ELECTRIC_FURNACE_MACHINE.blockEntity();
    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU = ELECTRIC_FURNACE_MACHINE.menu();

    // The Erg cable: moves energy between machines, connecting on all six sides
    public static final RegistryObject<Block> ERG_CABLE = BLOCKS.register("erg_cable",
        () -> new ErgCableBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("erg_cable"))
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.0F)
            .noOcclusion()
        )
    );
    public static final RegistryObject<Item> ERG_CABLE_ITEM = ITEMS.register("erg_cable",
        () -> new BlockItem(ERG_CABLE.get(), new Item.Properties().setId(ITEMS.key("erg_cable")).useBlockDescriptionPrefix())
    );
    public static final RegistryObject<BlockEntityType<ErgCableBlockEntity>> ERG_CABLE_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("erg_cable",
            () -> new BlockEntityType<>(ErgCableBlockEntity::new, java.util.Set.of(ERG_CABLE.get())));

    // Ash Block: nine ash pressed into a solid block — storage, and the Erg
    // Jar's electrolyte bed. Ash is the old-world source of potash lye, the
    // same alkaline chemistry as a modern KOH battery; dry ash itself is an
    // insulator (the carbon burned away as CO2 — that was the point)
    public static final RegistryObject<Block> ASH_BLOCK = BLOCKS.register("ash_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("ash_block"))
            .mapColor(MapColor.COLOR_GRAY)
            .sound(SoundType.SAND)
            .strength(0.5F)
        )
    );
    public static final RegistryObject<Item> ASH_BLOCK_ITEM = ITEMS.register("ash_block",
        () -> new BlockItem(ASH_BLOCK.get(), new Item.Properties().setId(ITEMS.key("ash_block")).useBlockDescriptionPrefix())
    );

    // The Erg Jar: Automati's battery — 320,000 E (five lumps of coal), 80 E/t
    // in and out. Iron electrode accepts, copper terminal opposite discharges;
    // the wrench moves the terminal. Charge pips glow brighter as it fills.
    public static final Machine<ErgJarBlockEntity, ErgJarMenu> ERG_JAR_MACHINE =
        registerMachine("erg_jar", ErgJarBlock::new,
            p -> p.lightLevel(state -> state.getValue(ErgJarBlock.CHARGE) * 2),
            ErgJarBlockEntity::new, ErgJarMenu::new);
    public static final RegistryObject<Block> ERG_JAR = ERG_JAR_MACHINE.block();
    public static final RegistryObject<Item> ERG_JAR_ITEM = ERG_JAR_MACHINE.item();
    public static final RegistryObject<BlockEntityType<ErgJarBlockEntity>> ERG_JAR_BLOCK_ENTITY = ERG_JAR_MACHINE.blockEntity();
    public static final RegistryObject<MenuType<ErgJarMenu>> ERG_JAR_MENU = ERG_JAR_MACHINE.menu();

    // The item duct: six-direction item transport — hoppers extract, ducts move
    public static final RegistryObject<Block> ITEM_DUCT = BLOCKS.register("item_duct",
        () -> new ItemDuctBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("item_duct"))
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.0F)
            .noOcclusion()
        )
    );
    public static final RegistryObject<Item> ITEM_DUCT_ITEM = ITEMS.register("item_duct",
        () -> new BlockItem(ITEM_DUCT.get(), new Item.Properties().setId(ITEMS.key("item_duct")).useBlockDescriptionPrefix())
    );
    public static final RegistryObject<BlockEntityType<ItemDuctBlockEntity>> ITEM_DUCT_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("item_duct",
            () -> new BlockEntityType<>(ItemDuctBlockEntity::new, java.util.Set.of(ITEM_DUCT.get())));

    // The Automati creative tab: named, iconed with the coal generator, placed after the combat tab
    public static final RegistryObject<CreativeModeTab> AUTOMATI_TAB = CREATIVE_MODE_TABS.register("automati_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.automati"))
            .icon(() -> COAL_GENERATOR_ITEM.get().getDefaultInstance())
            .displayItems((_, output) -> {
                output.accept(BUCKING_IRON.get());
                output.accept(IRON_STICK.get());
                output.accept(FACTORY_BLOCK_ITEM.get());
                output.accept(COAL_GENERATOR_ITEM.get());
                output.accept(CRUSHER_ITEM.get());
                output.accept(ELECTRIC_FURNACE_ITEM.get());
                output.accept(LOAD_BANK_ITEM.get());
                output.accept(ERG_JAR_ITEM.get());
                output.accept(ERG_CABLE_ITEM.get());
                output.accept(ITEM_DUCT_ITEM.get());
                output.accept(ENGINEERS_GOGGLES.get());
                output.accept(ENGINEERS_WRENCH.get());
                output.accept(ASH.get());
                output.accept(ASH_BLOCK_ITEM.get());
                output.accept(IRON_DUST.get());
                output.accept(COPPER_DUST.get());
                output.accept(GOLD_DUST.get());
            }).build());

    public Automati(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        // Data generation (./gradlew runData) — see AutomatiDatagen
        net.minecraftforge.data.event.GatherDataEvent.getBus(modBusGroup).addListener(AutomatiDatagen::gatherData);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modBusGroup);
        // Register the Deferred Registers for block entities, menus, sounds and recipes
        BLOCK_ENTITIES.register(modBusGroup);
        MENUS.register(modBusGroup);
        SOUNDS.register(modBusGroup);
        RECIPE_TYPES.register(modBusGroup);
        RECIPE_SERIALIZERS.register(modBusGroup);
        RECIPE_BOOK_CATEGORIES.register(modBusGroup);

        // Register the item to a creative tab
        BuildCreativeModeTabContentsEvent.BUS.addListener(Automati::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Automati common setup complete");
    }

    // Add the factory block item to the building blocks tab
    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(FACTORY_BLOCK_ITEM);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onAddGuiOverlayLayers(net.minecraftforge.client.event.AddGuiOverlayLayersEvent event) {
            // Register the goggles readout as a HUD layer
            event.getLayeredDraw().add(Identifier.fromNamespaceAndPath(MODID, "goggles_hud"), new GogglesHudLayer());
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Automati client setup complete");

            // Tie the machine menus to their screens on the client
            event.enqueueWork(() -> {
                MenuScreens.register(COAL_GENERATOR_MENU.get(), CoalGeneratorScreen::new);
                MenuScreens.register(LOAD_BANK_MENU.get(), LoadBankScreen::new);
                MenuScreens.register(CRUSHER_MENU.get(), CrusherScreen::new);
                MenuScreens.register(ELECTRIC_FURNACE_MENU.get(), ElectricFurnaceScreen::new);
                MenuScreens.register(ERG_JAR_MENU.get(), ErgJarScreen::new);
            });
        }
    }
}
