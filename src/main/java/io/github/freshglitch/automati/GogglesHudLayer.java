package io.github.freshglitch.automati;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

// The goggles HUD: while Engineer's Goggles are worn, looking at a block with
// an Erg buffer shows its charge below the crosshair, and looking at an item
// duct shows its cargo.
public class GogglesHudLayer implements ForgeLayer {
    // Also the colour of the live readouts in the machine screens
    public static final int READOUT_GREEN = 0xFF9BD4A0;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF9E9E9E;
    private static final int BAR_BACK = 0xFF2B2B2B;
    private static final int BAR_FILL = 0xFFF5B52A;
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 6;

    @Override
    public void extract(GuiGraphicsExtractor extractor, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(Automati.ENGINEERS_GOGGLES.get())
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK)
            return;
        BlockEntity blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        if (blockEntity == null)
            return;

        Font font = minecraft.font;
        Component name = blockEntity.getBlockState().getBlock().getName();
        int centerX = extractor.guiWidth() / 2;
        int top = extractor.guiHeight() / 2 + 12;

        if (blockEntity instanceof ItemDuctBlockEntity duct) {
            extractor.centeredText(font, name, centerX, top, WHITE);
            cargo(extractor, font, duct.getClientBuffer(), centerX, top);
            return;
        }

        IEnergyStorage storage = blockEntity.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (storage == null || storage.getMaxEnergyStored() <= 0)
            return;
        extractor.centeredText(font, name, centerX, top, WHITE);
        int textTop = chargeBar(extractor, font, storage, centerX, top + 11);

        // Live port traffic, when there is any
        if (blockEntity instanceof AbstractErgBlockEntity erg && (erg.getFlowInRate() > 0 || erg.getFlowOutRate() > 0))
            extractor.centeredText(font,
                String.format("In %,d \u00B7 Out %,d E/t", erg.getFlowInRate(), erg.getFlowOutRate()),
                centerX, textTop + 11, READOUT_GREEN);
    }

    private static void cargo(GuiGraphicsExtractor extractor, Font font, ItemStack cargo, int centerX, int top) {
        if (cargo.isEmpty()) {
            extractor.centeredText(font, "Empty", centerX, top + 12, DIM);
            return;
        }
        String label = cargo.getCount() + " \u00D7 " + cargo.getHoverName().getString();
        int left = centerX - (font.width(label) + 20) / 2;
        extractor.item(cargo, left, top + 10);
        extractor.text(font, Component.literal(label), left + 20, top + 14, TEXT);
    }

    // Draws the bar and the stored / capacity line; returns that line's y
    private static int chargeBar(GuiGraphicsExtractor extractor, Font font, IEnergyStorage storage, int centerX, int barTop) {
        int max = storage.getMaxEnergyStored();
        int stored = Math.min(storage.getEnergyStored(), max);
        int barLeft = centerX - BAR_WIDTH / 2;
        int filled = (int) ((long) BAR_WIDTH * stored / max);

        extractor.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, BAR_BACK);
        if (filled > 0)
            extractor.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, BAR_FILL);

        int textTop = barTop + BAR_HEIGHT + 3;
        extractor.centeredText(font, String.format("%,d / %,d E", stored, max), centerX, textTop, TEXT);
        return textTop;
    }
}
