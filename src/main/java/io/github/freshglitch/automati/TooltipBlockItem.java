package io.github.freshglitch.automati;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

// A block item with fixed tooltip lines under its name
public class TooltipBlockItem extends BlockItem {
    private final List<Component> lines;

    public TooltipBlockItem(Block block, Properties properties, Component... lines) {
        super(block, properties);
        this.lines = List.of(lines);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        lines.forEach(tooltip);
    }
}
