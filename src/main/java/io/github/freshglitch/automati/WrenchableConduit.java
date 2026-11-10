package io.github.freshglitch.automati;

import net.minecraft.core.Direction;

// A conduit whose sides the Engineer's Wrench can invert: an inverted side
// does the opposite of what the conduit would decide on its own. That severs
// a link it made by itself, or splices one its policy declined (such as a
// hopper's non-nozzle faces). Implementers store the six flags as one bit
// mask, bit n being the side with ordinal n.
public interface WrenchableConduit {
    int invertedSides();

    void setInvertedSides(int mask);

    default boolean isSideInverted(Direction side) {
        return (invertedSides() & (1 << side.ordinal())) != 0;
    }

    default void setSideInverted(Direction side, boolean inverted) {
        int bit = 1 << side.ordinal();
        setInvertedSides(inverted ? invertedSides() | bit : invertedSides() & ~bit);
    }

    default void toggleSide(Direction side) {
        setSideInverted(side, !isSideInverted(side));
    }
}
