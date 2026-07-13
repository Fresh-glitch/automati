package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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

// The crusher block: counter-rotating blade rollers on the top face, visible
// from every side, so no facing — just LIT while the rollers are spinning.
public class CrusherBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CrusherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof CrusherBlockEntity crusher)
                crusher.serverTick(lvl, pos, st);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher) {
            serverPlayer.openMenu(crusher, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // Grinding ambience and stone dust kicked up by the rollers
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.15) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Automati.CRUSHER_LOOP.get(), SoundSource.BLOCKS,
                0.7F, 0.95F + random.nextFloat() * 0.1F, false);
        }

        if (random.nextDouble() < 0.3) {
            level.addParticle(ParticleTypes.POOF,
                pos.getX() + 0.3 + random.nextDouble() * 0.4,
                pos.getY() + 1.05,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                0.0, 0.02, 0.0);
        }
    }
}
