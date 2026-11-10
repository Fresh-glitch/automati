package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// A block entity that works once per server tick. Machine and conduit blocks
// all hand out the same ticker, which dispatches here.
public interface ServerTicking {
    void serverTick(Level level, BlockPos pos, BlockState state);

    @Nullable
    static <T extends BlockEntity> BlockEntityTicker<T> ticker(Level level) {
        if (level.isClientSide())
            return null;
        return (tickLevel, pos, state, blockEntity) -> {
            if (blockEntity instanceof ServerTicking ticking)
                ticking.serverTick(tickLevel, pos, state);
        };
    }
}
