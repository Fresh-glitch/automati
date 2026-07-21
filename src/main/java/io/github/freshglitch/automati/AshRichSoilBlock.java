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

// Ash-Rich Soil: farmland conditioned with SCF (the silicon-calcium
// amendment cooked from ash — which, per Automati lore, is potash anyway).
// Real fly-ash soil science, gamified:
//   - never dries out (ash improves water retention) — moisture is pinned at 7
//   - crops above grow ~25% faster (the soil donates bonus growth ticks)
//   - nutrients leave with the harvest: when a player breaks the crop above,
//     there's a 10% chance the soil depletes back to dirt (see the
//     BlockEvent.BreakEvent hook in Automati). Automated harvesting doesn't
//     trigger it — machines are gentler than hoes.
public class AshRichSoilBlock extends FarmlandBlock {

    public AshRichSoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, 7));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // no moisture bookkeeping: this soil is permanently wet. Instead,
        // one soil tick in four hands the crop above a free growth tick —
        // in expectation, +25% growth speed.
        if (state.getValue(MOISTURE) < 7) {
            level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
        }
        if (random.nextInt(4) == 0) {
            BlockPos above = pos.above();
            BlockState crop = level.getBlockState(above);
            if (crop.getBlock() instanceof CropBlock && crop.isRandomlyTicking()) {
                crop.randomTick(level, above, random);
            }
        }
    }

    // Crops treat this like farmland (vanilla checks for the exact farmland
    // block, so the Forge hook has to say yes on its behalf)
    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
        return plantable.getPlantType(level, pos) == PlantType.CROP
            || super.canSustainPlant(state, level, pos, facing, plantable);
    }

    // Depletion entry point for the harvest hook: 10% chance to exhaust
    public static void maybeDeplete(ServerLevel level, BlockPos soilPos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.setBlockAndUpdate(soilPos, Blocks.DIRT.defaultBlockState());
        }
    }
}
