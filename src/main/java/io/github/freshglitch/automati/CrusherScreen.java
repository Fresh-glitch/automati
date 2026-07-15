package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Crusher GUI: the progress arrow between input and output. Panel and Erg
// gauge come from the base class.
public class CrusherScreen extends AbstractErgScreen<CrusherMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/crusher.png");

    // Progress arrow fill: 24x16 at (176,0), drawn left-to-right over the engraved arrow
    private static final int ARROW_U = 176, ARROW_V = 0, ARROW_WIDTH = 24, ARROW_HEIGHT = 16;
    private static final int ARROW_X = 78, ARROW_Y = 35;

    public CrusherScreen(CrusherMenu menu, Inventory playerInventory, Component title) {
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
