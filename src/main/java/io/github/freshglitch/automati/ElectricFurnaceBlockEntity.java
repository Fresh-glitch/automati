package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

// The electric furnace: smelts everything a vanilla furnace can, using Ergs
// instead of fuel — twice the speed, twice the smelts per coal. Reads the
// vanilla SMELTING recipe library directly, experience included.
public class ElectricFurnaceBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    public static final int CAPACITY = 20_000;      // Erg buffer size
    public static final int MAX_RECEIVE = 200;      // max Ergs accepted per tick
    public static final int USE_PER_TICK = 40;      // exactly one coal generator's output
    public static final int SMELT_TICKS = 100;      // twice vanilla furnace speed

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private RecipeHolder<SmeltingRecipe> lastRecipe;
    private float bankedExperience;
    private int progress;

    // What does this item smelt into? Answered by vanilla's own recipe library.
    private ItemStack resultFor(Level level, ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel))
            return ItemStack.EMPTY;
        SingleRecipeInput input = new SingleRecipeInput(stack);
        var found = serverLevel.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level, lastRecipe);
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
            if (level == null || level.isClientSide())
                return !stack.isEmpty();
            return !resultFor(level, stack).isEmpty();
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Hopper view: smeltables in, products out
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

    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> automationView);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> SMELT_TICKS;
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

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ELECTRIC_FURNACE_BLOCK_ENTITY.get(), pos, state, CAPACITY, MAX_RECEIVE, ErgPort.RECEIVE_ONLY);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack result = resultFor(level, input);

        boolean working = false;
        if (result.isEmpty()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
        } else if (outputHasRoomFor(result) && energy.getEnergyStored() >= USE_PER_TICK) {
            energy.setEnergy(energy.getEnergyStored() - USE_PER_TICK);
            progress++;
            working = true;
            if (progress >= SMELT_TICKS) {
                input.shrink(1);
                depositOutput(result);
                bankedExperience += lastRecipe.value().experience();
                progress = 0;
            }
            setChanged();
        }
        // valid input but no energy / full output = pause, progress kept

        if (state.getValue(ElectricFurnaceBlock.LIT) != working)
            level.setBlock(pos, state.setValue(ElectricFurnaceBlock.LIT, working), 3);

        maybeSyncEnergyToClients(level, pos);
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

    // Smelting experience accumulates and pays out when the player takes the output
    public void popExperience(Player player) {
        if (level instanceof ServerLevel serverLevel) {
            int amount = Math.round(bankedExperience);
            bankedExperience = 0;
            if (amount > 0)
                ExperienceOrb.award(serverLevel, player.position(), amount);
            setChanged();
        }
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
        return Component.translatable("block.automati.electric_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this, data);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemOptional.invalidate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Input", ItemStack.OPTIONAL_CODEC, inventory.getStackInSlot(INPUT_SLOT));
        output.store("Output", ItemStack.OPTIONAL_CODEC, inventory.getStackInSlot(OUTPUT_SLOT));
        output.putInt("Progress", progress);
        output.putFloat("BankedXp", bankedExperience);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("Input", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> inventory.setStackInSlot(INPUT_SLOT, stack));
        input.read("Output", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> inventory.setStackInSlot(OUTPUT_SLOT, stack));
        progress = input.getIntOr("Progress", 0);
        bankedExperience = input.getFloatOr("BankedXp", 0.0F);
    }
}
