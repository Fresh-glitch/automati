package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The coal generator burns coal or charcoal into Ergs and pushes them to its
// neighbours. Every burnt piece leaves one ash behind, and a full ash bin
// clogs the machine until it is emptied.
public class CoalGeneratorBlockEntity extends AbstractErgBlockEntity implements MenuProvider {
    public static final int CAPACITY = 100_000;
    public static final int GENERATION_PER_TICK = 40;
    public static final int MAX_PUSH_PER_TICK = 200;     // in total, shared among the neighbours
    public static final int BURN_TICKS_PER_FUEL = 1600;  // one piece, same as in a furnace
    public static final int MAX_ASH = 64;

    private int burnTime;   // ticks left on the burning piece

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(Automati.COAL_GENERATOR.blockEntity().get(), pos, state, CAPACITY, MAX_PUSH_PER_TICK, ErgPort.EXTRACT_ONLY);
        // fuel in, ash out
        inventory = new MachineInventory(this, "Fuel", "Ash",
            stack -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL));
        syncToMenu(() -> burnTime, () -> isClogged() ? 1 : 0);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        // burn; a piece that burns out leaves its ash behind
        if (burnTime > 0) {
            burnTime--;
            energy.generate(GENERATION_PER_TICK);
            changed = true;
            if (burnTime == 0)
                depositAsh(level, pos);
        }

        // restoke only while the buffer has room and the ash bin is not full
        if (burnTime == 0 && !isClogged() && energy.getEnergyStored() < energy.getMaxEnergyStored()
                && !inventory.extractItem(MachineInventory.INPUT, 1, false).isEmpty()) {
            burnTime = BURN_TICKS_PER_FUEL;
            changed = true;
        }

        boolean burning = burnTime > 0;
        if (state.getValue(AbstractMachineBlock.LIT) != burning)
            level.setBlock(pos, state.setValue(AbstractMachineBlock.LIT, burning), 3);

        distributeEnergy(level, pos, Math.min(energy.getEnergyStored(), MAX_PUSH_PER_TICK));
        maybeSyncEnergyToClients(level, pos);

        if (changed)
            setChanged();
    }

    public boolean isClogged() {
        return inventory.getStackInSlot(MachineInventory.OUTPUT).getCount() >= MAX_ASH;
    }

    private void depositAsh(Level level, BlockPos pos) {
        inventory.addOutput(new ItemStack(Automati.ASH.get()));
        // the deposit that fills the bin chokes the machine, audibly, once
        if (isClogged())
            level.playSound(null, pos, Automati.COAL_GENERATOR_CLOG.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", burnTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("BurnTime", 0);
    }
}
