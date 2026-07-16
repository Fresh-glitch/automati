package io.github.freshglitch.automati;

import net.minecraft.core.Direction;

// A conduit whose per-side connections the Engineer's Wrench can adjust.
// The wrench INVERTS the automatic connection decision for a side: it severs
// links the conduit made on its own, and splices links the conduit's policy
// declined (like a hopper's non-nozzle faces).
public interface WrenchableConduit {
    boolean isSideInverted(Direction side);

    void setSideInverted(Direction side, boolean inverted);

    default void toggleSide(Direction side) {
        setSideInverted(side, !isSideInverted(side));
    }
}
