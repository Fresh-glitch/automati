package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The load bank block. It has no front: its coils wrap all four sides, and
// heat shimmers off the top while it burns energy away.
public class LoadBankBlock extends AbstractMachineBlock {

    public LoadBankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoadBankBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextDouble() < 0.4)
            riseFromTop(level, pos, random, ParticleTypes.WHITE_SMOKE, 0.2, 1.02, 0.05);
    }
}
