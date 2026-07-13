package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Client-side rendering of the coal generator GUI: background panel,
// burn flame above the fuel slot, and the Erg gauge on the right.
public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(Automati.MODID, "textures/gui/container/coal_generator.png");

    // Flame sprite: 14x14 at (176,0) in the texture, drawn above the fuel slot
    private static final int FLAME_U = 176, FLAME_V = 0, FLAME_SIZE = 14;
    private static final int FLAME_X = 81, FLAME_Y = 35;
    // Energy fill: 16x50 at (176,31), drawn inside the gauge frame.
    // 50px for 100,000 E = one 5px ridge segment per 10,000 E.
    private static final int BAR_U = 176, BAR_V = 31, BAR_WIDTH = 16, BAR_HEIGHT = 50;
    private static final int BAR_X = 152, BAR_Y = 18;
    // Clog warning lamp: 8x8 at (192,0), shown above the ash slot when the bin is full
    private static final int LAMP_U = 192, LAMP_V = 0, LAMP_SIZE = 8;
    private static final int LAMP_X = 121, LAMP_Y = 38;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int left = leftPos;
        int top = topPos;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        // Flame burns down as the current piece of fuel is consumed
        if (menu.isBurning()) {
            int height = Math.max(1, Math.round(FLAME_SIZE * menu.getBurnProgress()));
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + FLAME_X, top + FLAME_Y + (FLAME_SIZE - height),
                FLAME_U, FLAME_V + (FLAME_SIZE - height),
                FLAME_SIZE, height, 256, 256);
        }

        // Energy gauge fills bottom-up
        int filled = Math.round(BAR_HEIGHT * (float) menu.getEnergy() / menu.getCapacity());
        if (filled > 0) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + BAR_X, top + BAR_Y + (BAR_HEIGHT - filled),
                BAR_U, BAR_V + (BAR_HEIGHT - filled),
                BAR_WIDTH, filled, 256, 256);
        }

        // Red warning lamp when the ash bin has choked the machine
        if (menu.isClogged()) {
            extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left + LAMP_X, top + LAMP_Y, LAMP_U, LAMP_V, LAMP_SIZE, LAMP_SIZE, 256, 256);
        }
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

        // Hovering the warning lamp explains the problem
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
