package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

// Load bank container: no slots, just the dump rate and its two buttons
public class LoadBankMenu extends AbstractErgMenu<LoadBankBlockEntity> {
    public static final int BUTTON_DECREASE = 0;
    public static final int BUTTON_INCREASE = 1;

    // Client side
    public LoadBankMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, machineAt(playerInventory, buf));
    }

    public LoadBankMenu(int containerId, Inventory playerInventory, LoadBankBlockEntity loadBank) {
        super(Automati.LOAD_BANK.menu().get(), containerId, loadBank, 0);
        addPlayerInventory(playerInventory);
    }

    public int getDumpRate() {
        return machineValue(0);
    }

    // Runs on the server when the screen's - or + button is clicked
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_DECREASE && id != BUTTON_INCREASE)
            return false;
        machine.adjustDumpRate(id == BUTTON_INCREASE ? LoadBankBlockEntity.RATE_STEP : -LoadBankBlockEntity.RATE_STEP);
        return true;
    }
}
