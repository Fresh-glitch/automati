package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

// Base for the six-way conduits (Erg cable, item duct). One boolean property
// per side drives both the multipart model and the shape. A side connects
// when the neighbour offers the conduit's capability on the facing side and
// the conduit's own policy, possibly inverted with the wrench, agrees.
public abstract class AbstractConduitBlock extends Block implements EntityBlock {
    // Indexed by Direction.ordinal()
    private static final BooleanProperty[] SIDES = {
        BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST };
    private static final Direction[] DIRECTIONS = Direction.values();

    // Half-widths of the 6px core and the 4px arms, in blocks. The wrench
    // hit-tests clicks against the same numbers.
    public static final double CORE_HALF = 3 / 16.0;
    public static final double ARM_HALF = 2 / 16.0;

    // One shape per combination of connected sides; bit n is the side with ordinal n
    private static final VoxelShape[] SHAPES = buildShapes();

    private final Capability<?> link;

    // link: the capability a neighbour must offer for a side to connect
    protected AbstractConduitBlock(Properties properties, Capability<?> link) {
        super(properties);
        this.link = link;
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : SIDES)
            state = state.setValue(side, false);
        registerDefaultState(state);
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = Block.box(5, 5, 5, 11, 11, 11);
        VoxelShape[] arms = {
            Block.box(6, 0, 6, 10, 5, 10),    // down
            Block.box(6, 11, 6, 10, 16, 10),  // up
            Block.box(6, 6, 0, 10, 10, 5),    // north
            Block.box(6, 6, 11, 10, 10, 16),  // south
            Block.box(0, 6, 6, 5, 10, 10),    // west
            Block.box(11, 6, 6, 16, 10, 10)   // east
        };
        VoxelShape[] shapes = new VoxelShape[1 << arms.length];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = core;
            for (int side = 0; side < arms.length; side++)
                if ((mask & (1 << side)) != 0)
                    shape = Shapes.or(shape, arms[side]);
            shapes[mask] = shape;
        }
        return shapes;
    }

    public static boolean isConnected(BlockState state, Direction side) {
        return state.getValue(SIDES[side.ordinal()]);
    }

    // The conduit's own rule for a side, before any wrench override
    protected boolean autoConnects(BlockGetter level, BlockPos pos, Direction side) {
        return true;
    }

    private boolean canConnect(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity neighbour = level.getBlockEntity(pos.relative(side));
        if (neighbour == null || !neighbour.getCapability(link, side.getOpposite()).isPresent())
            return false;
        boolean inverted = level.getBlockEntity(pos) instanceof WrenchableConduit conduit
            && conduit.isSideInverted(side);
        return autoConnects(level, pos, side) ^ inverted;
    }

    // Recomputes one side, for the wrench
    public BlockState refreshSide(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.setValue(SIDES[side.ordinal()], canConnect(level, pos, side));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIDES);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : DIRECTIONS)
            state = refreshSide(state, context.getLevel(), context.getClickedPos(), side);
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return refreshSide(state, level, pos, direction);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (int side = 0; side < SIDES.length; side++)
            if (state.getValue(SIDES[side]))
                mask |= 1 << side;
        return SHAPES[mask];
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }
}
