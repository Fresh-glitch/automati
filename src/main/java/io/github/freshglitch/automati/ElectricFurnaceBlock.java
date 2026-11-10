package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The electric furnace block. While cooking it whines (the generator hum,
// pitched up) and sparks at the element window in its front.
public class ElectricFurnaceBlock extends AbstractFacingMachineBlock {

    public ElectricFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
        if (random.nextDouble() < 0.12)
            playAmbience(level, pos, Automati.COAL_GENERATOR_LOOP.get(), 0.35F, 1.5F + random.nextFloat() * 0.1F);
        if (random.nextDouble() < 0.2)
            frontSpark(state, level, pos, random, 0.35, 0.3);
    }
}
