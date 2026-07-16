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

// The electric furnace block: faces the player, hums electrically, and shows
// its glowing elements through the front window while smelting.
public class ElectricFurnaceBlock extends AbstractMachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ElectricFurnaceBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    // Electric ambience: the generator hum pitched up into a resistive whine,
    // plus sparks at the element window
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.12) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Automati.COAL_GENERATOR_LOOP.get(), SoundSource.BLOCKS,
                0.35F, 1.5F + random.nextFloat() * 0.1F, false);
        }

        Direction facing = state.getValue(FACING);
        if (random.nextDouble() < 0.2) {
            double jitter = (random.nextDouble() - 0.5) * 0.5;
            double px = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getStepX() == 0 ? jitter : 0.0);
            double pz = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getStepZ() == 0 ? jitter : 0.0);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                px, pos.getY() + 0.35 + random.nextDouble() * 0.3, pz,
                0.0, 0.0, 0.0);
        }
    }
}
