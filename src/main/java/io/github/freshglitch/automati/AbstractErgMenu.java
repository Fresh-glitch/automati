package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.SlotItemHandler;

// Base container for Erg machines. Mirrors the machine's menu values (energy
// first), re-sends them while the menu opens, lays out the player inventory
// and handles shift-click. A subclass adds its machine slots, then calls
// addPlayerInventory().
public abstract class AbstractErgMenu<T extends AbstractErgBlockEntity> extends AbstractContainerMenu {
    protected final T machine;
    private final ContainerData data;
    private final int inputSlots;

    private int machineSlots;
    private int initialResyncTicks = 5;

    // inputSlots: how many of the leading machine slots shift-click may fill
    protected AbstractErgMenu(MenuType<?> menuType, int containerId, T machine, int inputSlots) {
        super(menuType, containerId);
        this.machine = machine;
        this.inputSlots = inputSlots;
        // the server reads the live values; the client mirrors them in an
        // array of the same size
        ContainerData live = machine.menuData();
        this.data = machine.getLevel().isClientSide() ? new SimpleContainerData(live.getCount()) : live;
        addDataSlots(data);
    }

    // Client side: the machine's position arrives with the open-menu packet
    @SuppressWarnings("unchecked")
    protected static <T extends AbstractErgBlockEntity> T machineAt(Inventory playerInventory, FriendlyByteBuf buf) {
        return (T) playerInventory.player.level().getBlockEntity(buf.readBlockPos());
    }

    // The machine's output slot: players and shift-click can only take from it
    protected static class OutputSlot extends SlotItemHandler {
        public OutputSlot(MachineInventory inventory, int x, int y) {
            super(inventory, MachineInventory.OUTPUT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    // Three rows and the hotbar at the standard GUI offsets
    protected void addPlayerInventory(Inventory playerInventory) {
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    // The first data sync can race the open-menu packet and leave the client
    // with zeroed gauges, so the full state is re-sent for the first few ticks
    @Override
    public void broadcastChanges() {
        if (initialResyncTicks > 0) {
            initialResyncTicks--;
            broadcastFullState();
        } else {
            super.broadcastChanges();
        }
    }

    // Data slots arrive as signed 16-bit values, hence the masks
    public int getEnergy() {
        return ((data.get(1) & 0xFFFF) << 16) | (data.get(0) & 0xFFFF);
    }

    public int getCapacity() {
        return machine.energy.getMaxEnergyStored();
    }

    // One of the values the machine registered with syncToMenu(), in order
    protected int machineValue(int index) {
        return data.get(2 + index);
    }

    // Machine slots empty into the player inventory and the player inventory
    // feeds the input slots. A menu without machine slots shuffles between
    // the main inventory and the hotbar instead.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int hotbar = machineSlots + 27;
        int end = machineSlots + 36;

        boolean moved;
        if (index < machineSlots)
            moved = moveItemStackTo(stack, machineSlots, end, true);
        else if (machineSlots > 0)
            moved = moveItemStackTo(stack, 0, inputSlots, false);
        else if (index < hotbar)
            moved = moveItemStackTo(stack, hotbar, end, false);
        else
            moved = moveItemStackTo(stack, 0, hotbar, false);
        if (!moved)
            return ItemStack.EMPTY;

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();
        // as in vanilla: a slot that reacts to being emptied also reacts here
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return !machine.isRemoved()
            && player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0;
    }
}
