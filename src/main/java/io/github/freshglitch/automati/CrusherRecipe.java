package io.github.freshglitch.automati;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

// A crushing recipe: one ingredient in, one product out. Data-driven — every
// recipe is a JSON file under data/automati/recipe/, editable by datapacks.
// Rides on the same single-input machinery the vanilla stonecutter uses.
public class CrusherRecipe extends SingleItemRecipe {
    public static final MapCodec<CrusherRecipe> MAP_CODEC = SingleItemRecipe.simpleMapCodec(CrusherRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> STREAM_CODEC = SingleItemRecipe.simpleStreamCodec(CrusherRecipe::new);

    public CrusherRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        super(commonInfo, input, result);
    }

    @Override
    public RecipeSerializer<CrusherRecipe> getSerializer() {
        return Automati.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CrusherRecipe> getType() {
        return Automati.CRUSHING_RECIPE_TYPE.get();
    }

    @Override
    public String group() {
        return "crushing";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return Automati.CRUSHING_CATEGORY.get();
    }
}
