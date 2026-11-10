package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

// The Erg cable: connects to anything with an energy port, machines and
// other cables alike
public class ErgCableBlock extends AbstractConduitBlock {

    public ErgCableBlock(Properties properties) {
        super(properties, ForgeCapabilities.ENERGY);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ErgCableBlockEntity(pos, state);
    }
}
