package io.github.freshglitch.automati;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// Coal generator GUI: the burn flame above the fuel slot and the clog lamp
// above the ash bin
public class CoalGeneratorScreen extends AbstractErgScreen<CoalGeneratorMenu> {
    // Flame sprite: 14x14 at texture (176,0)
    private static final int FLAME_X = 81, FLAME_Y = 35, FLAME_SIZE = 14;
    // Clog lamp sprite: 8x8 at texture (192,0)
    private static final int LAMP_X = 121, LAMP_Y = 38, LAMP_SIZE = 8;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "coal_generator");
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // the flame burns down with the piece of fuel
        if (menu.isBurning()) {
            int height = Math.max(1, Math.round(FLAME_SIZE * menu.getBurnProgress()));
            int burnt = FLAME_SIZE - height;
            sprite(extractor, FLAME_X, FLAME_Y + burnt, 176, burnt, FLAME_SIZE, height);
        }
        if (menu.isClogged())
            sprite(extractor, LAMP_X, LAMP_Y, 192, 0, LAMP_SIZE, LAMP_SIZE);
    }

    @Override
    protected void extractExtraTooltips(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        if (menu.isClogged() && isOver(LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE, mouseX, mouseY)) {
            extractor.setTooltipForNextFrame(font,
                Component.translatable("gui.automati.coal_generator.clogged"), mouseX, mouseY);
        }
    }
}
