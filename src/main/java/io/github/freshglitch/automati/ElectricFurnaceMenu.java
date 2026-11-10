package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

// Electric furnace container: input slot, output slot, cooking progress and
// the smelt/blast mode
public class ElectricFurnaceMenu extends AbstractErgMenu<ElectricFurnaceBlockEntity> {
    public static final int BUTTON_TOGGLE_MODE = 0;

    // Client side
    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, machineAt(playerInventory, buf));
    }

    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, ElectricFurnaceBlockEntity furnace) {
        super(Automati.ELECTRIC_FURNACE.menu().get(), containerId, furnace, 1);
        addSlot(new SlotItemHandler(furnace.getInventory(), MachineInventory.INPUT, 56, 35));
        // taking the product pays out the banked smelting experience
        addSlot(new OutputSlot(furnace.getInventory(), 116, 35) {
            @Override
            public void onTake(Player player, ItemStack stack) {
                furnace.popExperience(player);
                super.onTake(player, stack);
            }
        });
        addPlayerInventory(playerInventory);
    }

    // 0..1: how far along the current item is
    public float getProgress() {
        return (float) machineValue(0) / ElectricFurnaceBlockEntity.SMELT_TICKS;
    }

    public boolean isBlastMode() {
        return machineValue(1) != 0;
    }

    // Runs on the server when the screen's mode button is clicked
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE_MODE)
            return false;
        machine.toggleBlastMode();
        return true;
    }
}
