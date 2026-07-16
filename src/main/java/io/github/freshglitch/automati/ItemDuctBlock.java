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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// The item duct: moves items in all six directions — the thing vanilla
// hoppers never learned. Connects to anything with an item inventory,
// fence-style, exactly like the Erg cable does for energy.
public class ItemDuctBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
        Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
        Direction.EAST, EAST, Direction.WEST, WEST,
        Direction.UP, UP, Direction.DOWN, DOWN);

    private final VoxelShape[] shapes = buildShapes();

    public ItemDuctBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = Block.box(5, 5, 5, 11, 11, 11);
        VoxelShape[] arms = {
            Block.box(6, 6, 0, 10, 10, 5),    // north
            Block.box(6, 6, 11, 10, 10, 16),  // south
            Block.box(11, 6, 6, 16, 10, 10),  // east
            Block.box(0, 6, 6, 5, 10, 10),    // west
            Block.box(6, 11, 6, 10, 16, 10),  // up
            Block.box(6, 0, 6, 10, 5, 10)     // down
        };
        VoxelShape[] result = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int bit = 0; bit < 6; bit++)
                if ((mask & (1 << bit)) != 0)
                    shape = Shapes.or(shape, arms[bit]);
            result[mask] = shape;
        }
        return result;
    }

    private static int shapeIndex(BlockState state) {
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(SOUTH)) mask |= 2;
        if (state.getValue(EAST)) mask |= 4;
        if (state.getValue(WEST)) mask |= 8;
        if (state.getValue(UP)) mask |= 16;
        if (state.getValue(DOWN)) mask |= 32;
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[shapeIndex(state)];
    }

    // Connection policy: everything with an inventory qualifies, EXCEPT a
    // hopper's non-nozzle faces — a hopper only pushes out where it points,
    // so that's the only face worth auto-connecting to. (The wrench can
    // override this per side.)
    public static boolean policyAllows(@Nullable BlockGetter level, BlockPos ductPos, Direction side) {
        if (level == null)
            return true;
        BlockState neighbor = level.getBlockState(ductPos.relative(side));
        if (neighbor.getBlock() instanceof net.minecraft.world.level.block.HopperBlock)
            return neighbor.getValue(net.minecraft.world.level.block.HopperBlock.FACING) == side.getOpposite();
        return true;
    }

    // A side connects when the neighbour offers an item inventory on the face
    // pointing back at us AND our own policy (possibly wrench-inverted) agrees
    private boolean canConnect(BlockGetter level, BlockPos ductPos, Direction direction) {
        BlockEntity neighbour = level.getBlockEntity(ductPos.relative(direction));
        if (neighbour == null || !neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).isPresent())
            return false;
        boolean inverted = level.getBlockEntity(ductPos) instanceof ItemDuctBlockEntity duct
            && duct.isSideInverted(direction);
        return policyAllows(level, ductPos, direction) ^ inverted;
    }

    // Recompute one side's connection after a wrench toggle
    public BlockState refreshSide(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), canConnect(level, pos, side));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                canConnect(context.getLevel(), context.getClickedPos(), direction));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction),
            canConnect(level, pos, direction));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemDuctBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof ItemDuctBlockEntity duct)
                duct.serverTick(lvl, pos, st);
        };
    }
}
