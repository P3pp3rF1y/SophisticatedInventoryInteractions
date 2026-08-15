package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraftforge.network.NetworkDirection;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;

public class InventoryInteractionsPacketHandler extends PacketHandler {
	public static final InventoryInteractionsPacketHandler INSTANCE = new InventoryInteractionsPacketHandler();

	private InventoryInteractionsPacketHandler() {
		super(SophisticatedInventoryInteractions.MOD_ID, SophisticatedInventoryInteractions.getNetworkProtocolVersion());
	}

	@Override
	public void registerMessages() {
		registerMessage(ContainerInteractionPayload.class, ContainerInteractionPayload::encode, ContainerInteractionPayload::decode,
				ContainerInteractionPayload::onMessage, NetworkDirection.PLAY_TO_SERVER);
		registerMessage(RequestSortMemoryPayload.class, RequestSortMemoryPayload::encode, RequestSortMemoryPayload::decode, RequestSortMemoryPayload::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(SetSortMemoryPayload.class, SetSortMemoryPayload::encode, SetSortMemoryPayload::decode, SetSortMemoryPayload::onMessage,
				NetworkDirection.PLAY_TO_SERVER);
		registerMessage(SyncSortMemoryPayload.class, SyncSortMemoryPayload::encode, SyncSortMemoryPayload::decode, SyncSortMemoryPayload::onMessage,
				NetworkDirection.PLAY_TO_CLIENT);
	}
}
