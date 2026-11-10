package io.github.freshglitch.automati;

import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

// A buffer of Ergs (E), Automati's energy unit. It rides on Forge's energy
// capability, so machines trade energy through the standard interface.
public class ErgStorage extends EnergyStorage {
    // Ergs that crossed the port since the last poll: the raw data behind the
    // goggles' flow readout
    private int tickIn;
    private int tickOut;

    public ErgStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer, maxTransfer);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (!simulate)
            tickIn += received;
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (!simulate)
            tickOut += extracted;
        return extracted;
    }

    public int pollTickIn() {
        int polled = tickIn;
        tickIn = 0;
        return polled;
    }

    public int pollTickOut() {
        int polled = tickOut;
        tickOut = 0;
        return polled;
    }

    // Generation and consumption happen inside the machine: they skip the
    // transfer limit and do not count as port traffic
    public void generate(int amount) {
        energy = Math.min(capacity, energy + amount);
    }

    public void consume(int amount) {
        energy = Math.max(0, energy - amount);
    }

    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    // One-way views of this buffer. Consumers show their neighbours a
    // receive-only port and producers an extract-only one, which keeps energy
    // from flowing backwards through the grid.
    public IEnergyStorage receiveOnlyView() {
        return new View(true, false);
    }

    public IEnergyStorage extractOnlyView() {
        return new View(false, true);
    }

    private final class View implements IEnergyStorage {
        private final boolean receives;
        private final boolean extracts;

        private View(boolean receives, boolean extracts) {
            this.receives = receives;
            this.extracts = extracts;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receives ? ErgStorage.this.receiveEnergy(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return extracts ? ErgStorage.this.extractEnergy(maxExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            return ErgStorage.this.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return ErgStorage.this.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return extracts;
        }

        @Override
        public boolean canReceive() {
            return receives;
        }
    }
}
