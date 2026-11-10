package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

// A machine with a front, which faces the player when placed, like a furnace
public abstract class AbstractFacingMachineBlock extends AbstractMachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected AbstractFacingMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // An electric spark just off the front face, somewhere across its width
    // and between minY and minY + spreadY up the block
    protected static void frontSpark(BlockState state, Level level, BlockPos pos, RandomSource random,
                                     double minY, double spreadY) {
        Direction facing = state.getValue(FACING);
        double jitter = (random.nextDouble() - 0.5) * 0.5;
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getStepX() == 0 ? jitter : 0.0);
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getStepZ() == 0 ? jitter : 0.0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK,
            x, pos.getY() + minY + random.nextDouble() * spreadY, z, 0.0, 0.0, 0.0);
    }
}
