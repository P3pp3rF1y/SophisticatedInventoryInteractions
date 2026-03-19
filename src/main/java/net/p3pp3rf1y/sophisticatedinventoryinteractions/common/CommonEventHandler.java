package net.p3pp3rf1y.sophisticatedinventoryinteractions.common;

import net.neoforged.bus.api.IEventBus;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.init.ModPayloads;

public class CommonEventHandler {
	private CommonEventHandler() {
	}

	public static void registerHandlers(IEventBus modBus) {
		modBus.addListener(ModPayloads::registerPayloads);
	}
}
