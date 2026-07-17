package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

// The Erg Jar: Automati's battery. Ergs enter through the iron electrode and
// leave through the copper terminal (FACING) on the opposite face — the cell
// stack runs straight through the block. The Engineer's Wrench moves the
// terminal to whichever face it clicks (the electrode follows, staying
// opposite). CHARGE drives the visible pips on the sides — a glance reads the
// reserves without goggles or GUI.
public class ErgJarBlock extends AbstractMachineBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public ErgJarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(LIT, false)
            .setValue(FACING, Direction.UP)
            .setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, CHARGE);
    }

    // Terminal points at whatever the player is looking toward when placing:
    // "aim at the machine you want to feed"
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ErgJarBlockEntity(pos, state);
    }
}
