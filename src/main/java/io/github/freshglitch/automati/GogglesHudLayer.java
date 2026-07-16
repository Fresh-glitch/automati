package io.github.freshglitch.automati;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

// The goggles HUD: while Engineer's Goggles are worn, looking at any block
// with an Erg buffer shows its charge right below the crosshair.
public class GogglesHudLayer implements ForgeLayer {
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 6;

    @Override
    public void extract(GuiGraphicsExtractor gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;
        if (!mc.player.getItemBySlot(EquipmentSlot.HEAD).is(Automati.ENGINEERS_GOGGLES.get()))
            return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;
        var blockEntity = mc.level.getBlockEntity(hit.getBlockPos());
        if (blockEntity == null)
            return;

        // item ducts have no energy port — the goggles show their cargo instead
        if (blockEntity instanceof ItemDuctBlockEntity duct) {
            int centerX = gg.guiWidth() / 2;
            int top = gg.guiHeight() / 2 + 12;
            gg.centeredText(mc.font,
                mc.level.getBlockState(hit.getBlockPos()).getBlock().getName(),
                centerX, top, 0xFFFFFFFF);

            var cargo = duct.getClientBuffer();
            if (cargo.isEmpty()) {
                gg.centeredText(mc.font, Component.literal("Empty"), centerX, top + 12, 0xFF9E9E9E);
            } else {
                String label = cargo.getCount() + " × " + cargo.getHoverName().getString();
                int labelWidth = mc.font.width(label);
                int rowLeft = centerX - (labelWidth + 20) / 2;
                gg.item(cargo, rowLeft, top + 10);
                gg.text(mc.font, Component.literal(label), rowLeft + 20, top + 14, 0xFFE0E0E0);
            }
            return;
        }

        blockEntity.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> {
            int max = storage.getMaxEnergyStored();
            if (max <= 0)
                return;
            int stored = Math.min(storage.getEnergyStored(), max);

            int centerX = gg.guiWidth() / 2;
            int top = gg.guiHeight() / 2 + 12;

            Component name = mc.level.getBlockState(hit.getBlockPos()).getBlock().getName();
            gg.centeredText(mc.font, name, centerX, top, 0xFFFFFFFF);

            int barLeft = centerX - BAR_WIDTH / 2;
            int barTop = top + 11;
            gg.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, 0xFF2B2B2B);
            int filled = (int) ((long) BAR_WIDTH * stored / max);
            if (filled > 0)
                gg.fill(barLeft, barTop, barLeft + filled, barTop + BAR_HEIGHT, 0xFFF5B52A);
            gg.centeredText(mc.font,
                Component.literal(String.format("%,d / %,d E", stored, max)),
                centerX, barTop + BAR_HEIGHT + 3, 0xFFE0E0E0);

            // live port traffic, when there is any: what's flowing in and out
            if (blockEntity instanceof AbstractErgBlockEntity erg
                    && (erg.getFlowInRate() > 0 || erg.getFlowOutRate() > 0)) {
                gg.centeredText(mc.font,
                    Component.literal(String.format("In %,d · Out %,d E/t",
                        erg.getFlowInRate(), erg.getFlowOutRate())),
                    centerX, barTop + BAR_HEIGHT + 14, 0xFF9BD4A0);
            }
        });
    }
}
