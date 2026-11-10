package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Load bank GUI: the - and + buttons either side of the dump-rate gauge
public class LoadBankScreen extends AbstractErgScreen<LoadBankMenu> {
    // Rate gauge fill: 40x12 at texture (176,0)
    private static final int RATE_X = 68, RATE_Y = 34, RATE_WIDTH = 40, RATE_HEIGHT = 12;

    public LoadBankScreen(LoadBankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "load_bank");
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(rateButton("-", LoadBankMenu.BUTTON_DECREASE, 48));
        addRenderableWidget(rateButton("+", LoadBankMenu.BUTTON_INCREASE, 112));
    }

    // The click only notifies the server, which adjusts the rate and syncs it back
    private Button rateButton(String label, int buttonId, int x) {
        return Button.builder(Component.literal(label),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId))
            .bounds(leftPos + x, topPos + 31, 16, 18)
            .build();
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int filled = Math.round(RATE_WIDTH * (float) menu.getDumpRate() / LoadBankBlockEntity.MAX_RATE);
        if (filled > 0)
            sprite(extractor, RATE_X, RATE_Y, 176, 0, filled, RATE_HEIGHT);

        extractor.centeredText(font, String.format("%,d E/t", menu.getDumpRate()),
            leftPos + 88, topPos + 55, GogglesHudLayer.READOUT_GREEN);
    }
}
