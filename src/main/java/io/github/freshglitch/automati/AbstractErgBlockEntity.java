package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
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

import java.util.function.IntSupplier;

// Base for every block entity that holds Ergs. Owns the buffer and its port,
// persistence, the throttled client sync behind the goggles HUD, and the
// plumbing machines share: menu values, display name, an optional inventory.
//
// A new machine passes capacity, transfer rate and port type to super and
// implements serverTick(), calling maybeSyncEnergyToClients() in it.
// Producers push their output with distributeEnergy().
public abstract class AbstractErgBlockEntity extends BlockEntity implements ServerTicking {

    // What the energy port lets neighbours do
    public enum ErgPort {
        RECEIVE_ONLY,  // consumers
        EXTRACT_ONLY,  // producers
        OPEN           // conduits
    }

    private static final Direction[] DIRECTIONS = Direction.values();
    private static final IntSupplier[] NO_MENU_VALUES = {};

    protected final ErgStorage energy;
    private final LazyOptional<IEnergyStorage> energyOptional;

    // Machines with slots assign this in their constructor. The base then
    // saves it, serves it to hoppers and spills it when the block breaks.
    @Nullable
    protected MachineInventory inventory;

    private IntSupplier[] menuValues = NO_MENU_VALUES;

    private int lastSyncedEnergy = -1;
    private int syncCooldown;

    // Port traffic averaged over 20-tick windows, so the readout holds steady
    private int flowAccumIn, flowAccumOut, flowSampleTicks;
    private int flowInRate, flowOutRate;
    private int lastSyncedFlowIn = -1, lastSyncedFlowOut = -1;

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

    public int getFlowInRate() {
        return flowInRate;
    }

    public int getFlowOutRate() {
        return flowOutRate;
    }

    public MachineInventory getInventory() {
        return inventory;
    }

    // Shares a budget of Ergs evenly among the neighbours that can take them,
    // like parallel branches of a circuit. Override feedsSide() and
    // pushLimit() to narrow where the output goes.
    protected void distributeEnergy(Level level, BlockPos pos, int budget) {
        if (budget <= 0 || energy.getEnergyStored() == 0)
            return;

        IEnergyStorage[] targets = new IEnergyStorage[DIRECTIONS.length];
        int[] limits = new int[DIRECTIONS.length];
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : DIRECTIONS) {
            if (!feedsSide(direction))
                continue;
            BlockEntity neighbour = level.getBlockEntity(cursor.setWithOffset(pos, direction));
            if (neighbour == null)
                continue;
            int limit = Math.min(budget, pushLimit(neighbour, budget));
            if (limit <= 0)
                continue;
            IEnergyStorage target = neighbour.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target != null && target.canReceive() && target.receiveEnergy(limit, true) > 0) {
                targets[count] = target;
                limits[count++] = limit;
            }
        }
        if (count == 0)
            return;

        int share = Math.max(1, budget / count);
        for (int i = 0; i < count && energy.getEnergyStored() > 0; i++) {
            int amount = Math.min(Math.min(share, limits[i]), energy.getEnergyStored());
            int accepted = targets[i].receiveEnergy(amount, true);
            if (accepted > 0) {
                targets[i].receiveEnergy(energy.extractEnergy(accepted, false), false);
                setChanged();
            }
        }
    }

    // Whether distributeEnergy() may push out through this side
    protected boolean feedsSide(Direction side) {
        return true;
    }

    // The most distributeEnergy() may send this neighbour; 0 or less skips it
    protected int pushLimit(BlockEntity neighbour, int budget) {
        return budget;
    }

    // Sends the energy level and flow rates to nearby clients, at most twice
    // a second and only when something changed
    protected void maybeSyncEnergyToClients(Level level, BlockPos pos) {
        flowAccumIn += energy.pollTickIn();
        flowAccumOut += energy.pollTickOut();
        if (++flowSampleTicks >= 20) {
            flowInRate = flowAccumIn / 20;
            flowOutRate = flowAccumOut / 20;
            flowAccumIn = 0;
            flowAccumOut = 0;
            flowSampleTicks = 0;
        }

        if (syncCooldown > 0) {
            syncCooldown--;
            return;
        }
        if (energy.getEnergyStored() != lastSyncedEnergy
                || flowInRate != lastSyncedFlowIn || flowOutRate != lastSyncedFlowOut) {
            lastSyncedEnergy = energy.getEnergyStored();
            lastSyncedFlowIn = flowInRate;
            lastSyncedFlowOut = flowOutRate;
            syncCooldown = 10;
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // Call once from the constructor: the values the machine's menu shows
    // besides the energy level. A later call replaces an earlier one.
    protected final void syncToMenu(IntSupplier... values) {
        menuValues = values;
    }

    // What an open menu mirrors. A data slot only carries 16 bits, so slots 0
    // and 1 hold the two halves of the energy level; the syncToMenu() values
    // follow from slot 2.
    public ContainerData menuData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energy.getEnergyStored() & 0xFFFF;
                    case 1 -> energy.getEnergyStored() >>> 16;
                    default -> menuValues[index - 2].getAsInt();
                };
            }

            @Override
            public void set(int index, int value) {
                // one way: the server never takes values back from the client
            }

            @Override
            public int getCount() {
                return 2 + menuValues.length;
            }
        };
    }

    // Satisfies MenuProvider for the machines that implement it
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY)
            return energyOptional.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER && inventory != null)
            return inventory.automation().cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyOptional.invalidate();
        if (inventory != null)
            inventory.automation().invalidate();
    }

    // Since 26.x this is where a container spills its contents on removal
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (inventory != null && level != null && !level.isClientSide())
            inventory.drop(level, pos);
    }

    // Clients get this tag on chunk load and on sendBlockUpdated. It runs
    // through loadAdditional, whose reads all tolerate missing keys.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("FlowIn", flowInRate);
        tag.putInt("FlowOut", flowOutRate);
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
        if (inventory != null)
            inventory.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
        // flow rates only travel in update tags
        flowInRate = input.getIntOr("FlowIn", 0);
        flowOutRate = input.getIntOr("FlowOut", 0);
        if (inventory != null)
            inventory.load(input);
    }
}
