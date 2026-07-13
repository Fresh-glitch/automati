package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// Tier-1 generator: burns coal/charcoal into Ergs. The block itself is thin —
// facing/lit state and menu opening — all the machine logic lives in the block entity.
public class CoalGeneratorBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CoalGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Face the player when placed, like a furnace
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoalGeneratorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof CoalGeneratorBlockEntity generator)
                generator.serverTick(lvl, pos, st);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CoalGeneratorBlockEntity generator) {
            serverPlayer.openMenu(generator, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // Client-side ambience while the generator is running: a mechanical hum,
    // smoke drifting from the top, and electric sparks jumping out of the vent —
    // sparks instead of flames, so it reads as machinery rather than a furnace.
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.15) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Automati.COAL_GENERATOR_LOOP.get(), SoundSource.BLOCKS,
                0.6F, 0.9F + random.nextFloat() * 0.2F, false);
        }

        // smoke rises out of the exhaust stack in the middle of the top face
        if (random.nextDouble() < 0.25) {
            level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.4 + random.nextDouble() * 0.2,
                pos.getY() + 1.02,
                pos.getZ() + 0.4 + random.nextDouble() * 0.2,
                0.0, 0.03, 0.0);
        }

        Direction facing = state.getValue(FACING);
        if (random.nextDouble() < 0.3) {
            double jitter = (random.nextDouble() - 0.5) * 0.5;
            double px = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (facing.getStepX() == 0 ? jitter : 0.0);
            double pz = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (facing.getStepZ() == 0 ? jitter : 0.0);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                px, pos.getY() + 0.3 + random.nextDouble() * 0.35, pz,
                0.0, 0.0, 0.0);
        }
    }
}
