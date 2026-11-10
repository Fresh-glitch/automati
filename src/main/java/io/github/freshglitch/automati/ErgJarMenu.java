package io.github.freshglitch.automati;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

// Erg Jar container: no slots and no buttons, just the charge and flow readout
public class ErgJarMenu extends AbstractErgMenu<ErgJarBlockEntity> {

    // Client side
    public ErgJarMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, machineAt(playerInventory, buf));
    }

    public ErgJarMenu(int containerId, Inventory playerInventory, ErgJarBlockEntity jar) {
        super(Automati.ERG_JAR.menu().get(), containerId, jar, 0);
        addPlayerInventory(playerInventory);
    }

    // Flow rates come off the client block entity: they ride the same update
    // tags that feed the goggles HUD
    public int getFlowIn() {
        return machine.getFlowInRate();
    }

    public int getFlowOut() {
        return machine.getFlowOutRate();
    }
}
