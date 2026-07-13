package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.SlotItemHandler;

// Container logic for the crusher GUI: input slot, output slot, progress and
// energy gauges.
public class CrusherMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT_X = 56;
    public static final int INPUT_SLOT_Y = 35;
    public static final int OUTPUT_SLOT_X = 116;
    public static final int OUTPUT_SLOT_Y = 35;

    private final CrusherBlockEntity blockEntity;
    private final ContainerData data;

    // Client-side constructor: the block position arrives over the network
    public CrusherMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (CrusherBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(4));
    }

    // Server-side constructor
    public CrusherMenu(int containerId, Inventory playerInventory, CrusherBlockEntity blockEntity, ContainerData data) {
        super(Automati.CRUSHER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getInventory(), CrusherBlockEntity.INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y));
        // Product slot: take-only
        addSlot(new SlotItemHandler(blockEntity.getInventory(), CrusherBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    // Same initial-sync race protection as the other machines
    private int initialResyncTicks = 5;

    @Override
    public void broadcastChanges() {
        if (initialResyncTicks > 0) {
            initialResyncTicks--;
            broadcastFullState();
        } else {
            super.broadcastChanges();
        }
    }

    public boolean isCrushing() {
        return data.get(0) > 0;
    }

    // 0..1 fraction of the current operation completed
    public float getProgress() {
        int total = data.get(1);
        return total == 0 ? 0 : (float) data.get(0) / total;
    }

    public int getEnergy() {
        return ((data.get(3) & 0xFFFF) << 16) | (data.get(2) & 0xFFFF);
    }

    public int getCapacity() {
        return CrusherBlockEntity.CAPACITY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index <= 1) {
            // Machine slots -> player inventory
            if (!moveItemStackTo(stack, 2, 38, true))
                return ItemStack.EMPTY;
        } else {
            // Player inventory -> input slot, if the crusher knows the item
            if (!CrusherBlockEntity.resultFor(stack).isEmpty()) {
                if (!moveItemStackTo(stack, 0, 1, false))
                    return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
            && player.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos())) <= 64.0;
    }
}
