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
    public static final RegistryObject<Block> COAL_GENERATOR = BLOCKS.register("coal_generator",
        () -> new CoalGeneratorBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("coal_generator"))
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(5.0F, 6.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(CoalGeneratorBlock.LIT) ? 13 : 0)
        )
    );
    public static final RegistryObject<Item> COAL_GENERATOR_ITEM = ITEMS.register("coal_generator",
        () -> new BlockItem(COAL_GENERATOR.get(), new Item.Properties().setId(ITEMS.key("coal_generator")).useBlockDescriptionPrefix())
    );
    // The block entity type ties the generator block to its ticking logic
    public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("coal_generator",
            () -> new BlockEntityType<>(CoalGeneratorBlockEntity::new, java.util.Set.of(COAL_GENERATOR.get())));
    // The menu type lets the server open the generator GUI on the client
    public static final RegistryObject<MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
        MENUS.register("coal_generator",
            () -> IForgeMenuType.create(CoalGeneratorMenu::new));

    // The load bank: a configurable energy sink for testing generators under load
    public static final RegistryObject<Block> LOAD_BANK = BLOCKS.register("load_bank",
        () -> new LoadBankBlock(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("load_bank"))
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(5.0F, 6.0F)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> state.getValue(LoadBankBlock.LIT) ? 7 : 0)
        )
    );
    public static final RegistryObject<Item> LOAD_BANK_ITEM = ITEMS.register("load_bank",
        () -> new BlockItem(LOAD_BANK.get(), new Item.Properties().setId(ITEMS.key("load_bank")).useBlockDescriptionPrefix())
    );
    public static final RegistryObject<BlockEntityType<LoadBankBlockEntity>> LOAD_BANK_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("load_bank",
            () -> new BlockEntityType<>(LoadBankBlockEntity::new, java.util.Set.of(LOAD_BANK.get())));
    public static final RegistryObject<MenuType<LoadBankMenu>> LOAD_BANK_MENU =
        MENUS.register("load_bank",
            () -> IForgeMenuType.create(LoadBankMenu::new));

    // The Automati creative tab: named, iconed with the coal generator, placed after the combat tab
    public static final RegistryObject<CreativeModeTab> AUTOMATI_TAB = CREATIVE_MODE_TABS.register("automati_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.automati"))
            .icon(() -> COAL_GENERATOR_ITEM.get().getDefaultInstance())
            .displayItems((_, output) -> {
                output.accept(FACTORY_BLOCK_ITEM.get());
                output.accept(COAL_GENERATOR_ITEM.get());
                output.accept(LOAD_BANK_ITEM.get());
                output.accept(ASH.get());
            }).build());

    public Automati(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modBusGroup);
        // Register the Deferred Registers for block entities, menus and sounds
        BLOCK_ENTITIES.register(modBusGroup);
        MENUS.register(modBusGroup);
        SOUNDS.register(modBusGroup);

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
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Automati client setup complete");

            // Tie the machine menus to their screens on the client
            event.enqueueWork(() -> {
                MenuScreens.register(COAL_GENERATOR_MENU.get(), CoalGeneratorScreen::new);
                MenuScreens.register(LOAD_BANK_MENU.get(), LoadBankScreen::new);
            });
        }
    }
}
