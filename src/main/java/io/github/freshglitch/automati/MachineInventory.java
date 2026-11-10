package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.Predicate;

// The two slots of an item-handling machine: slot 0 takes whatever the
// machine accepts, slot 1 is its output, which only the machine itself fills.
// Hoppers and ducts go through the automation view: insert into the input,
// extract from the output, nothing else.
public class MachineInventory extends ItemStackHandler {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;

    private final BlockEntity owner;
    private final String inputKey;
    private final String outputKey;
    private final Predicate<ItemStack> accepts;
    private final LazyOptional<IItemHandler> automation = LazyOptional.of(AutomationView::new);

    // inputKey and outputKey name the two slots in the machine's save data
    public MachineInventory(BlockEntity owner, String inputKey, String outputKey, Predicate<ItemStack> accepts) {
        super(2);
        this.owner = owner;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.accepts = accepts;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == INPUT && accepts.test(stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        owner.setChanged();
    }

    public LazyOptional<IItemHandler> automation() {
        return automation;
    }

    public boolean outputFits(ItemStack result) {
        ItemStack output = getStackInSlot(OUTPUT);
        return output.isEmpty()
            || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    // Check outputFits() first: this does not
    public void addOutput(ItemStack result) {
        ItemStack output = getStackInSlot(OUTPUT);
        if (output.isEmpty()) {
            setStackInSlot(OUTPUT, result.copy());
        } else {
            output.grow(result.getCount());
            owner.setChanged();
        }
    }

    public void drop(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), getStackInSlot(INPUT));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), getStackInSlot(OUTPUT));
    }

    public void save(ValueOutput output) {
        output.store(inputKey, ItemStack.OPTIONAL_CODEC, getStackInSlot(INPUT));
        output.store(outputKey, ItemStack.OPTIONAL_CODEC, getStackInSlot(OUTPUT));
    }

    // Slots absent from the data are left alone: the energy sync packets reuse
    // the load path and must not wipe the client's copy
    public void load(ValueInput input) {
        input.read(inputKey, ItemStack.OPTIONAL_CODEC).ifPresent(stack -> setStackInSlot(INPUT, stack));
        input.read(outputKey, ItemStack.OPTIONAL_CODEC).ifPresent(stack -> setStackInSlot(OUTPUT, stack));
    }

    private final class AutomationView implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return MachineInventory.this.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT ? MachineInventory.this.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT ? MachineInventory.this.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return MachineInventory.this.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MachineInventory.this.isItemValid(slot, stack);
        }
    }
}
