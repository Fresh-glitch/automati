package io.github.freshglitch.automati;

import net.minecraftforge.energy.EnergyStorage;

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
}
