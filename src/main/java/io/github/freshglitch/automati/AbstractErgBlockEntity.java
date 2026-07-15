package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import java.util.function.ToIntFunction;

// Base class for every Erg-holding block entity. Owns the buffer, its
// save/load, the exposed energy port, and the throttled client sync that
// feeds the Engineer's Goggles HUD.
//
// A new machine's checklist:
//   1. extend this, passing capacity, transfer rate, and its port type
//   2. override serverTick() with the machine's logic; call
//      maybeSyncEnergyToClients() somewhere in it
//   3. producers: call distributeEnergy() to push output to neighbours
// Everything else — persistence, capabilities, goggles support — is inherited.
public abstract class AbstractErgBlockEntity extends BlockEntity {

    // What the machine's energy port lets neighbours do
    public enum ErgPort {
        RECEIVE_ONLY,  // consumers: energy flows in, never out
        EXTRACT_ONLY,  // producers: energy flows out, never in
        OPEN           // conduits: both directions
    }

    protected final ErgStorage energy;
    private final LazyOptional<IEnergyStorage> energyOptional;

    private int lastSyncedEnergy = -1;
    private int syncCooldown;

    protected AbstractErgBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                     int capacity, int maxTransfer, ErgPort port) {
        super(type, pos, state);
        this.energy = new ErgStorage(capacity, maxTransfer);
        IEnergyStorage exposed = switch (port) {
            case RECEIVE_ONLY -> energy.receiveOnlyView();
            case EXTRACT_ONLY -> energy.extractOnlyView();
            case OPEN -> energy;
        };
        this.energyOptional = LazyOptional.of(() -> exposed);
    }

    // Machines override this with their logic; blocks dispatch to it each tick
    public void serverTick(Level level, BlockPos pos, BlockState state) {
    }

    // Share a budget of Ergs evenly among every adjacent block that can accept
    // them — parallel loads draw simultaneously, like branches of a real
    // circuit. perNeighbourLimit may cap what a specific neighbour can take
    // this tick (return <= 0 to skip it); pass null for no per-neighbour rule.
    protected void distributeEnergy(Level level, BlockPos pos, int budget,
                                    @Nullable ToIntFunction<BlockEntity> perNeighbourLimit) {
        if (budget <= 0 || energy.getEnergyStored() == 0)
            return;

        List<IEnergyStorage> targets = new ArrayList<>();
        List<Integer> limits = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour == null)
                continue;

            int limit = perNeighbourLimit == null ? budget
                : Math.min(budget, perNeighbourLimit.applyAsInt(neighbour));
            if (limit <= 0)
                continue;

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

    // Push the energy level to nearby clients at most twice a second, and
    // only when it actually changed
    protected void maybeSyncEnergyToClients(Level level, BlockPos pos) {
        if (syncCooldown > 0) {
            syncCooldown--;
            return;
        }
        if (energy.getEnergyStored() != lastSyncedEnergy) {
            lastSyncedEnergy = energy.getEnergyStored();
            syncCooldown = 10;
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
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

    // The tag clients receive on chunk load and on sendBlockUpdated — it flows
    // into loadAdditional, whose reads all tolerate missing keys
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", energy.getEnergyStored());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
