package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Factory casing that smooths out when it is part of a continuous floor:
// a casing surrounded by casings on all four horizontal sides swaps to the
// seamless plate model, so floor interiors render smooth while edges, walls
// and lone blocks keep the riveted plating.
public class FactoryBlock extends Block {
    public static final BooleanProperty SMOOTH = BooleanProperty.create("smooth");

    public FactoryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SMOOTH, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SMOOTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(SMOOTH, isInterior(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // Only horizontal neighbours influence whether this casing counts as flooring
        if (direction.getAxis().isHorizontal())
            return state.setValue(SMOOTH, isInterior(level, pos));
        return state;
    }

    private boolean isInterior(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(dir)).is(this))
                return false;
        }
        return true;
    }
}
