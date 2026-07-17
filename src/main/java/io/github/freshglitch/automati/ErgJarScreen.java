package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Erg Jar GUI: the big stored-charge readout and the live charge/discharge
// rates. Panel and Erg gauge come from the base class.
public class ErgJarScreen extends AbstractErgScreen<ErgJarMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/erg_jar.png");

    public ErgJarScreen(ErgJarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Stored charge as a percentage — the jar's headline number
        int percent = (int) Math.round(100.0 * menu.getEnergy() / menu.getCapacity());
        extractor.centeredText(font,
            Component.literal(percent + "% charged"),
            leftPos + 80, topPos + 32, 0xFF9BD4A0);

        // Live port traffic, same numbers the goggles show
        ErgJarBlockEntity jar = menu.getJar();
        if (jar != null) {
            extractor.centeredText(font,
                Component.literal(String.format("In %,d E/t", jar.getFlowInRate())),
                leftPos + 80, topPos + 48, 0xFF9BD4A0);
            extractor.centeredText(font,
                Component.literal(String.format("Out %,d E/t", jar.getFlowOutRate())),
                leftPos + 80, topPos + 60, 0xFF9BD4A0);
        }
    }
}
