package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Base screen for Erg machines: draws the panel, the Erg gauge with its hover
// tooltip, and a centered title. Subclasses add their own elements in
// extractMachine() and their own tooltips in extractExtraTooltips().
//
// Every GUI texture shares one layout: the 176x166 panel at (0,0), the gauge
// fill (16x50) at (176,31) and, for machines with a progress arrow, its fill
// (24x16) at (176,0).
public abstract class AbstractErgScreen<T extends AbstractErgMenu<?>> extends AbstractContainerScreen<T> {
    private static final int GAUGE_X = 152, GAUGE_Y = 18, GAUGE_WIDTH = 16, GAUGE_HEIGHT = 50;
    private static final int ARROW_X = 78, ARROW_Y = 35, ARROW_WIDTH = 24, ARROW_HEIGHT = 16;

    private final Identifier texture;

    // textureName: the file under textures/gui/container/, without extension
    protected AbstractErgScreen(T menu, Inventory playerInventory, Component title, String textureName) {
        super(menu, playerInventory, title);
        this.texture = Automati.id("textures/gui/container/" + textureName + ".png");
    }

    // Titles sit top-center like on vanilla's furnace; "Inventory" stays left
    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        sprite(extractor, 0, 0, 0, 0, imageWidth, imageHeight);
        extractMachine(extractor, mouseX, mouseY, partialTick);

        // the gauge fills from the bottom up
        int filled = Math.round(GAUGE_HEIGHT * (float) menu.getEnergy() / menu.getCapacity());
        if (filled > 0) {
            int empty = GAUGE_HEIGHT - filled;
            sprite(extractor, GAUGE_X, GAUGE_Y + empty, 176, 31 + empty, GAUGE_WIDTH, filled);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        if (isOver(GAUGE_X, GAUGE_Y, GAUGE_WIDTH, GAUGE_HEIGHT, mouseX, mouseY)) {
            extractor.setTooltipForNextFrame(font,
                Component.literal(String.format("%,d / %,d E", menu.getEnergy(), menu.getCapacity())),
                mouseX, mouseY);
        }
        extractExtraTooltips(extractor, mouseX, mouseY);
    }

    // Machine-specific background elements: flames, arrows, rate bars...
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
    }

    // Machine-specific hover tooltips
    protected void extractExtraTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
    }

    // Draws a piece of the GUI texture at a position relative to the panel
    protected void sprite(GuiGraphicsExtractor extractor, int x, int y, int u, int v, int width, int height) {
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + x, topPos + y, u, v, width, height, 256, 256);
    }

    // Fills the progress arrow from the left; progress runs 0..1
    protected void progressArrow(GuiGraphicsExtractor extractor, float progress) {
        int filled = Math.round(ARROW_WIDTH * progress);
        if (filled > 0)
            sprite(extractor, ARROW_X, ARROW_Y, 176, 0, filled, ARROW_HEIGHT);
    }

    // Whether the mouse is over a rectangle given relative to the panel
    protected boolean isOver(int x, int y, int width, int height, double mouseX, double mouseY) {
        double relativeX = mouseX - leftPos;
        double relativeY = mouseY - topPos;
        return relativeX >= x && relativeX < x + width && relativeY >= y && relativeY < y + height;
    }
}
