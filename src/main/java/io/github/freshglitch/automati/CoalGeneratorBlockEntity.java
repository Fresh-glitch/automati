package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

// The generator's brain: burns coal/charcoal into Ergs, stores them, and pushes
// them to adjacent machines every tick. Every piece of burnt fuel leaves ash
// behind — and a full ash bin clogs the machine until it is emptied.
public class CoalGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 100_000;          // Erg buffer size
    public static final int GENERATION_PER_TICK = 40;    // Ergs per tick while burning
    public static final int MAX_PUSH_PER_TICK = 200;     // max Ergs pushed to each neighbour per tick
    public static final int BURN_TICKS_PER_FUEL = 1600;  // one coal/charcoal, same as a furnace

    public static final int FUEL_SLOT = 0;
    public static final int ASH_SLOT = 1;
    public static final int MAX_ASH = 64;                // a full bin chokes the machine

    // Slot 0: fuel (strictly coal/charcoal). Slot 1: ash byproduct — nothing may
    // be inserted there; the machine itself deposits ash bypassing this check.
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL));
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // What hoppers and other machines see: fuel goes in, only ash comes out.
    // A hopper below the generator drains the ash bin; one above feeds it coal.
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
            return slot == FUEL_SLOT ? inventory.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == ASH_SLOT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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

    private final ErgStorage energy = new ErgStorage(CAPACITY, MAX_PUSH_PER_TICK);
    private final LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> automationView);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energy);

    private int burnTime; // ticks of burn remaining on the current piece of fuel

    // Synced to the menu/screen. Energy is split into two 16-bit halves because
    // vanilla container data slots are not safe beyond 16 bits.
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> BURN_TICKS_PER_FUEL;
                case 2 -> energy.getEnergyStored() & 0xFFFF;
                case 3 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                case 4 -> isClogged() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0)
                burnTime = value;
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.COAL_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        // Burn the current fuel; when it burns out, its residue drops into the ash bin
        if (burnTime > 0) {
            burnTime--;
            energy.generate(GENERATION_PER_TICK);
            changed = true;
            if (burnTime == 0)
                depositAsh(level, pos);
        }

        // Ignite a new piece only if the buffer has room and the grate isn't
        // choked with ash — a clogged machine refuses to restoke
        if (burnTime == 0 && !isClogged() && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            ItemStack consumed = inventory.extractItem(FUEL_SLOT, 1, false);
            if (!consumed.isEmpty()) {
                burnTime = BURN_TICKS_PER_FUEL;
                changed = true;
            }
        }

        // Keep the LIT blockstate in sync so the front glows while burning
        boolean burning = burnTime > 0;
        if (state.getValue(CoalGeneratorBlock.LIT) != burning)
            level.setBlock(pos, state.setValue(CoalGeneratorBlock.LIT, burning), 3);

        pushEnergyToNeighbours(level, pos);

        if (changed)
            setChanged();
    }

    // A full ash bin means the machine can no longer breathe
    public boolean isClogged() {
        return inventory.getStackInSlot(ASH_SLOT).getCount() >= MAX_ASH;
    }

    private void depositAsh(Level level, BlockPos pos) {
        ItemStack ash = inventory.getStackInSlot(ASH_SLOT);
        if (ash.isEmpty())
            inventory.setStackInSlot(ASH_SLOT, new ItemStack(Automati.ASH.get()));
        else
            ash.grow(1);
        setChanged();

        // that deposit just filled the bin: the machine chokes, audibly, once
        if (isClogged())
            level.playSound(null, pos, Automati.COAL_GENERATOR_CLOG.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    // Offer energy to every adjacent block that exposes an energy capability
    private void pushEnergyToNeighbours(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (energy.getEnergyStored() == 0)
                return;
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour == null)
                continue;
            neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(target -> {
                if (target.canReceive()) {
                    int accepted = target.receiveEnergy(Math.min(MAX_PUSH_PER_TICK, energy.getEnergyStored()), true);
                    if (accepted > 0) {
                        target.receiveEnergy(energy.extractEnergy(accepted, false), false);
                        setChanged();
                    }
                }
            });
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(FUEL_SLOT));
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(ASH_SLOT));
    }

    // Called by the game just before this block entity is removed from the world —
    // this is where containers drop their contents since 26.x
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide())
            dropContents(level, pos);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.automati.coal_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(containerId, playerInventory, this, data);
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
        output.store("Fuel", ItemStack.OPTIONAL_CODEC, inventory.getStackInSlot(FUEL_SLOT));
        output.store("Ash", ItemStack.OPTIONAL_CODEC, inventory.getStackInSlot(ASH_SLOT));
        output.putInt("BurnTime", burnTime);
        output.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.setStackInSlot(FUEL_SLOT, input.read("Fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        inventory.setStackInSlot(ASH_SLOT, input.read("Ash", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        burnTime = input.getIntOr("BurnTime", 0);
        energy.setEnergy(input.getIntOr("Energy", 0));
    }
}
