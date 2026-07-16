package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// The Engineer's Wrench: click a conduit to toggle one of its connections.
// It severs links the conduit made automatically, and splices links its
// policy declined. Clicking an arm targets that arm; clicking the core
// targets the face you clicked; clicking the face of a MACHINE where a
// conduit meets it toggles that joint from the other side.
public class WrenchItem extends Item {

    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockEntity(pos) instanceof WrenchableConduit conduit) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            toggle(level, pos, conduit, pickSide(context, pos));
            return InteractionResult.SUCCESS;
        }

        // grazing-angle grace: clicking the face of a non-conduit block where
        // a conduit joins it toggles that conduit's side toward this block
        BlockPos neighborPos = pos.relative(context.getClickedFace());
        if (level.getBlockEntity(neighborPos) instanceof WrenchableConduit conduit) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            toggle(level, neighborPos, conduit, context.getClickedFace().getOpposite());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void toggle(Level level, BlockPos pos, WrenchableConduit conduit, Direction side) {
        BlockPos otherPos = pos.relative(side);
        BlockEntity other = level.getBlockEntity(otherPos);

        if (other instanceof WrenchableConduit otherConduit && sameFamily(conduit, otherConduit)) {
            // a joint between two conduits of the same network is one LINK:
            // both ends move in lockstep — severed together, restored together
            // — so a half-severed limbo state can never exist
            boolean severed = conduit.isSideInverted(side)
                || otherConduit.isSideInverted(side.getOpposite());
            conduit.setSideInverted(side, !severed);
            otherConduit.setSideInverted(side.getOpposite(), !severed);
            refreshConnection(level, otherPos, side.getOpposite());
        } else {
            conduit.toggleSide(side);
        }
        refreshConnection(level, pos, side);

        level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
    }

    private static boolean sameFamily(WrenchableConduit a, WrenchableConduit b) {
        return (a instanceof ErgCableBlockEntity && b instanceof ErgCableBlockEntity)
            || (a instanceof ItemDuctBlockEntity && b instanceof ItemDuctBlockEntity);
    }

    private static void refreshConnection(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ErgCableBlock cable)
            state = cable.refreshSide(state, level, pos, side);
        else if (state.getBlock() instanceof ItemDuctBlock duct)
            state = duct.refreshSide(state, level, pos, side);
        level.setBlock(pos, state, 3);
    }

    // Which connection did the player mean? Test the hit point against each
    // arm's actual box (with a little tolerance, so grazing hits still count);
    // a hit on the central core means the face clicked.
    private static Direction pickSide(UseOnContext context, BlockPos pos) {
        Vec3 local = context.getClickLocation()
            .subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        Direction best = armAt(local, 0.03);
        if (best == null)
            best = armAt(local, 0.08); // second pass, extra forgiving
        return best != null ? best : context.getClickedFace();
    }

    // Inside an arm: > coreHalf along exactly that arm's axis, within the
    // arm's cross-section (± 2px) on the other two
    @Nullable
    private static Direction armAt(Vec3 local, double eps) {
        double coreHalf = 0.1875;   // core is 6px wide
        double armHalf = 0.125;     // arms are 4px wide
        for (Direction direction : Direction.values()) {
            double along = local.x * direction.getStepX()
                + local.y * direction.getStepY()
                + local.z * direction.getStepZ();
            if (along < coreHalf - eps)
                continue;
            boolean fits = true;
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (axis == direction.getAxis())
                    continue;
                double cross = switch (axis) {
                    case X -> local.x;
                    case Y -> local.y;
                    case Z -> local.z;
                };
                if (Math.abs(cross) > armHalf + eps) {
                    fits = false;
                    break;
                }
            }
            if (fits)
                return direction;
        }
        return null;
    }
}
