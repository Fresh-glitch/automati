package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Base screen for Erg machines: draws the panel texture and the standard
// ridged Erg gauge (16x50 fill at texture (176,31), socket at (152,18)) with
// its hover tooltip. Subclasses draw their machine-specific elements in
// extractMachine() and add extra tooltips in extractExtraTooltips().
public abstract class AbstractErgScreen<T extends AbstractErgMenu> extends AbstractContainerScreen<T> {
    protected static final int BAR_U = 176, BAR_V = 31, BAR_WIDTH = 16, BAR_HEIGHT = 50;
    protected static final int BAR_X = 152, BAR_Y = 18;

    private final Identifier texture;

    protected AbstractErgScreen(T menu, Inventory playerInventory, Component title, Identifier texture) {
        super(menu, playerInventory, title);
        this.texture = texture;
    }

    protected Identifier texture() {
        return texture;
    }

    // Machine names sit top-center, the way vanilla's furnace and crafting
    // table do (the "Inventory" label stays left-aligned, also like vanilla)
    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        extractMachine(extractor, mouseX, mouseY, partialTick);

        // Energy gauge fills bottom-up
        int filled = Math.round(BAR_HEIGHT * (float) menu.getEnergy() / menu.getCapacity());
        if (filled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
                leftPos + BAR_X, topPos + BAR_Y + (BAR_HEIGHT - filled),
                BAR_U, BAR_V + (BAR_HEIGHT - filled),
                BAR_WIDTH, filled, 256, 256);
        }
    }

    // Machine-specific background elements: flames, arrows, rate bars...
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
    }

    // Machine-specific hover tooltips beyond the energy gauge
    protected void extractExtraTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        // Hovering the gauge shows the exact stored Ergs
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_WIDTH && mouseY >= barTop && mouseY < barTop + BAR_HEIGHT) {
            extractor.setTooltipForNextFrame(font,
                Component.literal(String.format("%,d / %,d E", menu.getEnergy(), menu.getCapacity())),
                mouseX, mouseY);
        }

        extractExtraTooltips(extractor, mouseX, mouseY);
    }
}
