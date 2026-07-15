package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.SlotItemHandler;

// Container logic for the coal generator GUI: fuel slot, ash byproduct slot,
// burn and clog state. Energy sync, player inventory, and the open-race fix
// come from the base class.
public class CoalGeneratorMenu extends AbstractErgMenu {
    public static final int FUEL_SLOT_X = 80;
    public static final int FUEL_SLOT_Y = 53;
    public static final int ASH_SLOT_X = 116;
    public static final int ASH_SLOT_Y = 53;

    // Client-side constructor: the block position arrives over the network
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (CoalGeneratorBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(5));
    }

    // Server-side constructor
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, CoalGeneratorBlockEntity blockEntity, ContainerData data) {
        super(Automati.COAL_GENERATOR_MENU.get(), containerId, blockEntity, data, 2);

        addSlot(new SlotItemHandler(blockEntity.getInventory(), CoalGeneratorBlockEntity.FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));
        // Byproduct slot: the player can take ash out but never put anything in
        addSlot(new SlotItemHandler(blockEntity.getInventory(), CoalGeneratorBlockEntity.ASH_SLOT, ASH_SLOT_X, ASH_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(playerInventory);
    }

    public boolean isBurning() {
        return data.get(0) > 0;
    }

    // 0..1 fraction of the current piece of fuel remaining
    public float getBurnProgress() {
        int duration = data.get(1);
        return duration == 0 ? 0 : (float) data.get(0) / duration;
    }

    @Override
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
}
