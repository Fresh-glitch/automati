package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

// A load bank: the real-world rig engineers use to test generators. It accepts
// Ergs and burns them off through resistor coils as waste heat, at a rate the
// player dials in from the GUI. Deliberately wasteful — that's its job.
public class LoadBankBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    public static final int CAPACITY = 10_000;   // small input buffer
    public static final int MAX_RECEIVE = 400;   // max Ergs accepted per tick
    public static final int MAX_RATE = 400;      // max dump rate, Ergs per tick
    public static final int RATE_STEP = 20;      // one button press

    // What neighbours see: energy flows in, never back out
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

    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> receiveOnly);

    private int dumpRate = 40; // Ergs dissipated per tick; starts matched to one generator

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> dumpRate;
                case 1 -> energy.getEnergyStored() & 0xFFFF;
                case 2 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0)
                dumpRate = value;
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public LoadBankBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.LOAD_BANK_BLOCK_ENTITY.get(), pos, state, CAPACITY, MAX_RECEIVE);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        // Dissipate energy through the coils
        int dumped = Math.min(dumpRate, energy.getEnergyStored());
        if (dumped > 0) {
            energy.setEnergy(energy.getEnergyStored() - dumped);
            setChanged();
        }

        // Coils glow whenever energy is actually being burned off
        boolean glowing = dumped > 0;
        if (state.getValue(LoadBankBlock.LIT) != glowing)
            level.setBlock(pos, state.setValue(LoadBankBlock.LIT, glowing), 3);

        maybeSyncEnergyToClients(level, pos);
    }

    // Called from the menu when the player presses - or +
    public void adjustDumpRate(int delta) {
        dumpRate = Math.max(0, Math.min(MAX_RATE, dumpRate + delta));
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.automati.load_bank");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LoadBankMenu(containerId, playerInventory, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)
            return energyOptional.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("DumpRate", dumpRate);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        dumpRate = Math.max(0, Math.min(MAX_RATE, input.getIntOr("DumpRate", 40)));
    }
}
