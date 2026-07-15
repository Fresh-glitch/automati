package io.github.freshglitch.automati;

import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

// Automati's energy is measured in Ergs (E). Internally this rides on the Forge
// energy capability so our own machines (and, if we ever want, other mods') can
// exchange energy through the standard interface.
public class ErgStorage extends EnergyStorage {
    public ErgStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer, maxTransfer);
    }

    // Used by generators: adds energy directly, ignoring the receive-rate limit,
    // clamped to capacity.
    public void generate(int amount) {
        energy = Math.min(capacity, energy + amount);
    }

    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    // Port views: what a machine shows its neighbours. Consumers expose a
    // receive-only view, producers an extract-only one — the direction rules
    // that keep energy from flowing backwards through the grid.
    public IEnergyStorage receiveOnlyView() {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return ErgStorage.this.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
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
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    public IEnergyStorage extractOnlyView() {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return ErgStorage.this.extractEnergy(maxExtract, simulate);
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
                return true;
            }

            @Override
            public boolean canReceive() {
                return false;
            }
        };
    }
}
