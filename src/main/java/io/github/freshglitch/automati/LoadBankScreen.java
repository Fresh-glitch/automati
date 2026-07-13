package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Client-side rendering of the load bank GUI: the -/+ rate buttons, the
// horizontal dump-rate gauge between them, and the Erg buffer on the right.
public class LoadBankScreen extends AbstractContainerScreen<LoadBankMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/load_bank.png");

    // Rate fill: 40x12 at (176,0), drawn inside the rate gauge frame
    private static final int RATE_U = 176, RATE_V = 0, RATE_WIDTH = 40, RATE_HEIGHT = 12;
    private static final int RATE_X = 68, RATE_Y = 34;
    // Buffer fill: 16x50 at (176,31), same visual language as the generator
    private static final int BAR_U = 176, BAR_V = 31, BAR_WIDTH = 16, BAR_HEIGHT = 50;
    private static final int BAR_X = 152, BAR_Y = 18;

    public LoadBankScreen(LoadBankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        // The buttons only *send* the click; the server does the actual adjusting
        addRenderableWidget(Button.builder(Component.literal("-"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LoadBankMenu.BUTTON_DECREASE))
            .bounds(leftPos + 48, topPos + 31, 16, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal("+"),
                b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LoadBankMenu.BUTTON_INCREASE))
            .bounds(leftPos + 112, topPos + 31, 16, 18)
            .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int left = leftPos;
        int top = topPos;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        // Dump-rate gauge fills left to right, green through red
        int rateFilled = Math.round(RATE_WIDTH * (float) menu.getDumpRate() / menu.getMaxRate());
        if (rateFilled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + RATE_X, top + RATE_Y,
                RATE_U, RATE_V, rateFilled, RATE_HEIGHT, 256, 256);
        }

        // Buffer gauge fills bottom-up
        int filled = Math.round(BAR_HEIGHT * (float) menu.getEnergy() / menu.getCapacity());
        if (filled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + BAR_X, top + BAR_Y + (BAR_HEIGHT - filled),
                BAR_U, BAR_V + (BAR_HEIGHT - filled),
                BAR_WIDTH, filled, 256, 256);
        }

        // The number the whole machine exists for
        extractor.centeredText(font,
            Component.literal(String.format("%,d E/t", menu.getDumpRate())),
            left + 88, top + 55, 0xFF707070);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        // Hovering the buffer shows the exact stored Ergs
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_WIDTH && mouseY >= barTop && mouseY < barTop + BAR_HEIGHT) {
            extractor.setTooltipForNextFrame(font,
                Component.literal(String.format("%,d / %,d E", menu.getEnergy(), menu.getCapacity())),
                mouseX, mouseY);
        }
    }
}
