package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Coal generator GUI: burn flame above the fuel slot and the clog warning
// lamp. Panel and Erg gauge come from the base class.
public class CoalGeneratorScreen extends AbstractErgScreen<CoalGeneratorMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/coal_generator.png");

    // Flame sprite: 14x14 at (176,0) in the texture, drawn above the fuel slot
    private static final int FLAME_U = 176, FLAME_V = 0, FLAME_SIZE = 14;
    private static final int FLAME_X = 81, FLAME_Y = 35;
    // Clog warning lamp: 8x8 at (192,0), shown above the ash slot when the bin is full
    private static final int LAMP_U = 192, LAMP_V = 0, LAMP_SIZE = 8;
    private static final int LAMP_X = 121, LAMP_Y = 38;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Flame burns down as the current piece of fuel is consumed
        if (menu.isBurning()) {
            int height = Math.max(1, Math.round(FLAME_SIZE * menu.getBurnProgress()));
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture(),
                leftPos + FLAME_X, topPos + FLAME_Y + (FLAME_SIZE - height),
                FLAME_U, FLAME_V + (FLAME_SIZE - height),
                FLAME_SIZE, height, 256, 256);
        }

        // Red warning lamp when the ash bin has choked the machine
        if (menu.isClogged()) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, texture(),
                leftPos + LAMP_X, topPos + LAMP_Y, LAMP_U, LAMP_V, LAMP_SIZE, LAMP_SIZE, 256, 256);
        }
    }

    @Override
    protected void extractExtraTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int lampLeft = leftPos + LAMP_X;
        int lampTop = topPos + LAMP_Y;
        if (menu.isClogged()
                && mouseX >= lampLeft && mouseX < lampLeft + LAMP_SIZE
                && mouseY >= lampTop && mouseY < lampTop + LAMP_SIZE) {
            extractor.setTooltipForNextFrame(font,
                Component.translatable("gui.automati.coal_generator.clogged"),
                mouseX, mouseY);
        }
    }
}
