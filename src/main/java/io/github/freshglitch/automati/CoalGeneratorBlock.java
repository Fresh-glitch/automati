package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The coal generator block. While running it hums, smokes from the exhaust
// stack on top and throws sparks out of the front vent.
public class CoalGeneratorBlock extends AbstractFacingMachineBlock {

    public CoalGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoalGeneratorBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
        if (random.nextDouble() < 0.15)
            playAmbience(level, pos, Automati.COAL_GENERATOR_LOOP.get(), 0.6F, 0.9F + random.nextFloat() * 0.2F);
        if (random.nextDouble() < 0.25)
            riseFromTop(level, pos, random, ParticleTypes.SMOKE, 0.4, 1.02, 0.03);
        if (random.nextDouble() < 0.3)
            frontSpark(state, level, pos, random, 0.3, 0.35);
    }
}
