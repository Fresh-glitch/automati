package io.github.freshglitch.automati;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

// Base container for Erg machines: the initial-sync race protection, the
// split 32-bit energy getter, distance-based stillValid, and the standard
// player inventory rows. Subclasses add their machine slots, quick-move
// rules, and any extra synced values.
public abstract class AbstractErgMenu extends AbstractContainerMenu {
    protected final BlockEntity blockEntity;
    protected final ContainerData data;
    private final int energyLowIndex;

    private int initialResyncTicks = 5;

    protected AbstractErgMenu(MenuType<?> menuType, int containerId, BlockEntity blockEntity,
                              ContainerData data, int energyLowIndex) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.energyLowIndex = energyLowIndex;
        addDataSlots(data);
    }

    // Standard player inventory (3 rows + hotbar) at the standard GUI offsets
    protected void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    // The initial container-data sync can race the menu-open packet, leaving
    // the client with zeroed gauges. Re-send the full state for the first few
    // ticks so screens always open with real values.
    @Override
    public void broadcastChanges() {
        if (initialResyncTicks > 0) {
            initialResyncTicks--;
            broadcastFullState();
        } else {
            super.broadcastChanges();
        }
    }

    // Reassemble the 32-bit energy value from its two synced 16-bit halves
    public int getEnergy() {
        return ((data.get(energyLowIndex + 1) & 0xFFFF) << 16) | (data.get(energyLowIndex) & 0xFFFF);
    }

    public abstract int getCapacity();

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
            && player.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos())) <= 64.0;
    }
}
