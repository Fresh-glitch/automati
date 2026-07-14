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
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// A single cable segment: a small Erg buffer that flows energy downhill.
// Into machines it pushes at full rate; into other cables it only moves half
// the level difference per tick — the gradient is what makes energy travel
// along a line instead of sloshing back and forth.
public class ErgCableBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1_000;
    public static final int MAX_TRANSFER = 400;

    private final ErgStorage energy = new ErgStorage(CAPACITY, MAX_TRANSFER);
    private final LazyOptional<IEnergyStorage> energyOptional = LazyOptional.of(() -> energy);

    public ErgCableBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ERG_CABLE_BLOCK_ENTITY.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos) {
        int stored = energy.getEnergyStored();
        int budget = Math.min(stored, MAX_TRANSFER);
        if (budget == 0)
            return;

        // pass 1: census every eligible target and its per-target limit
        // (machines take full rate; other cables only downhill, half the gap)
        List<IEnergyStorage> targets = new ArrayList<>();
        List<Integer> limits = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour == null)
                continue;

            int limit;
            if (neighbour instanceof ErgCableBlockEntity other) {
                limit = Math.min(budget, (stored - other.energy.getEnergyStored()) / 2);
                if (limit <= 0)
                    continue;
            } else {
                limit = budget;
            }

            final int cap = limit;
            neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(target -> {
                if (target.canReceive() && target.receiveEnergy(cap, true) > 0) {
                    targets.add(target);
                    limits.add(cap);
                }
            });
        }
        if (targets.isEmpty())
            return;

        // pass 2: parallel branches share the budget evenly
        int share = Math.max(1, budget / targets.size());
        for (int i = 0; i < targets.size(); i++) {
            if (energy.getEnergyStored() == 0)
                break;
            int amount = Math.min(Math.min(share, limits.get(i)), energy.getEnergyStored());
            int accepted = targets.get(i).receiveEnergy(amount, true);
            if (accepted > 0) {
                targets.get(i).receiveEnergy(energy.extractEnergy(accepted, false), false);
                setChanged();
            }
        }
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
        output.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
    }
}
