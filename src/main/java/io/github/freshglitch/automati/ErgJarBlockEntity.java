package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

// The Erg Jar is storage and nothing else. Ergs enter through the iron
// electrode and leave through the copper terminal on the opposite face, up to
// 80 E/t each way; the four shell faces have no port at all. The cell is the
// recipe column: an iron and a copper electrode (a real galvanic pair) with
// ash as the electrolyte between them.
public class ErgJarBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    public static final int CAPACITY = 320_000;   // five pieces of coal, at 64,000 E each
    public static final int MAX_TRANSFER = 80;

    private final LazyOptional<IEnergyStorage> receiveOptional;
    private final LazyOptional<IEnergyStorage> extractOptional;

    public ErgJarBlockEntity(BlockPos pos, BlockState state) {
        // OPEN only backs the side-less query the goggles make; sided queries
        // get the one-way views below
        super(Automati.ERG_JAR.blockEntity().get(), pos, state, CAPACITY, MAX_TRANSFER, ErgPort.OPEN);
        this.receiveOptional = LazyOptional.of(energy::receiveOnlyView);
        this.extractOptional = LazyOptional.of(energy::extractOnlyView);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        distributeEnergy(level, pos, MAX_TRANSFER);

        int charge = chargeLevel();
        if (state.getValue(ErgJarBlock.CHARGE) != charge)
            level.setBlock(pos, state.setValue(ErgJarBlock.CHARGE, charge), 3);

        maybeSyncEnergyToClients(level, pos);
    }

    // The jar discharges through its terminal only
    @Override
    protected boolean feedsSide(Direction side) {
        return side == getBlockState().getValue(ErgJarBlock.FACING);
    }

    // Pips shown on the block: 0 when empty, at least 1 for any charge, 4 near full
    private int chargeLevel() {
        int stored = energy.getEnergyStored();
        return stored == 0 ? 0 : Math.max(1, (int) Math.round(stored * 4.0 / CAPACITY));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null) {
            Direction terminal = getBlockState().getValue(ErgJarBlock.FACING);
            if (side == terminal)
                return extractOptional.cast();
            if (side == terminal.getOpposite())
                return receiveOptional.cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        receiveOptional.invalidate();
        extractOptional.invalidate();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ErgJarMenu(containerId, playerInventory, this);
    }
}
