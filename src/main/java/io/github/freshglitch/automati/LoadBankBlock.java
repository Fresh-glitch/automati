package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// The load bank block: no facing (its coils wrap all four sides), just a LIT
// state that glows while energy is being dissipated as heat.
public class LoadBankBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LoadBankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LoadBankBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof LoadBankBlockEntity loadBank)
                loadBank.serverTick(lvl, pos, st);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof LoadBankBlockEntity loadBank) {
            serverPlayer.openMenu(loadBank, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // Heat shimmer rising off the block while it burns energy away
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.4) {
            level.addParticle(ParticleTypes.WHITE_SMOKE,
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 1.02,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0.0, 0.05, 0.0);
        }
    }
}
