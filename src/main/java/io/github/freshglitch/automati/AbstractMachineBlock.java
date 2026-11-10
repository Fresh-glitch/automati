package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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

// Base block for Erg machines: the LIT state, the server tick handed to the
// block entity, and right-click opening its menu. A subclass only adds the
// machine's personality: particles, sounds, hazards.
public abstract class AbstractMachineBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected AbstractMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            serverPlayer.openMenu(provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // Ambience helpers for animateTick(), which runs on the client only

    protected static void playAmbience(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    // A particle rising off the top face. inset keeps it away from the edges;
    // height is measured from the bottom of the block.
    protected static void riseFromTop(Level level, BlockPos pos, RandomSource random, ParticleOptions particle,
                                      double inset, double height, double speed) {
        double spread = 1.0 - 2.0 * inset;
        level.addParticle(particle,
            pos.getX() + inset + random.nextDouble() * spread,
            pos.getY() + height,
            pos.getZ() + inset + random.nextDouble() * spread,
            0.0, speed, 0.0);
    }
}
