package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The electric furnace cooks whatever a vanilla furnace can, or in blast mode
// whatever a blast furnace can, on Ergs instead of fuel. The mode only picks
// the recipe set: speed and cost are the same either way.
public class ElectricFurnaceBlockEntity extends AbstractProcessorBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_RECEIVE = 200;
    public static final int USE_PER_TICK = 40;    // exactly one coal generator's output
    public static final int SMELT_TICKS = 100;    // twice the speed of a vanilla furnace

    private final RecipeLookup<SmeltingRecipe> smelting = new RecipeLookup<>(() -> RecipeType.SMELTING);
    private final RecipeLookup<BlastingRecipe> blasting = new RecipeLookup<>(() -> RecipeType.BLASTING);

    private boolean blastMode;
    private float lastExperience;     // of the recipe matched most recently
    private float bankedExperience;   // paid out when the player takes the output

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ELECTRIC_FURNACE.blockEntity().get(), pos, state, CAPACITY, MAX_RECEIVE, USE_PER_TICK, SMELT_TICKS);
        syncToMenu(() -> progress, () -> blastMode ? 1 : 0);
    }

    @Override
    protected ItemStack resultFor(ServerLevel level, ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        AbstractCookingRecipe recipe = (blastMode ? blasting : smelting).find(level, input);
        if (recipe == null)
            return ItemStack.EMPTY;
        lastExperience = recipe.experience();
        return recipe.assemble(input);
    }

    @Override
    protected void onProcessed() {
        bankedExperience += lastExperience;
    }

    // The menu's mode button. A half-cooked item does not carry over into the
    // other process, so progress resets.
    public void toggleBlastMode() {
        blastMode = !blastMode;
        progress = 0;
        setChanged();
    }

    public void popExperience(Player player) {
        if (level instanceof ServerLevel serverLevel) {
            int amount = Math.round(bankedExperience);
            bankedExperience = 0;
            if (amount > 0)
                ExperienceOrb.award(serverLevel, player.position(), amount);
            setChanged();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("BankedXp", bankedExperience);
        output.putBoolean("BlastMode", blastMode);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bankedExperience = input.getFloatOr("BankedXp", 0.0F);
        blastMode = input.getBooleanOr("BlastMode", false);
    }
}
