package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

// A machine that turns one input item into one output over a fixed number of
// ticks, paying Ergs for every tick of work. Subclasses answer resultFor();
// the slots, the work loop, LIT and persistence live here.
public abstract class AbstractProcessorBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    private final int usePerTick;
    private final int processTicks;

    protected int progress;

    protected AbstractProcessorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           int capacity, int maxReceive, int usePerTick, int processTicks) {
        super(type, pos, state, capacity, maxReceive, ErgPort.RECEIVE_ONLY);
        this.usePerTick = usePerTick;
        this.processTicks = processTicks;
        inventory = new MachineInventory(this, "Input", "Output", this::accepts);
        syncToMenu(() -> progress);
    }

    // What the stack turns into, or EMPTY if this machine cannot process it
    protected abstract ItemStack resultFor(ServerLevel level, ItemStack stack);

    // Called after each finished item
    protected void onProcessed() {
    }

    // Recipes only exist on the server: the client accepts anything and the
    // server has the final word
    private boolean accepts(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return !(level instanceof ServerLevel serverLevel) || !resultFor(serverLevel, stack).isEmpty();
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack input = inventory.getStackInSlot(MachineInventory.INPUT);
        ItemStack result = input.isEmpty() ? ItemStack.EMPTY : resultFor((ServerLevel) level, input);

        boolean working = false;
        if (result.isEmpty()) {
            // nothing to work on: partial progress is lost
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
        } else if (inventory.outputFits(result) && energy.getEnergyStored() >= usePerTick) {
            energy.consume(usePerTick);
            working = true;
            if (++progress >= processTicks) {
                input.shrink(1);
                inventory.addOutput(result);
                progress = 0;
                onProcessed();
            }
            setChanged();
        }
        // out of energy or output full: the machine pauses and keeps its progress

        if (state.getValue(AbstractMachineBlock.LIT) != working)
            level.setBlock(pos, state.setValue(AbstractMachineBlock.LIT, working), 3);

        maybeSyncEnergyToClients(level, pos);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
    }

    // Finds single-input recipes of one type. The last match is remembered and
    // tried first, so working through a whole stack costs one real lookup.
    protected static final class RecipeLookup<R extends Recipe<SingleRecipeInput>> {
        private final Supplier<? extends RecipeType<R>> type;
        @Nullable
        private RecipeHolder<R> last;

        public RecipeLookup(Supplier<? extends RecipeType<R>> type) {
            this.type = type;
        }

        @Nullable
        public R find(ServerLevel level, SingleRecipeInput input) {
            Optional<RecipeHolder<R>> found = level.recipeAccess().getRecipeFor(type.get(), input, level, last);
            if (found.isEmpty())
                return null;
            last = found.get();
            return last.value();
        }
    }
}
