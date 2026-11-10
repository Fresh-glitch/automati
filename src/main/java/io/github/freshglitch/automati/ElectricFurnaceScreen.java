package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

// Electric furnace GUI: the progress arrow, and in the top-left corner a
// button showing the furnace the machine currently imitates. Clicking it
// switches between smelting and blasting.
public class ElectricFurnaceScreen extends AbstractErgScreen<ElectricFurnaceMenu> {
    // Mode button: 16x16, sprites at texture (200,0) furnace and (216,0) blast furnace
    private static final int MODE_X = 5, MODE_Y = 5, MODE_SIZE = 16;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "electric_furnace");
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        progressArrow(extractor, menu.getProgress());

        sprite(extractor, MODE_X, MODE_Y, menu.isBlastMode() ? 216 : 200, 0, MODE_SIZE, MODE_SIZE);
        if (isOver(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
            // brighten the button face on hover
            extractor.fill(leftPos + MODE_X + 1, topPos + MODE_Y + 1,
                leftPos + MODE_X + MODE_SIZE - 1, topPos + MODE_Y + MODE_SIZE - 1, 0x40FFFFFF);
        }
    }

    @Override
    protected void extractExtraTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        if (isOver(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
            extractor.setTooltipForNextFrame(font,
                Component.translatable("gui.automati.electric_furnace.mode." + (menu.isBlastMode() ? "blast" : "smelt")),
                mouseX, mouseY);
        }
    }

    // The click only notifies the server, which flips the mode and syncs it back
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && isOver(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ElectricFurnaceMenu.BUTTON_TOGGLE_MODE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubled);
    }
}
