package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientEventHandler {
	private static final ScreenInteractionInjector SCREEN_INTERACTION_INJECTOR = new ScreenInteractionInjector();

	private ClientEventHandler() {
	}

	public static void registerHandlers() {
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onScreenInit);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onScreenClosing);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onKeyPressed);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onScreenRendered);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onMouseButtonPressed);
	}

	private static void onScreenInit(ScreenEvent.Init.Post event) {
		SCREEN_INTERACTION_INJECTOR.onScreenInit(event);
	}

	private static void onScreenClosing(ScreenEvent.Closing event) {
		SCREEN_INTERACTION_INJECTOR.onScreenClosing(event);
	}

	private static void onScreenRendered(ScreenEvent.Render.Post event) {
		SCREEN_INTERACTION_INJECTOR.onScreenRendered(event);
	}

	private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
		SCREEN_INTERACTION_INJECTOR.onKeyPressed(event);
	}

	private static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
		SCREEN_INTERACTION_INJECTOR.onMouseButtonPressed(event);
	}
}
