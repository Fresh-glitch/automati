package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

// Container logic for the crusher GUI: input slot, output slot, and crushing
// progress. Energy sync, player inventory, and the open-race fix come from
// the base class.
public class CrusherMenu extends AbstractErgMenu {
    public static final int INPUT_SLOT_X = 56;
    public static final int INPUT_SLOT_Y = 35;
    public static final int OUTPUT_SLOT_X = 116;
    public static final int OUTPUT_SLOT_Y = 35;

    // Client-side constructor: the block position arrives over the network
    public CrusherMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (CrusherBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(4));
    }

    // Server-side constructor
    public CrusherMenu(int containerId, Inventory playerInventory, CrusherBlockEntity blockEntity, ContainerData data) {
        super(Automati.CRUSHER_MENU.get(), containerId, blockEntity, data, 2);

        addSlot(new SlotItemHandler(blockEntity.getInventory(), CrusherBlockEntity.INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y));
        // Product slot: take-only
        addSlot(new SlotItemHandler(blockEntity.getInventory(), CrusherBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
    }

    public boolean isCrushing() {
        return data.get(0) > 0;
    }

    // 0..1 fraction of the current operation completed
    public float getProgress() {
        int total = data.get(1);
        return total == 0 ? 0 : (float) data.get(0) / total;
    }

    @Override
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
            // Player inventory -> input slot; the slot itself asks the recipe
            // manager (server-side) whether the item is crushable
            if (!moveItemStackTo(stack, 0, 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return original;
    }
}
