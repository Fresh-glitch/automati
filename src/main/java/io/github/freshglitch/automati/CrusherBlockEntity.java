package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

// The crusher: Automati's first productive Erg consumer. A shredder — material
// dropped onto the counter-rotating blade rollers up top gets torn down into
// something finer: ores into doubled raw metal, cobble into gravel, and so on.
public class CrusherBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 20_000;      // Erg buffer size
    public static final int MAX_RECEIVE = 200;      // max Ergs accepted per tick
    public static final int USE_PER_TICK = 20;      // Ergs consumed per working tick
    public static final int CRUSH_TICKS = 200;      // 10 seconds per operation

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    // Cache of the last matched recipe: the recipe manager checks it first,
    // so crushing a whole stack costs one real lookup instead of hundreds
    private RecipeHolder<CrusherRecipe> lastRecipe;

    // What comes out when a given item goes through the rollers — answered by
    // the data-driven crushing recipes under data/automati/recipe/crushing/
    private ItemStack resultFor(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel))
            return ItemStack.EMPTY;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        var found = serverLevel.recipeAccess().getRecipeFor(Automati.CRUSHING_RECIPE_TYPE.get(), input, level, lastRecipe);
        if (found.isPresent()) {
            lastRecipe = found.get();
            return found.get().value().assemble(input);
        }
        return ItemStack.EMPTY;
    }

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot != INPUT_SLOT)
                return false;
            // recipes only exist server-side; the client accepts optimistically
            // and the server has the final word
            if (level == null || level.isClientSide())
                return !stack.isEmpty();
            return !resultFor(level, stack).isEmpty();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Hopper view: crushable items in the top, only products out the bottom
    private final IItemHandler automationView = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT_SLOT ? inventory.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    };

    private final ErgStorage energy = new ErgStorage(CAPACITY, MAX_RECEIVE);

    // Neighbours may feed the crusher energy but never drain it
    private final IEnergyStorage receiveOnly = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return energy.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> automationView);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> receiveOnly);

    private int progress; // ticks of crushing done on the current item

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> CRUSH_TICKS;
                case 2 -> energy.getEnergyStored() & 0xFFFF;
                case 3 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0)
                progress = value;
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.CRUSHER_BLOCK_ENTITY.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack result = resultFor(level, input);

        boolean working = false;
        if (result.isEmpty()) {
            // nothing crushable in the hopper: any partial progress is lost
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
        } else if (outputHasRoomFor(result) && energy.getEnergyStored() >= USE_PER_TICK) {
            // grind: burn energy, advance the operation
            energy.setEnergy(energy.getEnergyStored() - USE_PER_TICK);
            progress++;
            working = true;
            if (progress >= CRUSH_TICKS) {
                input.shrink(1);
                depositOutput(result);
                progress = 0;
            }
            setChanged();
        }
        // note: valid input but no energy / full output = pause, progress kept

        if (state.getValue(CrusherBlock.LIT) != working)
            level.setBlock(pos, state.setValue(CrusherBlock.LIT, working), 3);
    }

    private boolean outputHasRoomFor(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty())
            return true;
        return ItemStack.isSameItemSameComponents(out, result)
            && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void depositOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty())
            inventory.setStackInSlot(OUTPUT_SLOT, result.copy());
        else
            out.grow(result.getCount());
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(INPUT_SLOT));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(OUTPUT_SLOT));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide())
            dropContents(level, pos);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.automati.crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this, data);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)
            return energyOptional.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemOptional.invalidate();
        energyOptional.invalidate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", ItemStack.OPTIONAL_CODEC, inventory.getStackInSlot(INPUT_SLOT));
        output.store("Output", ItemStack.OPTIONAL_CODEC, inventory.getStackInSlot(OUTPUT_SLOT));
        output.putInt("Progress", progress);
        output.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.setStackInSlot(INPUT_SLOT, input.read("Input", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        inventory.setStackInSlot(OUTPUT_SLOT, input.read("Output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        progress = input.getIntOr("Progress", 0);
        energy.setEnergy(input.getIntOr("Energy", 0));
    }
}
