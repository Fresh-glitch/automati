package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

// Container logic for the electric furnace GUI: input, output (which pays out
// banked smelting experience when taken), and progress.
public class ElectricFurnaceMenu extends AbstractErgMenu {
    public static final int INPUT_SLOT_X = 56;
    public static final int INPUT_SLOT_Y = 35;
    public static final int OUTPUT_SLOT_X = 116;
    public static final int OUTPUT_SLOT_Y = 35;
    public static final int BUTTON_TOGGLE_MODE = 0;

    private final ElectricFurnaceBlockEntity furnace;

    // Client-side constructor: the block position arrives over the network
    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
            (ElectricFurnaceBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
            new SimpleContainerData(5));
    }

    // Server-side constructor
    public ElectricFurnaceMenu(int containerId, Inventory playerInventory, ElectricFurnaceBlockEntity blockEntity, ContainerData data) {
        super(Automati.ELECTRIC_FURNACE_MENU.get(), containerId, blockEntity, data, 2);
        this.furnace = blockEntity;

        addSlot(new SlotItemHandler(blockEntity.getInventory(), ElectricFurnaceBlockEntity.INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y));
        // Product slot: take-only, and taking collects the banked experience
        addSlot(new SlotItemHandler(blockEntity.getInventory(), ElectricFurnaceBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                furnace.popExperience(player);
                super.onTake(player, stack);
            }
        });

        addPlayerInventory(playerInventory);
    }

    public boolean isSmelting() {
        return data.get(0) > 0;
    }

    public boolean isBlastMode() {
        return data.get(4) != 0;
    }

    // The mode button lands here, on the server
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_MODE) {
            furnace.toggleBlastMode();
            return true;
        }
        return false;
    }

    // 0..1 fraction of the current smelt completed
    public float getProgress() {
        int total = data.get(1);
        return total == 0 ? 0 : (float) data.get(0) / total;
    }

    @Override
    public int getCapacity() {
        return ElectricFurnaceBlockEntity.CAPACITY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index <= 1) {
            if (!moveItemStackTo(stack, 2, 38, true))
                return ItemStack.EMPTY;
        } else {
            // the slot itself asks the recipe manager (server-side) what's smeltable
            if (!moveItemStackTo(stack, 0, 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return original;
    }
}
