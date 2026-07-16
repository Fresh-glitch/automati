package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

// One duct segment: a single-stack buffer that remembers which side its
// contents came in from and never sends them back that way. Real inventories
// (chests, machines) take delivery priority over onward ducts; ties
// round-robin. Hoppers extract, ducts transport. The Engineer's Wrench
// inverts a side's automatic connection decision — severing normal links or
// splicing ones the hopper-nozzle policy declined.
public class ItemDuctBlockEntity extends BlockEntity implements WrenchableConduit {
    public static final int TRANSFER_COOLDOWN = 8;   // hopper cadence
    public static final int ITEMS_PER_TRANSFER = 4;  // but four times the volume

    private ItemStack buffer = ItemStack.EMPTY;
    @Nullable
    private Direction entrySide;
    private int cooldown;
    private int roundRobin;
    private int invertedMask;

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

    // Is this side open for business? The automatic policy (everything except
    // a hopper's non-nozzle faces), XOR any wrench override.
    public boolean isSideOpen(Direction side) {
        return ItemDuctBlock.policyAllows(level, worldPosition, side) ^ isSideInverted(side);
    }

    // Six side-aware ports: inserting through a face stamps that face as the
    // entry side. One-way — nothing can pull back out of a duct.
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] sidePorts = new LazyOptional[6];

    public ItemDuctBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.ITEM_DUCT_BLOCK_ENTITY.get(), pos, state);
        for (Direction direction : Direction.values()) {
            SidePort port = new SidePort(direction);
            sidePorts[direction.ordinal()] = LazyOptional.of(() -> port);
        }
    }

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

            if (buffer.isEmpty()) {
                if (!simulate) {
                    buffer = stack.copy();
                    entrySide = side;
                    setChanged();
                }
                return ItemStack.EMPTY;
            }

            if (!ItemStack.isSameItemSameComponents(buffer, stack))
                return stack;
            int space = buffer.getMaxStackSize() - buffer.getCount();
            if (space <= 0)
                return stack;
            int taken = Math.min(space, stack.getCount());
            if (!simulate) {
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

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (buffer.isEmpty())
            return;

        // pass 1: deliver into real inventories; pass 2: forward along ducts
        Direction[] all = Direction.values();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < all.length; i++) {
                Direction direction = all[(roundRobin + i) % all.length];
                if (direction == entrySide)
                    continue;
                if (!state.getValue(ItemDuctBlock.PROPERTY_BY_DIRECTION.get(direction)))
                    continue;

                BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
                if (neighbour == null)
                    continue;
                boolean isDuct = neighbour instanceof ItemDuctBlockEntity;
                if (isDuct != (pass == 1))
                    continue;

                IItemHandler target = neighbour
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .orElse(null);
                if (target == null)
                    continue;

                ItemStack toSend = buffer.copyWithCount(Math.min(ITEMS_PER_TRANSFER, buffer.getCount()));
                ItemStack leftover = ItemHandlerHelper.insertItem(target, toSend, false);
                int moved = toSend.getCount() - leftover.getCount();
                if (moved > 0) {
                    buffer.shrink(moved);
                    if (buffer.isEmpty()) {
                        buffer = ItemStack.EMPTY;
                        entrySide = null;
                    }
                    roundRobin = (direction.ordinal() + 1) % all.length;
                    cooldown = TRANSFER_COOLDOWN;
                    setChanged();
                    return;
                }
            }
        }

        // nowhere to go right now: nap instead of hammering neighbours
        cooldown = TRANSFER_COOLDOWN;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            Direction port = side == null ? Direction.DOWN : side;
            // closed sides offer no port: nothing gets in, and the connection
            // handshake fails from both ends
            if (side != null && !isSideOpen(side))
                return LazyOptional.empty();
            return sidePorts[port.ordinal()].cast();
        }
        return super.getCapability(cap, side);
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
        output.putInt("InvertedSides", invertedMask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("Buffer", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> buffer = stack);
        int side = input.getIntOr("EntrySide", -1);
        entrySide = side < 0 ? null : Direction.values()[side];
        cooldown = input.getIntOr("Cooldown", 0);
        roundRobin = input.getIntOr("RoundRobin", 0);
        invertedMask = input.getIntOr("InvertedSides", 0);
    }
}
