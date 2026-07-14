package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
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

// Base class for every Erg-holding block entity. Owns the buffer, saves and
// loads it, and keeps nearby clients informed so the Engineer's Goggles HUD
// has something to show. A new machine extends this, passes its capacity and
// transfer rate to the constructor, calls maybeSyncEnergyToClients() from its
// tick — and diagnostics work, no further wiring.
public abstract class AbstractErgBlockEntity extends BlockEntity {
    protected final ErgStorage energy;

    private int lastSyncedEnergy = -1;
    private int syncCooldown;

    protected AbstractErgBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxTransfer) {
        super(type, pos, state);
        this.energy = new ErgStorage(capacity, maxTransfer);
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
