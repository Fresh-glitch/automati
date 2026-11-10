package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

// The item duct moves items in all six directions, which hoppers cannot. It
// connects to anything with an item inventory.
public class ItemDuctBlock extends AbstractConduitBlock {

    public ItemDuctBlock(Properties properties) {
        super(properties, ForgeCapabilities.ITEM_HANDLER);
    }

    // Every inventory qualifies except a hopper's non-nozzle faces: a hopper
    // only pushes out where it points, so that is the one face worth joining
    public static boolean policyAllows(@Nullable BlockGetter level, BlockPos ductPos, Direction side) {
        if (level == null)
            return true;
        BlockState neighbour = level.getBlockState(ductPos.relative(side));
        return !(neighbour.getBlock() instanceof HopperBlock)
            || neighbour.getValue(HopperBlock.FACING) == side.getOpposite();
    }

    @Override
    protected boolean autoConnects(BlockGetter level, BlockPos pos, Direction side) {
        return policyAllows(level, pos, side);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemDuctBlockEntity(pos, state);
    }
}
