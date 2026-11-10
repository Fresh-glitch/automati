package io.github.freshglitch.automati;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

// The Engineer's Wrench toggles one connection of a conduit: it severs links
// the conduit made by itself and splices links its policy declined. Clicking
// an arm targets that arm, clicking the core targets the clicked face, and
// clicking a machine face where a conduit meets it toggles that joint. On an
// Erg Jar it moves the copper terminal to the clicked face.
public class WrenchItem extends Item {
    private static final List<Component> TOOLTIP = List.of(
        Automati.tooltip("engineers_wrench.conduits", ChatFormatting.GRAY),
        Automati.tooltip("engineers_wrench.jar", ChatFormatting.GRAY));

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        TOOLTIP.forEach(tooltip);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(pos);

        // Jar first, so a jar face never falls through to the joint case below
        if (state.getBlock() instanceof ErgJarBlock) {
            if (!level.isClientSide() && state.getValue(ErgJarBlock.FACING) != face) {
                level.setBlock(pos, state.setValue(ErgJarBlock.FACING, face), 3);
                level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof WrenchableConduit conduit) {
            if (!level.isClientSide())
                toggle(level, pos, conduit, pickSide(context, pos));
            return InteractionResult.SUCCESS;
        }

        // A machine face with a conduit against it: toggle the conduit's side
        // toward the machine
        BlockPos conduitPos = pos.relative(face);
        if (level.getBlockEntity(conduitPos) instanceof WrenchableConduit conduit) {
            if (!level.isClientSide())
                toggle(level, conduitPos, conduit, face.getOpposite());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void toggle(Level level, BlockPos pos, WrenchableConduit conduit, Direction side) {
        BlockPos otherPos = pos.relative(side);
        if (level.getBlockEntity(otherPos) instanceof WrenchableConduit other && other.getClass() == conduit.getClass()) {
            // A joint between two conduits of one kind is a single link: both
            // ends are severed or restored together, never just one of them
            boolean severed = conduit.isSideInverted(side) || other.isSideInverted(side.getOpposite());
            conduit.setSideInverted(side, !severed);
            other.setSideInverted(side.getOpposite(), !severed);
            refreshConnection(level, otherPos, side.getOpposite());
        } else {
            conduit.toggleSide(side);
        }
        refreshConnection(level, pos, side);
        level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
    }

    private static void refreshConnection(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractConduitBlock conduit)
            level.setBlock(pos, conduit.refreshSide(state, level, pos, side), 3);
    }

    // The arm under the hit point, tested with a little tolerance and then a
    // more forgiving second pass; a hit on the core means the clicked face
    private static Direction pickSide(UseOnContext context, BlockPos pos) {
        Vec3 local = context.getClickLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction arm = armAt(local, 0.03);
        if (arm == null)
            arm = armAt(local, 0.08);
        return arm != null ? arm : context.getClickedFace();
    }

    // A point is in an arm when it lies beyond the core along the arm's axis
    // and within the arm's cross-section on the other two
    @Nullable
    private static Direction armAt(Vec3 local, double tolerance) {
        for (Direction side : Direction.values()) {
            double along = local.x * side.getStepX() + local.y * side.getStepY() + local.z * side.getStepZ();
            if (along < AbstractConduitBlock.CORE_HALF - tolerance)
                continue;
            Vec3 across = local.subtract(along * side.getStepX(), along * side.getStepY(), along * side.getStepZ());
            double offAxis = Math.max(Math.abs(across.x), Math.max(Math.abs(across.y), Math.abs(across.z)));
            if (offAxis <= AbstractConduitBlock.ARM_HALF + tolerance)
                return side;
        }
        return null;
    }
}
