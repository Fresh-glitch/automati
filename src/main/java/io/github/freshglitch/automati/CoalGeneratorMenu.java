package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.SlotItemHandler;

// Server<->client container logic for the coal generator GUI:
// one fuel slot plus the standard 36 player inventory slots.
public class CoalGeneratorMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT_X = 80;
    public static final int FUEL_SLOT_Y = 53;
    public static final int ASH_SLOT_X = 116;
    public static final int ASH_SLOT_Y = 53;

    private final CoalGeneratorBlockEntity blockEntity;
    private final ContainerData data;

    // Client-side constructor: the block position arrives over the network
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (CoalGeneratorBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(5));
    }

    // Server-side constructor
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, CoalGeneratorBlockEntity blockEntity, ContainerData data) {
        super(Automati.COAL_GENERATOR_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getInventory(), CoalGeneratorBlockEntity.FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));
        // Byproduct slot: the player can take ash out but never put anything in
        addSlot(new SlotItemHandler(blockEntity.getInventory(), CoalGeneratorBlockEntity.ASH_SLOT, ASH_SLOT_X, ASH_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        // Hotbar
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    // The initial container-data sync can race the menu-open packet, leaving the
    // client showing zeroed gauges on an idle generator until something changes.
    // Re-send the full state for the first few ticks so the screen always opens
    // with real values.
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

    public boolean isBurning() {
        return data.get(0) > 0;
    }

    // 0..1 fraction of the current piece of fuel remaining
    public float getBurnProgress() {
        int duration = data.get(1);
        return duration == 0 ? 0 : (float) data.get(0) / duration;
    }

    // Reassemble the 32-bit energy value from its two synced 16-bit halves
    public int getEnergy() {
        return ((data.get(3) & 0xFFFF) << 16) | (data.get(2) & 0xFFFF);
    }

    public int getCapacity() {
        return CoalGeneratorBlockEntity.CAPACITY;
    }

    public boolean isClogged() {
        return data.get(4) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index <= 1) {
            // Fuel or ash slot -> player inventory
            if (!moveItemStackTo(stack, 2, 38, true))
                return ItemStack.EMPTY;
        } else {
            // Player inventory -> fuel slot (only coal/charcoal will fit)
            if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
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
