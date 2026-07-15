package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

// The coal generator block: faces the player like a furnace and carries the
// running ambience. Ticking, LIT state, and menu opening come from the base.
public class CoalGeneratorBlock extends AbstractMachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CoalGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the player when placed, like a furnace
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoalGeneratorBlockEntity(pos, state);
    }

    // Client-side ambience while the generator is running: a mechanical hum,
    // smoke rising from the exhaust stack, and electric sparks out of the vent
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.15) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Automati.COAL_GENERATOR_LOOP.get(), SoundSource.BLOCKS,
                0.6F, 0.9F + random.nextFloat() * 0.2F, false);
        }

        // smoke rises out of the exhaust stack in the middle of the top face
        if (random.nextDouble() < 0.25) {
            level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.4 + random.nextDouble() * 0.2,
                pos.getY() + 1.02,
                pos.getZ() + 0.4 + random.nextDouble() * 0.2,
                0.0, 0.03, 0.0);
        }

        Direction facing = state.getValue(FACING);
        if (random.nextDouble() < 0.3) {
            double jitter = (random.nextDouble() - 0.5) * 0.5;
            double px = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getStepX() == 0 ? jitter : 0.0);
            double pz = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getStepZ() == 0 ? jitter : 0.0);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                px, pos.getY() + 0.3 + random.nextDouble() * 0.35, pz,
                0.0, 0.0, 0.0);
        }
    }
}
