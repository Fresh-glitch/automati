package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Erg Jar GUI: the charge in percent and the live charge and discharge rates
public class ErgJarScreen extends AbstractErgScreen<ErgJarMenu> {

    public ErgJarScreen(ErgJarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "erg_jar");
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int percent = (int) Math.round(100.0 * menu.getEnergy() / menu.getCapacity());
        readout(extractor, percent + "% charged", 32);
        readout(extractor, String.format("In %,d E/t", menu.getFlowIn()), 48);
        readout(extractor, String.format("Out %,d E/t", menu.getFlowOut()), 60);
    }

    private void readout(GuiGraphicsExtractor extractor, String text, int y) {
        extractor.centeredText(font, text, leftPos + 80, topPos + y, GogglesHudLayer.READOUT_GREEN);
    }
}
