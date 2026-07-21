package io.github.freshglitch.automati;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
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

// Data generation: ./gradlew runData writes the mechanical JSONs (loot tables,
// block tags, vanilla-type recipes) into src/generated/resources. Anything a
// loop can express lives here; anything artistic (the 3D wrench model, GUI
// textures, crushing recipe JSONs — deliberately hand-editable) stays in
// src/main/resources. Never hand-edit generated files: the next runData wins.
public final class AutomatiDatagen {

    private AutomatiDatagen() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
            List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        generator.addProvider(event.includeServer(),
            new BlockTagsGen(output, lookup));
        generator.addProvider(event.includeServer(), new Recipes.Runner(output, lookup));
        generator.addProvider(event.includeClient(), new Models(output));
    }

    // Client models/blockstates for the blocks whose JSON is pure mechanics.
    // The older machines (generator, load bank, crusher, furnace, factory
    // block) and the conduit multiparts keep their stable hand-written files;
    // new blocks should be generated here — the Erg Jar shows the multi-state
    // pattern, the Ash Block the trivial one.
    static final class Models extends net.minecraft.client.data.models.ModelProvider {
        Models(PackOutput output) {
            super(output);
        }

        @Override
        protected java.util.stream.Stream<Block> getKnownBlocks() {
            // fly_ash_glass and ash_rich_soil are hand-authored (they need
            // render_type / the farmland template) and deliberately absent
            return java.util.stream.Stream.of(Automati.ERG_JAR.get(), Automati.ASH_BLOCK.get(),
                Automati.ASH_BRICKS.get(), Automati.ROAD_BASE.get());
        }

        @Override
        protected java.util.stream.Stream<net.minecraft.world.item.Item> getKnownItems() {
            return java.util.stream.Stream.of(Automati.ERG_JAR_ITEM.get(), Automati.ASH_BLOCK_ITEM.get(),
                Automati.ASH_BRICKS_ITEM.get(), Automati.ROAD_BASE_ITEM.get(),
                Automati.SCF.get(), Automati.WASHED_ASH.get(), Automati.INDUSTRIAL_FERTILIZER.get(),
                Automati.ASH_CLAY_BLEND.get(), Automati.ASH_BRICK.get(), Automati.SINTERED_ASH_PELLET.get());
        }

        @Override
        protected net.minecraft.client.data.models.BlockModelGenerators getBlockModelGenerators(
                BlockStateGeneratorCollector blockStates, ItemInfoCollector items, SimpleModelCollector models) {
            return new Gen(blockStates, items, models);
        }

        // Vanilla's ItemModelGenerators would emit definitions for every
        // vanilla item; override run() to generate ONLY Automati's flat item
        // sprites. (The collector still backfills block-item definitions
        // registry-wide — build.gradle's runData prune deletes that overreach
        // after generation.)
        @Override
        protected net.minecraft.client.data.models.ItemModelGenerators getItemModelGenerators(
                ItemInfoCollector items, SimpleModelCollector models) {
            return new net.minecraft.client.data.models.ItemModelGenerators(items, models) {
                @Override
                public void run() {
                    generateFlatItem(Automati.SCF.get(),
                        net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
                    generateFlatItem(Automati.WASHED_ASH.get(),
                        net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
                    generateFlatItem(Automati.INDUSTRIAL_FERTILIZER.get(),
                        net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
                    generateFlatItem(Automati.ASH_CLAY_BLEND.get(),
                        net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
                    generateFlatItem(Automati.ASH_BRICK.get(),
                        net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
                    generateFlatItem(Automati.SINTERED_ASH_PELLET.get(),
                        net.minecraft.client.data.models.model.ModelTemplates.FLAT_ITEM);
                }
            };
        }

        static final class Gen extends net.minecraft.client.data.models.BlockModelGenerators {
            Gen(BlockStateGeneratorCollector blockStates, ItemInfoCollector items, SimpleModelCollector models) {
                super(blockStates, items, models);
            }

            @Override
            public void run() {
                createTrivialCube(Automati.ASH_BLOCK.get());
                createTrivialCube(Automati.ASH_BRICKS.get());
                createTrivialCube(Automati.ROAD_BASE.get());
                // explicit item definitions — the framework's block-item
                // backfill is unreliable when vanilla's generators are muted
                for (var blockWithItem : List.of(Automati.ASH_BLOCK, Automati.ASH_BRICKS, Automati.ROAD_BASE)) {
                    itemModelOutput.accept(blockWithItem.get().asItem(),
                        net.minecraft.client.data.models.model.ItemModelUtils.plainModel(
                            net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation(
                                blockWithItem.get())));
                }

                // Erg Jar: one model per charge level (terminal up), rotated
                // per FACING barrel-style — the pip bar fills toward the
                // terminal in every orientation
                Block jar = Automati.ERG_JAR.get();
                Identifier chargeZeroModel = null;
                var chargeDispatch = net.minecraft.client.data.models.blockstates.PropertyDispatch
                    .initial(ErgJarBlock.CHARGE);
                for (int charge = 0; charge <= 4; charge++) {
                    Identifier model = net.minecraft.client.data.models.model.ModelTemplates.CUBE_BOTTOM_TOP
                        .createWithSuffix(jar, "_" + charge, new net.minecraft.client.data.models.model.TextureMapping()
                            .put(net.minecraft.client.data.models.model.TextureSlot.TOP,
                                net.minecraft.client.data.models.model.TextureMapping.getBlockTexture(jar, "_terminal"))
                            .put(net.minecraft.client.data.models.model.TextureSlot.BOTTOM,
                                net.minecraft.client.data.models.model.TextureMapping.getBlockTexture(jar, "_electrode"))
                            .put(net.minecraft.client.data.models.model.TextureSlot.SIDE,
                                net.minecraft.client.data.models.model.TextureMapping.getBlockTexture(jar, "_side_" + charge)),
                            modelOutput);
                    if (charge == 0)
                        chargeZeroModel = model;
                    chargeDispatch = chargeDispatch.select(charge, plainVariant(model));
                }
                blockStateOutput.accept(net.minecraft.client.data.models.blockstates.MultiVariantGenerator
                    .dispatch(jar)
                    .with(chargeDispatch)
                    .with(net.minecraft.client.data.models.blockstates.PropertyDispatch.modify(ErgJarBlock.FACING)
                        .select(net.minecraft.core.Direction.UP, NOP)
                        .select(net.minecraft.core.Direction.DOWN, X_ROT_180)
                        .select(net.minecraft.core.Direction.NORTH, X_ROT_90)
                        .select(net.minecraft.core.Direction.SOUTH, X_ROT_90.then(Y_ROT_180))
                        .select(net.minecraft.core.Direction.EAST, X_ROT_90.then(Y_ROT_90))
                        .select(net.minecraft.core.Direction.WEST, X_ROT_90.then(Y_ROT_270))));
                // inventory form shows the empty jar
                itemModelOutput.accept(jar.asItem(),
                    net.minecraft.client.data.models.model.ItemModelUtils.plainModel(chargeZeroModel));
            }
        }
    }

    // Every Automati block drops itself (machines keep no inventory on break —
    // contents spill via preRemoveSideEffects, energy is lost, as designed)
    static final class BlockLoot extends BlockLootSubProvider {
        BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            for (Block block : selfDropping())
                dropSelf(block);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return selfDropping();
        }

        private static List<Block> selfDropping() {
            return List.of(
                Automati.FACTORY_BLOCK.get(),
                Automati.COAL_GENERATOR.get(),
                Automati.LOAD_BANK.get(),
                Automati.CRUSHER.get(),
                Automati.ELECTRIC_FURNACE.get(),
                Automati.ERG_CABLE.get(),
                Automati.ITEM_DUCT.get(),
                Automati.ERG_JAR.get(),
                Automati.ASH_BLOCK.get(),
                Automati.ASH_BRICKS.get(),
                Automati.ROAD_BASE.get(),
                Automati.FLY_ASH_GLASS.get(),
                Automati.ASH_RICH_SOIL.get());
        }
    }

    // Vanilla block-tag appends: tool + tier requirements
    static final class BlockTagsGen extends TagsProvider<Block> {
        BlockTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, Registries.BLOCK, lookup);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).addAll(List.of(
                key(Automati.FACTORY_BLOCK), key(Automati.COAL_GENERATOR), key(Automati.LOAD_BANK),
                key(Automati.CRUSHER), key(Automati.ERG_CABLE), key(Automati.ELECTRIC_FURNACE),
                key(Automati.ITEM_DUCT), key(Automati.ERG_JAR),
                key(Automati.ASH_BRICKS), key(Automati.FLY_ASH_GLASS)));
            tag(BlockTags.MINEABLE_WITH_SHOVEL).addAll(List.of(
                key(Automati.ASH_BLOCK), key(Automati.ROAD_BASE), key(Automati.ASH_RICH_SOIL)));
            tag(BlockTags.NEEDS_STONE_TOOL).addAll(List.of(
                key(Automati.FACTORY_BLOCK), key(Automati.COAL_GENERATOR), key(Automati.LOAD_BANK),
                key(Automati.CRUSHER), key(Automati.ELECTRIC_FURNACE), key(Automati.ERG_JAR)));
            // the whole point of fly-ash glass: the wither can't chew through it
            tag(BlockTags.WITHER_IMMUNE).add(key(Automati.FLY_ASH_GLASS));
            // dense industrial ceramics: nothing below diamond bites into them
            tag(BlockTags.NEEDS_DIAMOND_TOOL).addAll(List.of(
                key(Automati.ASH_BRICKS), key(Automati.FLY_ASH_GLASS)));
        }

        private static ResourceKey<Block> key(RegistryObject<Block> block) {
            return ResourceKey.create(Registries.BLOCK, block.getId());
        }
    }

    // Vanilla-type recipes, now with free recipe-book unlock advancements.
    // Crushing recipes are NOT here: data/automati/recipe/crushing/ stays
    // hand-authored JSON on purpose (adding one must never require Java).
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
            // -- materials --
            shaped(RecipeCategory.BUILDING_BLOCKS, Automati.FACTORY_BLOCK_ITEM.get(), 2)
                .pattern("ISI").pattern("S S").pattern("ISI")
                .define('I', Items.IRON_INGOT).define('S', Items.SMOOTH_STONE)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
            shaped(RecipeCategory.MISC, Automati.IRON_STICK.get(), 4)
                .pattern("I").pattern("I")
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(output);
            shaped(RecipeCategory.BUILDING_BLOCKS, Automati.ASH_BLOCK_ITEM.get())
                .pattern("AAA").pattern("AAA").pattern("AAA")
                .define('A', Automati.ASH.get())
                .unlockedBy("has_ash", has(Automati.ASH.get()))
                .save(output);
            shapeless(RecipeCategory.MISC, Automati.ASH.get(), 9)
                .requires(Automati.ASH_BLOCK_ITEM.get())
                .unlockedBy("has_ash_block", has(Automati.ASH_BLOCK_ITEM.get()))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, "ash_from_ash_block")));

            // -- tools --
            shaped(RecipeCategory.TOOLS, Automati.BUCKING_IRON.get())
                .pattern("I").pattern("S").pattern("S")
                .define('I', Items.IRON_INGOT).define('S', Automati.IRON_STICK.get())
                .unlockedBy("has_iron_stick", has(Automati.IRON_STICK.get()))
                .save(output);
            shaped(RecipeCategory.TOOLS, Automati.ENGINEERS_WRENCH.get())
                .pattern("I I").pattern(" CC").pattern(" C ")
                .define('I', Items.IRON_INGOT).define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);
            shaped(RecipeCategory.TOOLS, Automati.ENGINEERS_GOGGLES.get())
                .pattern("CCC").pattern("GRG")
                .define('C', Items.COPPER_INGOT).define('G', Items.GLASS_PANE).define('R', Items.REDSTONE)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);

            // -- machines --
            shaped(RecipeCategory.MISC, Automati.COAL_GENERATOR_ITEM.get())
                .pattern("RCR").pattern("CFC").pattern("RCR")
                .define('C', Automati.FACTORY_BLOCK_ITEM.get()).define('R', Items.REDSTONE).define('F', Items.FURNACE)
                .unlockedBy("has_factory_block", has(Automati.FACTORY_BLOCK_ITEM.get()))
                .save(output);
            shaped(RecipeCategory.MISC, Automati.LOAD_BANK_ITEM.get())
                .pattern("ICI").pattern("CRC").pattern("ICI")
                .define('I', Items.IRON_INGOT).define('C', Automati.FACTORY_BLOCK_ITEM.get()).define('R', Items.REDSTONE)
                .unlockedBy("has_factory_block", has(Automati.FACTORY_BLOCK_ITEM.get()))
                .save(output);
            shaped(RecipeCategory.MISC, Automati.CRUSHER_ITEM.get())
                .pattern("III").pattern("CBC").pattern("CRC")
                .define('I', Items.IRON_INGOT).define('C', Automati.FACTORY_BLOCK_ITEM.get())
                .define('B', Items.IRON_BLOCK).define('R', Items.REDSTONE)
                .unlockedBy("has_factory_block", has(Automati.FACTORY_BLOCK_ITEM.get()))
                .save(output);
            shaped(RecipeCategory.MISC, Automati.ELECTRIC_FURNACE_ITEM.get())
                .pattern("RCR").pattern("CBC").pattern("RCR")
                .define('C', Automati.FACTORY_BLOCK_ITEM.get()).define('R', Items.REDSTONE).define('B', Items.BLAST_FURNACE)
                .unlockedBy("has_factory_block", has(Automati.FACTORY_BLOCK_ITEM.get()))
                .save(output);
            shaped(RecipeCategory.MISC, Automati.ERG_JAR_ITEM.get())
                .pattern("FCF").pattern("FAF").pattern("FIF")
                .define('F', Automati.FACTORY_BLOCK_ITEM.get()).define('C', Items.COPPER_INGOT)
                .define('A', Automati.ASH_BLOCK_ITEM.get()).define('I', Items.IRON_INGOT)
                .unlockedBy("has_ash_block", has(Automati.ASH_BLOCK_ITEM.get()))
                .save(output);

            // -- conduits --
            shaped(RecipeCategory.MISC, Automati.ERG_CABLE_ITEM.get(), 6)
                .pattern("SSS").pattern("CCC").pattern("SSS")
                .define('S', Items.SMOOTH_STONE).define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(output);
            shaped(RecipeCategory.MISC, Automati.ITEM_DUCT_ITEM.get(), 6)
                .pattern("SSS").pattern("I I").pattern("SSS")
                .define('S', Items.SMOOTH_STONE).define('I', Items.IRON_INGOT)
                .unlockedBy("has_smooth_stone", has(Items.SMOOTH_STONE))
                .save(output);

            // -- dust cooking (smelt 200 ticks, blast 100, both 0.7 xp) --
            dustCooking(Automati.IRON_DUST.get(), Items.IRON_INGOT, "iron_ingot", "iron_dust");
            dustCooking(Automati.COPPER_DUST.get(), Items.COPPER_INGOT, "copper_ingot", "copper_dust");
            dustCooking(Automati.GOLD_DUST.get(), Items.GOLD_INGOT, "gold_ingot", "gold_dust");

            // -- the ash reuse chain --
            // gentle heat (any furnace, incl. our electric one) -> SCF
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(Automati.ASH.get()), RecipeCategory.MISC,
                    CookingBookCategory.MISC, Automati.SCF.get(), 0.1F, 200)
                .unlockedBy("has_ash", has(Automati.ASH.get()))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, "scf_from_smelting_ash")));
            // sintering heat (blast furnace only) -> pellets
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(Automati.ASH.get()), RecipeCategory.MISC,
                    CookingBookCategory.MISC, Automati.SINTERED_ASH_PELLET.get(), 0.1F, 100)
                .unlockedBy("has_ash", has(Automati.ASH.get()))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, "sintered_ash_pellet_from_blasting_ash")));
            // ash extends clay: 1 + 1 -> 2 blend, fired like vanilla brick
            shapeless(RecipeCategory.MISC, Automati.ASH_CLAY_BLEND.get(), 2)
                .requires(Automati.ASH.get()).requires(Items.CLAY_BALL)
                .unlockedBy("has_ash", has(Automati.ASH.get()))
                .save(output);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(Automati.ASH_CLAY_BLEND.get()), RecipeCategory.MISC,
                    CookingBookCategory.MISC, Automati.ASH_BRICK.get(), 0.3F, 200)
                .unlockedBy("has_ash_clay_blend", has(Automati.ASH_CLAY_BLEND.get()))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, "ash_brick_from_smelting_blend")));
            shaped(RecipeCategory.BUILDING_BLOCKS, Automati.ASH_BRICKS_ITEM.get())
                .pattern("BB").pattern("BB")
                .define('B', Automati.ASH_BRICK.get())
                .unlockedBy("has_ash_brick", has(Automati.ASH_BRICK.get()))
                .save(output);
            // stabilized road base: ash binds the gravel (and gravity loses)
            shapeless(RecipeCategory.BUILDING_BLOCKS, Automati.ROAD_BASE_ITEM.get())
                .requires(Automati.ASH.get()).requires(Items.GRAVEL)
                .unlockedBy("has_ash", has(Automati.ASH.get()))
                .save(output);
            // water leaching strips the toxic traces; the bucket comes back
            shapeless(RecipeCategory.MISC, Automati.WASHED_ASH.get(), 4)
                .requires(Automati.ASH.get(), 4).requires(Items.WATER_BUCKET)
                .unlockedBy("has_ash", has(Automati.ASH.get()))
                .save(output);
            // washed ash + biomass -> bulk fertilizer (a reskinned bone meal)
            shapeless(RecipeCategory.MISC, Automati.INDUSTRIAL_FERTILIZER.get(), 4)
                .requires(Automati.WASHED_ASH.get()).requires(Items.ROTTEN_FLESH)
                .unlockedBy("has_washed_ash", has(Automati.WASHED_ASH.get()))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, "industrial_fertilizer_from_rotten_flesh")));
            shapeless(RecipeCategory.MISC, Automati.INDUSTRIAL_FERTILIZER.get(), 6)
                .requires(Automati.WASHED_ASH.get()).requires(Items.BONE_MEAL)
                .unlockedBy("has_washed_ash", has(Automati.WASHED_ASH.get()))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, "industrial_fertilizer_from_bone_meal")));
            // vitrified sintered ash: the see-through bunker block
            shaped(RecipeCategory.BUILDING_BLOCKS, Automati.FLY_ASH_GLASS_ITEM.get())
                .pattern("PP").pattern("PP")
                .define('P', Automati.SINTERED_ASH_PELLET.get())
                .unlockedBy("has_sintered_ash_pellet", has(Automati.SINTERED_ASH_PELLET.get()))
                .save(output);
            // SCF-conditioned farmland; the water bucket is a returned catalyst
            shaped(RecipeCategory.BUILDING_BLOCKS, Automati.ASH_RICH_SOIL_ITEM.get(), 4)
                .pattern("DSD").pattern("SWS").pattern("DSD")
                .define('D', Items.DIRT).define('S', Automati.SCF.get()).define('W', Items.WATER_BUCKET)
                .unlockedBy("has_scf", has(Automati.SCF.get()))
                .save(output);
        }

        private void dustCooking(ItemLike dust, ItemLike ingot, String group, String dustName) {
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(dust), RecipeCategory.MISC,
                    CookingBookCategory.MISC, ingot, 0.7F, 200)
                .group(group)
                .unlockedBy("has_" + dustName, has(dust))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, group + "_from_smelting_" + dustName)));
            SimpleCookingRecipeBuilder.blasting(Ingredient.of(dust), RecipeCategory.MISC,
                    CookingBookCategory.MISC, ingot, 0.7F, 100)
                .group(group)
                .unlockedBy("has_" + dustName, has(dust))
                .save(output, ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath(Automati.MODID, group + "_from_blasting_" + dustName)));
        }
    }
}
