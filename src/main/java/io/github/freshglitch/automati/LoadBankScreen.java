package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Load bank GUI: the -/+ rate buttons and the horizontal dump-rate gauge.
// Panel and Erg gauge come from the base class.
public class LoadBankScreen extends AbstractErgScreen<LoadBankMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/load_bank.png");

    // Rate fill: 40x12 at (176,0), drawn inside the rate gauge frame
    private static final int RATE_U = 176, RATE_V = 0, RATE_WIDTH = 40, RATE_HEIGHT = 12;
    private static final int RATE_X = 68, RATE_Y = 34;

    public LoadBankScreen(LoadBankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
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
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Dump-rate gauge fills left to right, green through red
        int rateFilled = Math.round(RATE_WIDTH * (float) menu.getDumpRate() / menu.getMaxRate());
        if (rateFilled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture(),
                leftPos + RATE_X, topPos + RATE_Y,
                RATE_U, RATE_V, rateFilled, RATE_HEIGHT, 256, 256);
        }

        // The number the whole machine exists for
        extractor.centeredText(font,
            Component.literal(String.format("%,d E/t", menu.getDumpRate())),
            leftPos + 88, topPos + 55, 0xFF9BD4A0);
    }
}
