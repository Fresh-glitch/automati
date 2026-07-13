package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Client-side rendering of the crusher GUI: progress arrow between the two
// slots and the standard Erg gauge on the right.
public class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/crusher.png");

    // Progress arrow fill: 24x16 at (176,0), drawn left-to-right over the engraved arrow
    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_WIDTH = 24, ARROW_HEIGHT = 16;
    private static final int ARROW_X = 78, ARROW_Y = 35;
    // Energy fill: 16x50 at (176,31); 50px for 20,000 E = one ridge per 2,000 E
    private static final int BAR_U = 176, BAR_V = 31, BAR_WIDTH = 16, BAR_HEIGHT = 50;
    private static final int BAR_X = 152, BAR_Y = 18;

    public CrusherScreen(CrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int left = leftPos;
        int top = topPos;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        // Progress arrow sweeps toward the output slot
        int arrowFilled = Math.round(ARROW_WIDTH * menu.getProgress());
        if (arrowFilled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + ARROW_X, top + ARROW_Y,
                ARROW_U, ARROW_V, arrowFilled, ARROW_HEIGHT, 256, 256);
        }

        // Energy gauge fills bottom-up
        int filled = Math.round(BAR_HEIGHT * (float) menu.getEnergy() / menu.getCapacity());
        if (filled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + BAR_X, top + BAR_Y + (BAR_HEIGHT - filled),
                BAR_U, BAR_V + (BAR_HEIGHT - filled),
                BAR_WIDTH, filled, 256, 256);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_WIDTH && mouseY >= barTop && mouseY < barTop + BAR_HEIGHT) {
            extractor.setTooltipForNextFrame(font,
                Component.literal(String.format("%,d / %,d E", menu.getEnergy(), menu.getCapacity())),
                mouseX, mouseY);
        }
    }
}
