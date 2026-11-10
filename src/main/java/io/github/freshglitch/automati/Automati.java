package io.github.freshglitch.automati;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

// The registration hub: every block, item, block entity, menu, sound and
// recipe type of the mod is declared here.
@Mod(Automati.MODID)
public final class Automati {
    public static final String MODID = "automati";
    private static final Logger LOGGER = LogUtils.getLogger();

    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    private static final DeferredRegister<RecipeBookCategory> RECIPE_BOOK_CATEGORIES = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, MODID);

    // Shared block properties: machines and casing, and the two conduits
    private static final UnaryOperator<BlockBehaviour.Properties> CASING = p -> p
        .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5.0F, 6.0F).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> CONDUIT = p -> p
        .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.0F).noOcclusion();

    // ---- Sounds, recipe types, damage ----

    public static final RegistryObject<SoundEvent> COAL_GENERATOR_LOOP = sound("coal_generator_loop");
    public static final RegistryObject<SoundEvent> COAL_GENERATOR_CLOG = sound("coal_generator_clog");
    public static final RegistryObject<SoundEvent> CRUSHER_LOOP = sound("crusher_loop");

    // Crushing recipes are data: data/automati/recipe/crushing/
    public static final RegistryObject<RecipeType<CrusherRecipe>> CRUSHING_RECIPE_TYPE =
        RECIPE_TYPES.register("crushing", () -> RecipeType.simple(id("crushing")));
    public static final RegistryObject<RecipeSerializer<CrusherRecipe>> CRUSHING_SERIALIZER =
        RECIPE_SERIALIZERS.register("crushing", () -> new RecipeSerializer<>(CrusherRecipe.MAP_CODEC, CrusherRecipe.STREAM_CODEC));
    public static final RegistryObject<RecipeBookCategory> CRUSHING_CATEGORY =
        RECIPE_BOOK_CATEGORIES.register("crushing", RecipeBookCategory::new);

    // Dealt by the crusher's blades; defined in data/automati/damage_type/
    public static final ResourceKey<DamageType> CRUSHER_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("crusher"));

    // ---- Tools and materials ----

    public static final RegistryObject<Item> BUCKING_IRON = item("bucking_iron", p -> new BuckingIronItem(p.durability(192)));
    public static final RegistryObject<Item> IRON_STICK = item("iron_stick");
    public static final RegistryObject<Item> ENGINEERS_WRENCH = item("engineers_wrench", p -> new WrenchItem(p.stacksTo(1)));
    // Worn on the head; GogglesHudLayer draws what they reveal
    public static final RegistryObject<Item> ENGINEERS_GOGGLES = item("engineers_goggles", p -> new Item(p
        .stacksTo(1)
        .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
            .setCameraOverlay(id("misc/goggles_blur"))
            .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, id("goggles")))
            .build())));

    // Crusher output, smelted into ingots
    public static final RegistryObject<Item> IRON_DUST = item("iron_dust");
    public static final RegistryObject<Item> COPPER_DUST = item("copper_dust");
    public static final RegistryObject<Item> GOLD_DUST = item("gold_dust");

    // The ash chain. The coal generator's waste takes different roads at
    // different temperatures: a furnace cooks it into SCF, a blast furnace
    // sinters it into pellets. Washing strips its toxic traces for farm use.
    public static final RegistryObject<Item> ASH = item("ash");
    public static final RegistryObject<Item> WASHED_ASH = item("washed_ash");
    public static final RegistryObject<Item> SCF = item("scf");
    // A bone meal item, so it behaves exactly like bone meal
    public static final RegistryObject<Item> INDUSTRIAL_FERTILIZER = item("industrial_fertilizer", BoneMealItem::new);
    public static final RegistryObject<Item> ASH_CLAY_BLEND = item("ash_clay_blend");
    public static final RegistryObject<Item> ASH_BRICK = item("ash_brick");
    public static final RegistryObject<Item> SINTERED_ASH_PELLET = item("sintered_ash_pellet");

    // ---- Machines and conduits ----

    public static final RegistryObject<Block> FACTORY_BLOCK = block("factory_block", FactoryBlock::new, CASING);

    public static final Machine<CoalGeneratorBlockEntity, CoalGeneratorMenu> COAL_GENERATOR = machine("coal_generator",
        CoalGeneratorBlock::new, glowWhenLit(13), CoalGeneratorBlockEntity::new, CoalGeneratorMenu::new);
    public static final Machine<LoadBankBlockEntity, LoadBankMenu> LOAD_BANK = machine("load_bank",
        LoadBankBlock::new, glowWhenLit(7), LoadBankBlockEntity::new, LoadBankMenu::new);
    public static final Machine<CrusherBlockEntity, CrusherMenu> CRUSHER = machine("crusher",
        CrusherBlock::new, UnaryOperator.identity(), CrusherBlockEntity::new, CrusherMenu::new);
    public static final Machine<ElectricFurnaceBlockEntity, ElectricFurnaceMenu> ELECTRIC_FURNACE = machine("electric_furnace",
        ElectricFurnaceBlock::new, glowWhenLit(10), ElectricFurnaceBlockEntity::new, ElectricFurnaceMenu::new);
    // The charge pips glow brighter as the jar fills
    public static final Machine<ErgJarBlockEntity, ErgJarMenu> ERG_JAR = machine("erg_jar",
        ErgJarBlock::new, p -> p.lightLevel(state -> state.getValue(ErgJarBlock.CHARGE) * 2),
        ErgJarBlockEntity::new, ErgJarMenu::new);

    public static final RegistryObject<Block> ERG_CABLE = block("erg_cable", ErgCableBlock::new, CONDUIT);
    public static final RegistryObject<BlockEntityType<ErgCableBlockEntity>> ERG_CABLE_BLOCK_ENTITY =
        blockEntity("erg_cable", ErgCableBlockEntity::new, ERG_CABLE);
    public static final RegistryObject<Block> ITEM_DUCT = block("item_duct", ItemDuctBlock::new, CONDUIT);
    public static final RegistryObject<BlockEntityType<ItemDuctBlockEntity>> ITEM_DUCT_BLOCK_ENTITY =
        blockEntity("item_duct", ItemDuctBlockEntity::new, ITEM_DUCT);

    // ---- Ash blocks ----

    // Storage, and the Erg Jar's electrolyte bed: ash is the old source of
    // potash lye. Dry ash itself is an insulator.
    public static final RegistryObject<Block> ASH_BLOCK = block("ash_block", Block::new,
        p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.SAND).strength(0.5F));
    // Fired ash-clay masonry; blast resistance 90 shrugs off creepers and TNT
    public static final RegistryObject<Block> ASH_BRICKS = block("ash_bricks", Block::new,
        p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.STONE).strength(2.0F, 90.0F).requiresCorrectToolForDrops(),
        tooltip("ash_bricks.blast", ChatFormatting.GRAY));
    // Ash-stabilized gravel: it does not fall, and walking on it is 15% faster
    public static final RegistryObject<Block> ROAD_BASE = block("road_base", Block::new,
        p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.GRAVEL).strength(0.6F).speedFactor(1.15F));
    // Vitrified sintered ash: obsidian-grade blast resistance, and tagged
    // wither_immune so the wither cannot break it either
    public static final RegistryObject<Block> FLY_ASH_GLASS = block("fly_ash_glass", TransparentBlock::new,
        p -> p.mapColor(MapColor.COLOR_BLACK).sound(SoundType.GLASS).strength(3.0F, 1200.0F)
            .requiresCorrectToolForDrops().noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false),
        tooltip("fly_ash_glass.blast", ChatFormatting.GRAY),
        tooltip("fly_ash_glass.wither", ChatFormatting.DARK_PURPLE));
    public static final RegistryObject<Block> ASH_RICH_SOIL = block("ash_rich_soil", AshRichSoilBlock::new,
        p -> p.mapColor(MapColor.DIRT).sound(SoundType.GRAVEL).strength(0.6F).randomTicks());

    public static final RegistryObject<CreativeModeTab> AUTOMATI_TAB = CREATIVE_MODE_TABS.register("automati_tab",
        () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.automati"))
            .icon(() -> new ItemStack(COAL_GENERATOR.block().get()))
            .displayItems((_, output) -> tabContents().forEach(entry -> output.accept(entry.get())))
            .build());

    private static Stream<Supplier<? extends ItemLike>> tabContents() {
        return Stream.of(
            BUCKING_IRON, IRON_STICK, FACTORY_BLOCK,
            COAL_GENERATOR.block(), CRUSHER.block(), ELECTRIC_FURNACE.block(), LOAD_BANK.block(), ERG_JAR.block(),
            ERG_CABLE, ITEM_DUCT, ENGINEERS_GOGGLES, ENGINEERS_WRENCH,
            ASH, ASH_BLOCK, WASHED_ASH, SCF, INDUSTRIAL_FERTILIZER, ASH_CLAY_BLEND, ASH_BRICK, SINTERED_ASH_PELLET,
            ASH_BRICKS, ROAD_BASE, FLY_ASH_GLASS, ASH_RICH_SOIL,
            IRON_DUST, COPPER_DUST, GOLD_DUST);
    }

    // The registry entries every machine has. Its block item is registered
    // under the same name.
    public record Machine<BE extends BlockEntity, M extends AbstractContainerMenu>(
        RegistryObject<Block> block,
        RegistryObject<BlockEntityType<BE>> blockEntity,
        RegistryObject<MenuType<M>> menu) {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    static Component tooltip(String key, ChatFormatting color) {
        return Component.translatable("tooltip.automati." + key).withStyle(color);
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }

    private static RegistryObject<Item> item(String name) {
        return item(name, Item::new);
    }

    private static RegistryObject<Item> item(String name, Function<Item.Properties, ? extends Item> factory) {
        return ITEMS.register(name, () -> factory.apply(new Item.Properties().setId(ITEMS.key(name))));
    }

    // Registers a block together with its block item. Tooltip lines, if any,
    // show under the item's name.
    private static RegistryObject<Block> block(String name, Function<BlockBehaviour.Properties, ? extends Block> factory,
                                               UnaryOperator<BlockBehaviour.Properties> properties, Component... tooltip) {
        RegistryObject<Block> block = BLOCKS.register(name,
            () -> factory.apply(properties.apply(BlockBehaviour.Properties.of().setId(BLOCKS.key(name)))));
        ITEMS.register(name, () -> {
            Item.Properties itemProperties = new Item.Properties().setId(ITEMS.key(name)).useBlockDescriptionPrefix();
            return tooltip.length == 0
                ? new BlockItem(block.get(), itemProperties)
                : new TooltipBlockItem(block.get(), itemProperties, tooltip);
        });
        return block;
    }

    private static <BE extends BlockEntity> RegistryObject<BlockEntityType<BE>> blockEntity(
            String name, BlockEntityType.BlockEntitySupplier<BE> factory, RegistryObject<Block> block) {
        return BLOCK_ENTITIES.register(name, () -> new BlockEntityType<>(factory, Set.of(block.get())));
    }

    // One call per machine: block and item with the casing properties plus
    // the machine's own, block entity type and menu type
    private static <BE extends BlockEntity, M extends AbstractContainerMenu> Machine<BE, M> machine(
            String name, Function<BlockBehaviour.Properties, ? extends Block> blockFactory,
            UnaryOperator<BlockBehaviour.Properties> properties,
            BlockEntityType.BlockEntitySupplier<BE> blockEntityFactory, IContainerFactory<M> menuFactory) {
        RegistryObject<Block> block = block(name, blockFactory, p -> properties.apply(CASING.apply(p)));
        return new Machine<>(block, blockEntity(name, blockEntityFactory, block),
            MENUS.register(name, () -> IForgeMenuType.create(menuFactory)));
    }

    private static UnaryOperator<BlockBehaviour.Properties> glowWhenLit(int light) {
        return p -> p.lightLevel(state -> state.getValue(AbstractMachineBlock.LIT) ? light : 0);
    }

    public Automati(FMLJavaModLoadingContext context) {
        var modBus = context.getModBusGroup();
        Stream.of(BLOCKS, ITEMS, CREATIVE_MODE_TABS, BLOCK_ENTITIES, MENUS, SOUNDS,
                RECIPE_TYPES, RECIPE_SERIALIZERS, RECIPE_BOOK_CATEGORIES)
            .forEach(register -> register.register(modBus));

        FMLCommonSetupEvent.getBus(modBus).addListener(event -> LOGGER.info("Automati common setup complete"));
        GatherDataEvent.getBus(modBus).addListener(AutomatiDatagen::gatherData);
        BuildCreativeModeTabContentsEvent.BUS.addListener(Automati::addCreative);
        BlockEvent.BreakEvent.BUS.addListener(AshRichSoilBlock::onCropHarvest);
    }

    // The casing also sits in vanilla's building blocks tab
    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(FACTORY_BLOCK);
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onAddGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
            event.getLayeredDraw().add(id("goggles_hud"), new GogglesHudLayer());
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Automati client setup complete");
            event.enqueueWork(() -> {
                MenuScreens.register(COAL_GENERATOR.menu().get(), CoalGeneratorScreen::new);
                MenuScreens.register(LOAD_BANK.menu().get(), LoadBankScreen::new);
                MenuScreens.register(CRUSHER.menu().get(), CrusherScreen::new);
                MenuScreens.register(ELECTRIC_FURNACE.menu().get(), ElectricFurnaceScreen::new);
                MenuScreens.register(ERG_JAR.menu().get(), ErgJarScreen::new);
            });
        }
    }
}
