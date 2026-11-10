package io.github.freshglitch.automati;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

// Data generation: ./gradlew runData writes the mechanical JSONs (loot tables,
// block tags, vanilla-type recipes, simple models) into
// src/generated/resources. Hand-authored files stay in src/main/resources:
// crushing recipes, the older machine models, the wrench model, textures.
// Never edit generated files; the next runData overwrites them.
public final class AutomatiDatagen {

    private AutomatiDatagen() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
            List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        generator.addProvider(event.includeServer(), new BlockTagsGen(output, lookup));
        generator.addProvider(event.includeServer(), new Recipes.Runner(output, lookup));
        generator.addProvider(event.includeClient(), new Models(output));
    }

    // Blockstates, models and item definitions for the blocks and items whose
    // JSON is pure mechanics. The older machines, the factory block and the
    // conduits keep their hand-written files, as do fly_ash_glass (needs a
    // render_type) and ash_rich_soil (uses the farmland template). New blocks
    // belong here: the Erg Jar is the multi-state example, the cubes the
    // trivial one.
    static final class Models extends ModelProvider {
        private static final List<RegistryObject<Block>> CUBES =
            List.of(Automati.ASH_BLOCK, Automati.ASH_BRICKS, Automati.ROAD_BASE);
        private static final List<RegistryObject<Item>> FLAT_ITEMS = List.of(
            Automati.SCF, Automati.WASHED_ASH, Automati.INDUSTRIAL_FERTILIZER,
            Automati.ASH_CLAY_BLEND, Automati.ASH_BRICK, Automati.SINTERED_ASH_PELLET);

        Models(PackOutput output) {
            super(output);
        }

        @Override
        protected Stream<Block> getKnownBlocks() {
            return Stream.concat(Stream.of(Automati.ERG_JAR.block().get()), CUBES.stream().map(RegistryObject::get));
        }

        @Override
        protected Stream<Item> getKnownItems() {
            return Stream.concat(getKnownBlocks().map(Block::asItem), FLAT_ITEMS.stream().map(RegistryObject::get));
        }

        @Override
        protected BlockModelGenerators getBlockModelGenerators(BlockStateGeneratorCollector blockStates, ItemInfoCollector items, SimpleModelCollector models) {
            return new BlockModels(blockStates, items, models);
        }

        // Vanilla's generator would emit a definition for every vanilla item,
        // so run() is replaced to emit only Automati's flat sprites. The
        // collector still backfills block-item definitions registry-wide;
        // the runData prune in build.gradle deletes that overreach.
        @Override
        protected ItemModelGenerators getItemModelGenerators(ItemInfoCollector items, SimpleModelCollector models) {
            return new ItemModelGenerators(items, models) {
                @Override
                public void run() {
                    for (RegistryObject<Item> item : FLAT_ITEMS)
                        generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM);
                }
            };
        }

        static final class BlockModels extends BlockModelGenerators {
            BlockModels(BlockStateGeneratorCollector blockStates, ItemInfoCollector items, SimpleModelCollector models) {
                super(blockStates, items, models);
            }

            @Override
            public void run() {
                // Item definitions are written explicitly: the block-item
                // backfill is unreliable with vanilla's generators muted
                for (RegistryObject<Block> cube : CUBES) {
                    createTrivialCube(cube.get());
                    itemModelOutput.accept(cube.get().asItem(),
                        ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(cube.get())));
                }

                // Erg Jar: one model per charge level, built terminal-up and
                // rotated by FACING like a barrel, so the pips always fill
                // toward the terminal. The item shows the empty jar.
                Block jar = Automati.ERG_JAR.block().get();
                blockStateOutput.accept(MultiVariantGenerator.dispatch(jar)
                    .with(PropertyDispatch.initial(ErgJarBlock.CHARGE)
                        .generate(charge -> plainVariant(jarModel(jar, charge))))
                    .with(PropertyDispatch.modify(ErgJarBlock.FACING)
                        .select(Direction.UP, NOP)
                        .select(Direction.DOWN, X_ROT_180)
                        .select(Direction.NORTH, X_ROT_90)
                        .select(Direction.SOUTH, X_ROT_90.then(Y_ROT_180))
                        .select(Direction.EAST, X_ROT_90.then(Y_ROT_90))
                        .select(Direction.WEST, X_ROT_90.then(Y_ROT_270))));
                itemModelOutput.accept(jar.asItem(),
                    ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(jar, "_0")));
            }

            private Identifier jarModel(Block jar, int charge) {
                return ModelTemplates.CUBE_BOTTOM_TOP.createWithSuffix(jar, "_" + charge, new TextureMapping()
                    .put(TextureSlot.TOP, TextureMapping.getBlockTexture(jar, "_terminal"))
                    .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(jar, "_electrode"))
                    .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(jar, "_side_" + charge)), modelOutput);
            }
        }
    }

    // Every Automati block drops itself. Machine contents spill through
    // preRemoveSideEffects, and stored energy is lost.
    static final class BlockLoot extends BlockLootSubProvider {
        BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            getKnownBlocks().forEach(this::dropSelf);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Automati.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
        }
    }

    // Appends to vanilla's block tags: tool, tier and wither immunity
    static final class BlockTagsGen extends TagsProvider<Block> {
        BlockTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, Registries.BLOCK, lookup);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).addAll(keys(
                Automati.FACTORY_BLOCK, Automati.COAL_GENERATOR.block(), Automati.LOAD_BANK.block(),
                Automati.CRUSHER.block(), Automati.ERG_CABLE, Automati.ELECTRIC_FURNACE.block(),
                Automati.ITEM_DUCT, Automati.ERG_JAR.block(), Automati.ASH_BRICKS, Automati.FLY_ASH_GLASS));
            tag(BlockTags.MINEABLE_WITH_SHOVEL).addAll(keys(
                Automati.ASH_BLOCK, Automati.ROAD_BASE, Automati.ASH_RICH_SOIL));
            tag(BlockTags.NEEDS_STONE_TOOL).addAll(keys(
                Automati.FACTORY_BLOCK, Automati.COAL_GENERATOR.block(), Automati.LOAD_BANK.block(),
                Automati.CRUSHER.block(), Automati.ELECTRIC_FURNACE.block(), Automati.ERG_JAR.block()));
            tag(BlockTags.WITHER_IMMUNE).addAll(keys(Automati.FLY_ASH_GLASS));
            tag(BlockTags.NEEDS_DIAMOND_TOOL).addAll(keys(Automati.ASH_BRICKS, Automati.FLY_ASH_GLASS));
        }

        @SafeVarargs
        private static List<ResourceKey<Block>> keys(RegistryObject<Block>... blocks) {
            return Stream.of(blocks).map(block -> ResourceKey.create(Registries.BLOCK, block.getId())).toList();
        }
    }

    // Vanilla-type recipes, each with its recipe-book unlock advancement.
    // Crushing recipes are not here: data/automati/recipe/crushing/ stays
    // hand-authored so that adding one never needs Java.
    static final class Recipes extends RecipeProvider {

        Recipes(HolderLookup.Provider registries, RecipeOutput output) {
            super(registries, output);
        }

        static final class Runner extends RecipeProvider.Runner {
            Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
                super(output, lookup);
            }

            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
                return new Recipes(registries, output);
            }

            @Override
            public String getName() {
                return "Automati recipes";
            }
        }

        @Override
        protected void buildRecipes() {
            Item ash = Automati.ASH.get();
            Item ashBrick = Automati.ASH_BRICK.get();
            Item blend = Automati.ASH_CLAY_BLEND.get();
            Item ironStick = Automati.IRON_STICK.get();
            Item pellet = Automati.SINTERED_ASH_PELLET.get();
            Item scf = Automati.SCF.get();
            Item washedAsh = Automati.WASHED_ASH.get();
            Block ashBlock = Automati.ASH_BLOCK.get();
            Block casing = Automati.FACTORY_BLOCK.get();

            // Materials
            add(shaped(RecipeCategory.BUILDING_BLOCKS, casing, 2)
                .pattern("ISI").pattern("S S").pattern("ISI")
                .define('I', Items.IRON_INGOT).define('S', Items.SMOOTH_STONE), Items.IRON_INGOT);
            add(shaped(RecipeCategory.MISC, ironStick, 4)
                .pattern("I").pattern("I")
                .define('I', Items.IRON_INGOT), Items.IRON_INGOT);
            add(shaped(RecipeCategory.BUILDING_BLOCKS, ashBlock)
                .pattern("AAA").pattern("AAA").pattern("AAA")
                .define('A', ash), ash);
            add(shapeless(RecipeCategory.MISC, ash, 9).requires(ashBlock), ashBlock, "ash_from_ash_block");

            // Tools
            add(shaped(RecipeCategory.TOOLS, Automati.BUCKING_IRON.get())
                .pattern("I").pattern("S").pattern("S")
                .define('I', Items.IRON_INGOT).define('S', ironStick), ironStick);
            add(shaped(RecipeCategory.TOOLS, Automati.ENGINEERS_WRENCH.get())
                .pattern("I I").pattern(" CC").pattern(" C ")
                .define('I', Items.IRON_INGOT).define('C', Items.COPPER_INGOT), Items.COPPER_INGOT);
            add(shaped(RecipeCategory.TOOLS, Automati.ENGINEERS_GOGGLES.get())
                .pattern("CCC").pattern("GRG")
                .define('C', Items.COPPER_INGOT).define('G', Items.GLASS_PANE).define('R', Items.REDSTONE),
                Items.COPPER_INGOT);

            // Machines
            add(shaped(RecipeCategory.MISC, Automati.COAL_GENERATOR.block().get())
                .pattern("RCR").pattern("CFC").pattern("RCR")
                .define('C', casing).define('R', Items.REDSTONE).define('F', Items.FURNACE), casing);
            add(shaped(RecipeCategory.MISC, Automati.LOAD_BANK.block().get())
                .pattern("ICI").pattern("CRC").pattern("ICI")
                .define('I', Items.IRON_INGOT).define('C', casing).define('R', Items.REDSTONE), casing);
            add(shaped(RecipeCategory.MISC, Automati.CRUSHER.block().get())
                .pattern("III").pattern("CBC").pattern("CRC")
                .define('I', Items.IRON_INGOT).define('C', casing)
                .define('B', Items.IRON_BLOCK).define('R', Items.REDSTONE), casing);
            add(shaped(RecipeCategory.MISC, Automati.ELECTRIC_FURNACE.block().get())
                .pattern("RCR").pattern("CBC").pattern("RCR")
                .define('C', casing).define('R', Items.REDSTONE).define('B', Items.BLAST_FURNACE), casing);
            add(shaped(RecipeCategory.MISC, Automati.ERG_JAR.block().get())
                .pattern("FCF").pattern("FAF").pattern("FIF")
                .define('F', casing).define('C', Items.COPPER_INGOT)
                .define('A', ashBlock).define('I', Items.IRON_INGOT), ashBlock);

            // Conduits
            add(shaped(RecipeCategory.MISC, Automati.ERG_CABLE.get(), 6)
                .pattern("SSS").pattern("CCC").pattern("SSS")
                .define('S', Items.SMOOTH_STONE).define('C', Items.COPPER_INGOT), Items.COPPER_INGOT);
            add(shaped(RecipeCategory.MISC, Automati.ITEM_DUCT.get(), 6)
                .pattern("SSS").pattern("I I").pattern("SSS")
                .define('S', Items.SMOOTH_STONE).define('I', Items.IRON_INGOT), Items.SMOOTH_STONE);

            // Dusts
            dustCooking(Automati.IRON_DUST.get(), Items.IRON_INGOT);
            dustCooking(Automati.COPPER_DUST.get(), Items.COPPER_INGOT);
            dustCooking(Automati.GOLD_DUST.get(), Items.GOLD_INGOT);

            // The ash chain. Furnace heat makes SCF, sintering heat (blast
            // furnace only) makes pellets.
            add(smelt(ash, scf, 0.1F), ash, "scf_from_smelting_ash");
            add(blast(ash, pellet, 0.1F), ash, "sintered_ash_pellet_from_blasting_ash");
            // Ash extends clay one to one; the blend fires like vanilla brick
            add(shapeless(RecipeCategory.MISC, blend, 2).requires(ash).requires(Items.CLAY_BALL), ash);
            add(smelt(blend, ashBrick, 0.3F), blend, "ash_brick_from_smelting_blend");
            add(shaped(RecipeCategory.BUILDING_BLOCKS, Automati.ASH_BRICKS.get())
                .pattern("BB").pattern("BB")
                .define('B', ashBrick), ashBrick);
            add(shapeless(RecipeCategory.BUILDING_BLOCKS, Automati.ROAD_BASE.get())
                .requires(ash).requires(Items.GRAVEL), ash);
            // Washing and soil making return the empty bucket
            add(shapeless(RecipeCategory.MISC, washedAsh, 4).requires(ash, 4).requires(Items.WATER_BUCKET), ash);
            add(shapeless(RecipeCategory.MISC, Automati.INDUSTRIAL_FERTILIZER.get(), 4)
                .requires(washedAsh).requires(Items.ROTTEN_FLESH), washedAsh, "industrial_fertilizer_from_rotten_flesh");
            add(shapeless(RecipeCategory.MISC, Automati.INDUSTRIAL_FERTILIZER.get(), 6)
                .requires(washedAsh).requires(Items.BONE_MEAL), washedAsh, "industrial_fertilizer_from_bone_meal");
            add(shaped(RecipeCategory.BUILDING_BLOCKS, Automati.FLY_ASH_GLASS.get())
                .pattern("PP").pattern("PP")
                .define('P', pellet), pellet);
            add(shaped(RecipeCategory.BUILDING_BLOCKS, Automati.ASH_RICH_SOIL.get(), 4)
                .pattern("DSD").pattern("SWS").pattern("DSD")
                .define('D', Items.DIRT).define('S', scf).define('W', Items.WATER_BUCKET), scf);
        }

        // Saves a recipe that unlocks once the player holds the trigger item,
        // under its default id (the result's name)
        private void add(RecipeBuilder recipe, ItemLike trigger) {
            recipe.unlockedBy(getHasName(trigger), has(trigger)).save(output);
        }

        // The same under an explicit id, for results with more than one recipe
        private void add(RecipeBuilder recipe, ItemLike trigger, String id) {
            recipe.unlockedBy(getHasName(trigger), has(trigger))
                .save(output, ResourceKey.create(Registries.RECIPE, Automati.id(id)));
        }

        private static SimpleCookingRecipeBuilder smelt(ItemLike input, ItemLike result, float experience) {
            return SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC,
                CookingBookCategory.MISC, result, experience, 200);
        }

        private static SimpleCookingRecipeBuilder blast(ItemLike input, ItemLike result, float experience) {
            return SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC,
                CookingBookCategory.MISC, result, experience, 100);
        }

        // A dust smelts or blasts into its ingot for 0.7 xp
        private void dustCooking(ItemLike dust, ItemLike ingot) {
            String group = getItemName(ingot);
            String dustName = getItemName(dust);
            add(smelt(dust, ingot, 0.7F).group(group), dust, group + "_from_smelting_" + dustName);
            add(blast(dust, ingot, 0.7F).group(group), dust, group + "_from_blasting_" + dustName);
        }
    }
}
