package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

// One cable segment: a small Erg buffer that passes energy on along its
// connected sides. A side severed with the wrench has no port and no flow.
public class ErgCableBlockEntity extends AbstractErgBlockEntity implements WrenchableConduit {
    public static final int CAPACITY = 1_000;
    public static final int MAX_TRANSFER = 400;

    private int invertedSides;

    public ErgCableBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ERG_CABLE_BLOCK_ENTITY.get(), pos, state, CAPACITY, MAX_TRANSFER, ErgPort.OPEN);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        maybeSyncEnergyToClients(level, pos);
        distributeEnergy(level, pos, Math.min(energy.getEnergyStored(), MAX_TRANSFER));
    }

    @Override
    protected boolean feedsSide(Direction side) {
        return AbstractConduitBlock.isConnected(getBlockState(), side);
    }

    // Machines are fed at full rate. Another cable only gets half the level
    // difference, so energy runs downhill along a line of cables instead of
    // sloshing back and forth.
    @Override
    protected int pushLimit(BlockEntity neighbour, int budget) {
        return neighbour instanceof ErgCableBlockEntity other
            ? (energy.getEnergyStored() - other.energy.getEnergyStored()) / 2
            : budget;
    }

    @Override
    public int invertedSides() {
        return invertedSides;
    }

    @Override
    public void setInvertedSides(int mask) {
        invertedSides = mask;
        setChanged();
    }

    // A severed side offers no port, so the connection fails from both ends
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null && isSideInverted(side))
            return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("InvertedSides", invertedSides);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invertedSides = input.getIntOr("InvertedSides", 0);
    }
}
