package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// A load bank, the rig engineers test generators with: it takes Ergs and
// burns them off as waste heat, at a rate the player dials in from the GUI.
public class LoadBankBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    public static final int CAPACITY = 10_000;
    public static final int MAX_RECEIVE = 400;
    public static final int MAX_RATE = 400;     // highest dump rate, Ergs per tick
    public static final int RATE_STEP = 20;     // one button press

    private int dumpRate = 40;   // Ergs per tick; starts matched to one generator

    public LoadBankBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.LOAD_BANK.blockEntity().get(), pos, state, CAPACITY, MAX_RECEIVE, ErgPort.RECEIVE_ONLY);
        syncToMenu(() -> dumpRate);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        int dumped = Math.min(dumpRate, energy.getEnergyStored());
        if (dumped > 0) {
            energy.consume(dumped);
            setChanged();
        }

        // the coils glow only while energy is actually being burned off
        boolean glowing = dumped > 0;
        if (state.getValue(AbstractMachineBlock.LIT) != glowing)
            level.setBlock(pos, state.setValue(AbstractMachineBlock.LIT, glowing), 3);

        maybeSyncEnergyToClients(level, pos);
    }

    // The menu's - and + buttons
    public void adjustDumpRate(int delta) {
        dumpRate = Math.clamp(dumpRate + delta, 0, MAX_RATE);
        setChanged();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LoadBankMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("DumpRate", dumpRate);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        dumpRate = Math.clamp(input.getIntOr("DumpRate", 40), 0, MAX_RATE);
    }
}
