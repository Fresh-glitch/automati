package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Container logic for the load bank GUI: no machine slots, just the dump-rate
// buttons (vanilla menu-button channel) and the synced gauges. Energy sync,
// player inventory, and the open-race fix come from the base class.
public class LoadBankMenu extends AbstractErgMenu {
    public static final int BUTTON_DECREASE = 0;
    public static final int BUTTON_INCREASE = 1;

    private final LoadBankBlockEntity loadBank;

    // Client-side constructor: the block position arrives over the network
    public LoadBankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (LoadBankBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(3));
    }

    // Server-side constructor
    public LoadBankMenu(int containerId, Inventory playerInventory, LoadBankBlockEntity blockEntity, ContainerData data) {
        super(Automati.LOAD_BANK_MENU.get(), containerId, blockEntity, data, 1);
        this.loadBank = blockEntity;
        addPlayerInventory(playerInventory);
    }

    // The -/+ buttons land here, on the server
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_DECREASE) {
            loadBank.adjustDumpRate(-LoadBankBlockEntity.RATE_STEP);
            return true;
        }
        if (id == BUTTON_INCREASE) {
            loadBank.adjustDumpRate(LoadBankBlockEntity.RATE_STEP);
            return true;
        }
        return false;
    }

    public int getDumpRate() {
        return data.get(0);
    }

    @Override
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
}
