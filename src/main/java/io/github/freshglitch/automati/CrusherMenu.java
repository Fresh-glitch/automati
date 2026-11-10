package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.SlotItemHandler;

// Crusher container: input slot, output slot, crushing progress
public class CrusherMenu extends AbstractErgMenu<CrusherBlockEntity> {

    // Client side
    public CrusherMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, machineAt(playerInventory, buf));
    }

    public CrusherMenu(int containerId, Inventory playerInventory, CrusherBlockEntity crusher) {
        super(Automati.CRUSHER.menu().get(), containerId, crusher, 1);
        addSlot(new SlotItemHandler(crusher.getInventory(), MachineInventory.INPUT, 56, 35));
        addSlot(new OutputSlot(crusher.getInventory(), 116, 35));
        addPlayerInventory(playerInventory);
    }

    // 0..1: how far along the current item is
    public float getProgress() {
        return (float) machineValue(0) / CrusherBlockEntity.CRUSH_TICKS;
    }
}
