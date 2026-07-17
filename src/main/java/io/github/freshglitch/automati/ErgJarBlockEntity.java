package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

// The Erg Jar's brain. Storage-only: no fuel, no recipes — Ergs enter through
// the iron electrode face, and discharge through the copper terminal on the
// opposite face, up to 80 E/t each way. The electrode stack runs straight
// through the jar (iron base -> ash core -> copper cap, like the recipe
// column); the four shell faces carry no port at all. Direction is enforced
// by per-side capability views, the same port discipline that keeps the rest
// of the grid from flowing backwards.
public class ErgJarBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    public static final int CAPACITY = 320_000;  // exactly five lumps of coal (5 x 64,000 E)
    public static final int MAX_TRANSFER = 80;   // charge/discharge ceiling, Ergs per tick

    private final LazyOptional<IEnergyStorage> receiveOptional;
    private final LazyOptional<IEnergyStorage> extractOptional;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored() & 0xFFFF;
                case 1 -> (energy.getEnergyStored() >> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ErgJarBlockEntity(BlockPos pos, BlockState state) {
        // OPEN backs the side-less query (goggles); sided queries below never see it
        super(Automati.ERG_JAR_BLOCK_ENTITY.get(), pos, state, CAPACITY, MAX_TRANSFER, ErgPort.OPEN);
        this.receiveOptional = LazyOptional.of(energy::receiveOnlyView);
        this.extractOptional = LazyOptional.of(energy::extractOnlyView);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        // Discharge through the terminal only
        Direction terminal = state.getValue(ErgJarBlock.FACING);
        distributeEnergy(level, pos, MAX_TRANSFER, null, side -> side == terminal);

        // Keep the visible charge pips in step with the buffer
        int charge = computeCharge();
        if (state.getValue(ErgJarBlock.CHARGE) != charge)
            level.setBlock(pos, state.setValue(ErgJarBlock.CHARGE, charge), 3);

        maybeSyncEnergyToClients(level, pos);
    }

    // 0 pips = empty; any charge lights at least one; all four means full-ish
    private int computeCharge() {
        int stored = energy.getEnergyStored();
        if (stored == 0)
            return 0;
        return Math.max(1, (int) Math.round(stored * 4.0 / CAPACITY));
    }

    // Copper terminal talks extract-only, the iron electrode opposite it
    // receive-only, and the four shell faces expose no energy port at all.
    // Side-less queries (the goggles HUD) fall through to the base's full view.
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
    public Component getDisplayName() {
        return Component.translatable("block.automati.erg_jar");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ErgJarMenu(containerId, playerInventory, this, containerData);
    }
}
