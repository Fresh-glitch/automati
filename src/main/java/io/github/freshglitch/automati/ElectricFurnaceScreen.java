package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Electric furnace GUI: the progress arrow. Panel and Erg gauge come from
// the base class.
public class ElectricFurnaceScreen extends AbstractErgScreen<ElectricFurnaceMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/electric_furnace.png");

    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_WIDTH = 24, ARROW_HEIGHT = 16;
    private static final int ARROW_X = 78, ARROW_Y = 35;

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int arrowFilled = Math.round(ARROW_WIDTH * menu.getProgress());
        if (arrowFilled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture(),
                leftPos + ARROW_X, topPos + ARROW_Y,
                ARROW_U, ARROW_V, arrowFilled, ARROW_HEIGHT, 256, 256);
        }
    }
}
