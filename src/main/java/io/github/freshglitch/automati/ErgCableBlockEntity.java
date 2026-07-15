package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// A single cable segment: a small Erg buffer that flows energy downhill.
// Into machines it pushes at full rate; into other cables it only moves half
// the level difference per tick — the gradient is what makes energy travel
// along a line instead of sloshing back and forth.
public class ErgCableBlockEntity extends AbstractErgBlockEntity {
    public static final int CAPACITY = 1_000;
    public static final int MAX_TRANSFER = 400;

    public ErgCableBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ERG_CABLE_BLOCK_ENTITY.get(), pos, state, CAPACITY, MAX_TRANSFER, ErgPort.OPEN);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        maybeSyncEnergyToClients(level, pos);

        int stored = energy.getEnergyStored();
        int budget = Math.min(stored, MAX_TRANSFER);
        if (budget == 0)
            return;

        distributeEnergy(level, pos, budget, neighbour ->
            neighbour instanceof ErgCableBlockEntity other
                ? (stored - other.energy.getEnergyStored()) / 2
                : budget);
    }
}
