package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Container logic for the Erg Jar GUI: no machine slots, no buttons — just
// the synced charge gauge. Energy sync, player inventory, and the open-race
// fix come from the base class.
public class ErgJarMenu extends AbstractErgMenu {

    private final ErgJarBlockEntity jar;

    // Client-side constructor: the block position arrives over the network
    public ErgJarMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (ErgJarBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(2));
    }

    // Server-side constructor
    public ErgJarMenu(int containerId, Inventory playerInventory, ErgJarBlockEntity blockEntity, ContainerData data) {
        super(Automati.ERG_JAR_MENU.get(), containerId, blockEntity, data, 0);
        this.jar = blockEntity;
        addPlayerInventory(playerInventory);
    }

    // The screen reads live flow rates straight off the client block entity
    // (they ride the same update tags that feed the goggles HUD)
    public ErgJarBlockEntity getJar() {
        return jar;
    }

    @Override
    public int getCapacity() {
        return ErgJarBlockEntity.CAPACITY;
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
