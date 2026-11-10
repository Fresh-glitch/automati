package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

// The crusher shreds items into something finer: ores into doubled dust,
// cobble into gravel, and so on. What yields what is decided by the
// data-driven recipes in data/automati/recipe/crushing/.
public class CrusherBlockEntity extends AbstractProcessorBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 200;
    public static final int USE_PER_TICK = 20;
    public static final int CRUSH_TICKS = 200;   // 10 seconds per item

    private final RecipeLookup<CrusherRecipe> recipes = new RecipeLookup<>(Automati.CRUSHING_RECIPE_TYPE);

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.CRUSHER.blockEntity().get(), pos, state, CAPACITY, MAX_RECEIVE, USE_PER_TICK, CRUSH_TICKS);
    }

    @Override
    protected ItemStack resultFor(ServerLevel level, ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        CrusherRecipe recipe = recipes.find(level, input);
        return recipe == null ? ItemStack.EMPTY : recipe.assemble(input);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this);
    }
}
