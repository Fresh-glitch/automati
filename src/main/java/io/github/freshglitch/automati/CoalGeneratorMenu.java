package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.SlotItemHandler;

// Coal generator container: a fuel slot, the ash bin, burn and clog state
public class CoalGeneratorMenu extends AbstractErgMenu<CoalGeneratorBlockEntity> {

    // Client side
    public CoalGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, machineAt(playerInventory, buf));
    }

    public CoalGeneratorMenu(int containerId, Inventory playerInventory, CoalGeneratorBlockEntity generator) {
        super(Automati.COAL_GENERATOR.menu().get(), containerId, generator, 1);
        addSlot(new SlotItemHandler(generator.getInventory(), MachineInventory.INPUT, 80, 53));
        addSlot(new OutputSlot(generator.getInventory(), 116, 53));
        addPlayerInventory(playerInventory);
    }

    public boolean isBurning() {
        return machineValue(0) > 0;
    }

    // 0..1: how much of the burning piece is left
    public float getBurnProgress() {
        return (float) machineValue(0) / CoalGeneratorBlockEntity.BURN_TICKS_PER_FUEL;
    }

    public boolean isClogged() {
        return machineValue(1) != 0;
    }
}
