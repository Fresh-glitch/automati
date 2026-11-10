package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.event.level.BlockEvent;

// Ash-Rich Soil: farmland conditioned with SCF, the silicon-calcium amendment
// cooked from ash. It never dries out, crops on it grow about 25% faster, and
// a player harvesting the crop above may exhaust it back to dirt.
public class AshRichSoilBlock extends FarmlandBlock {
    private static final int BONUS_GROWTH_ODDS = 4;
    private static final int DEPLETION_ODDS = 10;

    public AshRichSoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, MAX_MOISTURE));
    }

    // No moisture bookkeeping: the soil stays wet, and one of its ticks in
    // four hands the crop above an extra growth tick
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MOISTURE) < MAX_MOISTURE)
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), 2);
        if (random.nextInt(BONUS_GROWTH_ODDS) != 0)
            return;
        BlockPos above = pos.above();
        BlockState crop = level.getBlockState(above);
        if (crop.getBlock() instanceof CropBlock && crop.isRandomlyTicking())
            crop.randomTick(level, above, random);
    }

    // Vanilla crops look for the exact farmland block, so the Forge hook has
    // to vouch for this one
    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
        return plantable.getPlantType(level, pos) == PlantType.CROP
            || super.canSustainPlant(state, level, pos, facing, plantable);
    }

    // Nutrients leave with the harvest: a player breaking the crop above
    // exhausts the soil one time in ten. Only players fire this event, so
    // automated harvesting spares the soil.
    public static void onCropHarvest(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof CropBlock))
            return;
        BlockPos soil = event.getPos().below();
        if (level.getBlockState(soil).is(Automati.ASH_RICH_SOIL.get()) && level.getRandom().nextInt(DEPLETION_ODDS) == 0)
            level.setBlockAndUpdate(soil, Blocks.DIRT.defaultBlockState());
    }
}
