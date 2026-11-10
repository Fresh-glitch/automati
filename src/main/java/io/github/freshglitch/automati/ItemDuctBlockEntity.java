package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

// One duct segment: a single-stack buffer that remembers the side its cargo
// came in from and never sends it back that way. Real inventories take
// delivery before onward ducts; ties round-robin. Hoppers extract, ducts
// transport.
public class ItemDuctBlockEntity extends BlockEntity implements ServerTicking, WrenchableConduit {
    public static final int TRANSFER_COOLDOWN = 8;   // hopper cadence
    public static final int ITEMS_PER_TRANSFER = 4;  // at four times the volume
    private static final int SYNC_INTERVAL = 10;
    private static final Direction[] DIRECTIONS = Direction.values();

    private ItemStack buffer = ItemStack.EMPTY;
    @Nullable
    private Direction entrySide;
    private int cooldown;
    private int roundRobin;
    private int invertedSides;

    private ItemStack lastSyncedBuffer = ItemStack.EMPTY;
    private int syncCooldown;

    // One insert-only port per face, indexed by Direction.ordinal()
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] sidePorts = new LazyOptional[DIRECTIONS.length];

    public ItemDuctBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ITEM_DUCT_BLOCK_ENTITY.get(), pos, state);
        for (Direction side : DIRECTIONS) {
            SidePort port = new SidePort(side);
            sidePorts[side.ordinal()] = LazyOptional.of(() -> port);
        }
    }

    // Inserting through a face stamps it as the entry side. Nothing can pull
    // cargo back out of a duct.
    private class SidePort implements IItemHandler {
        private final Direction side;

        private SidePort(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return buffer;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty())
                return ItemStack.EMPTY;
            if (!buffer.isEmpty() && !ItemStack.isSameItemSameComponents(buffer, stack))
                return stack;

            int space = buffer.isEmpty() ? stack.getCount() : buffer.getMaxStackSize() - buffer.getCount();
            int taken = Math.min(space, stack.getCount());
            if (taken <= 0)
                return stack;
            if (!simulate) {
                if (buffer.isEmpty())
                    buffer = stack.copyWithCount(taken);
                else
                    buffer.grow(taken);
                entrySide = side;
                setChanged();
            }
            return taken == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
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

    // The automatic policy for a side, flipped by any wrench override
    private boolean isSideOpen(Direction side) {
        return ItemDuctBlock.policyAllows(level, worldPosition, side) ^ isSideInverted(side);
    }

    // What the goggles show when looking at this duct
    public ItemStack getClientBuffer() {
        return buffer;
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        syncBufferToClients(level, pos, state);

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (buffer.isEmpty())
            return;

        // Rest after a transfer, and just as long when nothing took the cargo
        cooldown = TRANSFER_COOLDOWN;
        // Pass 0 delivers into real inventories, pass 1 forwards along ducts
        for (int pass = 0; pass < 2; pass++)
            for (int i = 0; i < DIRECTIONS.length; i++)
                if (pushTo(level, pos, state, DIRECTIONS[(roundRobin + i) % DIRECTIONS.length], pass == 1))
                    return;
    }

    private boolean pushTo(Level level, BlockPos pos, BlockState state, Direction side, boolean toDucts) {
        if (side == entrySide || !AbstractConduitBlock.isConnected(state, side))
            return false;
        BlockEntity neighbour = level.getBlockEntity(pos.relative(side));
        if (neighbour == null || (neighbour instanceof ItemDuctBlockEntity) != toDucts)
            return false;
        IItemHandler target = neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
        if (target == null)
            return false;

        ItemStack sent = buffer.copyWithCount(Math.min(ITEMS_PER_TRANSFER, buffer.getCount()));
        int moved = sent.getCount() - ItemHandlerHelper.insertItem(target, sent, false).getCount();
        if (moved <= 0)
            return false;

        buffer.shrink(moved);
        if (buffer.isEmpty()) {
            buffer = ItemStack.EMPTY;
            entrySide = null;
        }
        roundRobin = (side.ordinal() + 1) % DIRECTIONS.length;
        setChanged();
        return true;
    }

    // Tells nearby clients about the cargo for the goggles HUD, at most twice
    // a second and only when it changed
    private void syncBufferToClients(Level level, BlockPos pos, BlockState state) {
        if (syncCooldown > 0) {
            syncCooldown--;
            return;
        }
        if (!ItemStack.matches(buffer, lastSyncedBuffer)) {
            lastSyncedBuffer = buffer.copy();
            syncCooldown = SYNC_INTERVAL;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // Sent to clients on chunk load and on sendBlockUpdated
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ItemStack.OPTIONAL_CODEC
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), buffer)
            .result()
            .ifPresent(encoded -> tag.put("Buffer", encoded));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // A closed side offers no port, so nothing gets in and the connection
    // check fails from both ends
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap != ForgeCapabilities.ITEM_HANDLER)
            return super.getCapability(cap, side);
        if (side == null)
            return sidePorts[Direction.DOWN.ordinal()].cast();
        return isSideOpen(side) ? sidePorts[side.ordinal()].cast() : LazyOptional.empty();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (LazyOptional<IItemHandler> port : sidePorts)
            port.invalidate();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide())
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), buffer);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Buffer", ItemStack.OPTIONAL_CODEC, buffer);
        output.putInt("EntrySide", entrySide == null ? -1 : entrySide.ordinal());
        output.putInt("Cooldown", cooldown);
        output.putInt("RoundRobin", roundRobin);
        output.putInt("InvertedSides", invertedSides);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("Buffer", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> buffer = stack);
        int side = input.getIntOr("EntrySide", -1);
        entrySide = side < 0 ? null : DIRECTIONS[side];
        cooldown = input.getIntOr("Cooldown", 0);
        roundRobin = input.getIntOr("RoundRobin", 0);
        invertedSides = input.getIntOr("InvertedSides", 0);
    }
}
