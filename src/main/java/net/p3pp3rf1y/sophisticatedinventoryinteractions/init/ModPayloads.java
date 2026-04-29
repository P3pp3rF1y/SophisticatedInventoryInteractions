package net.p3pp3rf1y.sophisticatedinventoryinteractions.init;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.network.ContainerInteractionPayload;

public class ModPayloads {
	private ModPayloads() {
	}

	public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(SophisticatedInventoryInteractions.MOD_ID).versioned(SophisticatedInventoryInteractions.getNetworkProtocolVersion());
		registrar.playToServer(ContainerInteractionPayload.TYPE, ContainerInteractionPayload.STREAM_CODEC, ContainerInteractionPayload::handlePayload);
	}
}
