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

// Container logic for the load bank GUI. No machine slots — just the player
// inventory, the synced gauges, and the -/+ buttons that arrive as menu button
// clicks (the same channel the stonecutter and lectern use).
public class LoadBankMenu extends AbstractContainerMenu {
    public static final int BUTTON_DECREASE = 0;
    public static final int BUTTON_INCREASE = 1;

    private final LoadBankBlockEntity blockEntity;
    private final ContainerData data;

    // Client-side constructor: the block position arrives over the network
    public LoadBankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (LoadBankBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(3));
    }

    // Server-side constructor
    public LoadBankMenu(int containerId, Inventory playerInventory, LoadBankBlockEntity blockEntity, ContainerData data) {
        super(Automati.LOAD_BANK_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        // Player inventory (3 rows of 9) + hotbar
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    // Same initial-sync race protection as the coal generator
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

    // The -/+ buttons land here, on the server
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_DECREASE) {
            blockEntity.adjustDumpRate(-LoadBankBlockEntity.RATE_STEP);
            return true;
        }
        if (id == BUTTON_INCREASE) {
            blockEntity.adjustDumpRate(LoadBankBlockEntity.RATE_STEP);
            return true;
        }
        return false;
    }

    public int getDumpRate() {
        return data.get(0);
    }

    public int getEnergy() {
        return ((data.get(2) & 0xFFFF) << 16) | (data.get(1) & 0xFFFF);
    }

    public int getCapacity() {
        return LoadBankBlockEntity.CAPACITY;
    }

    public int getMaxRate() {
        return LoadBankBlockEntity.MAX_RATE;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // No machine slots, so shift-click just shuttles between inventory and hotbar
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false))
                return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 27, false))
                return ItemStack.EMPTY;
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
