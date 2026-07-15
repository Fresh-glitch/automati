package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The load bank block: no facing (its coils wrap all four sides). Ticking,
// LIT state, and menu opening come from the base.
public class LoadBankBlock extends AbstractMachineBlock {

    public LoadBankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoadBankBlockEntity(pos, state);
    }

    // Heat shimmer rising off the block while it burns energy away
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.4) {
            level.addParticle(ParticleTypes.WHITE_SMOKE,
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 1.02,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0.0, 0.05, 0.0);
        }
    }
}
