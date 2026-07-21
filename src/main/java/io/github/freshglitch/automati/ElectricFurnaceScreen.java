package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Electric furnace GUI: the progress arrow and the smelt/blast mode toggle —
// a socket in the top-left corner holding a literal furnace (or blast
// furnace) icon; click it to switch which process the machine runs.
// Panel and Erg gauge come from the base class.
public class ElectricFurnaceScreen extends AbstractErgScreen<ElectricFurnaceMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/electric_furnace.png");

    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_WIDTH = 24, ARROW_HEIGHT = 16;
    private static final int ARROW_X = 78, ARROW_Y = 35;
    // 16x16 icon inside the 18x18 socket the GUI texture draws at (6,14)
    private static final int MODE_X = 7, MODE_Y = 15;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    private boolean isOverModeButton(double mouseX, double mouseY) {
        return mouseX >= leftPos + MODE_X && mouseX < leftPos + MODE_X + 16
            && mouseY >= topPos + MODE_Y && mouseY < topPos + MODE_Y + 16;
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int arrowFilled = Math.round(ARROW_WIDTH * menu.getProgress());
        if (arrowFilled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture(),
                leftPos + ARROW_X, topPos + ARROW_Y,
                ARROW_U, ARROW_V, arrowFilled, ARROW_HEIGHT, 256, 256);
        }

        // the mode icon IS the block the machine is imitating right now
        extractor.item(new net.minecraft.world.item.ItemStack(
                menu.isBlastMode() ? net.minecraft.world.item.Items.BLAST_FURNACE : net.minecraft.world.item.Items.FURNACE),
            leftPos + MODE_X, topPos + MODE_Y);
        if (isOverModeButton(mouseX, mouseY)) {
            // vanilla slot-hover wash
            extractor.fill(leftPos + MODE_X, topPos + MODE_Y,
                leftPos + MODE_X + 16, topPos + MODE_Y + 16, 0x80FFFFFF);
        }
    }

    @Override
    protected void extractExtraTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        if (isOverModeButton(mouseX, mouseY)) {
            extractor.setTooltipForNextFrame(font,
                Component.translatable(menu.isBlastMode()
                    ? "gui.automati.electric_furnace.mode.blast"
                    : "gui.automati.electric_furnace.mode.smelt"),
                mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (event.button() == 0 && isOverModeButton(event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ElectricFurnaceMenu.BUTTON_TOGGLE_MODE);
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubled);
    }
}
