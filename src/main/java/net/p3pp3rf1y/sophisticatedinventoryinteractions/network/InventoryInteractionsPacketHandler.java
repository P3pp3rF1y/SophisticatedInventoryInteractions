package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;

public class InventoryInteractionsPacketHandler extends PacketHandler {
	public static final InventoryInteractionsPacketHandler INSTANCE = new InventoryInteractionsPacketHandler();

	private InventoryInteractionsPacketHandler() {
		super(SophisticatedInventoryInteractions.MOD_ID);
	}

	@Override
	public void registerMessages() {
		registerMessage(ContainerInteractionPayload.class, ContainerInteractionPayload::encode, ContainerInteractionPayload::decode, ContainerInteractionPayload::onMessage);
	}
}
