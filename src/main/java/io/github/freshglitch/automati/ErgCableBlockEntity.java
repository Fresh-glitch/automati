package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

// A single cable segment: a small Erg buffer that flows energy downhill.
// Into machines it pushes at full rate; into other cables it only moves half
// the level difference per tick — the gradient is what makes energy travel
// along a line instead of sloshing back and forth. Sides severed with the
// Engineer's Wrench expose no energy port and carry no flow.
public class ErgCableBlockEntity extends AbstractErgBlockEntity implements WrenchableConduit {
    public static final int CAPACITY = 1_000;
    public static final int MAX_TRANSFER = 400;

    private int invertedMask;

    public ErgCableBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ERG_CABLE_BLOCK_ENTITY.get(), pos, state, CAPACITY, MAX_TRANSFER, ErgPort.OPEN);
    }

    @Override
    public boolean isSideInverted(Direction side) {
        return (invertedMask & (1 << side.ordinal())) != 0;
    }

    @Override
    public void setSideInverted(Direction side, boolean inverted) {
        if (inverted)
            invertedMask |= (1 << side.ordinal());
        else
            invertedMask &= ~(1 << side.ordinal());
        setChanged();
    }

    // A severed side offers no port: neighbours can't push in, and the
    // connection handshake fails from both ends
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null && isSideInverted(side))
            return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        maybeSyncEnergyToClients(level, pos);

        int stored = energy.getEnergyStored();
        int budget = Math.min(stored, MAX_TRANSFER);
        if (budget == 0)
            return;

        distributeEnergy(level, pos, budget,
            neighbour -> neighbour instanceof ErgCableBlockEntity other
                ? (stored - other.energy.getEnergyStored()) / 2
                : budget,
            // only flow along sides that are actually connected
            direction -> state.getValue(ErgCableBlock.PROPERTY_BY_DIRECTION.get(direction)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("InvertedSides", invertedMask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invertedMask = input.getIntOr("InvertedSides", 0);
    }
}
