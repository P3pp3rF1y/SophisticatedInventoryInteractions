package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;

public class ClientEventHandler {
	private static final ScreenInteractionInjector SCREEN_INTERACTION_INJECTOR = new ScreenInteractionInjector();

	private ClientEventHandler() {
	}

	public static void registerHandlers() {
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onScreenInit);
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onScreenClosing);
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onKeyPressed);
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onScreenRendered);
		MinecraftForge.EVENT_BUS.addListener(ClientEventHandler::onMouseButtonPressed);
	}

	public static void applySortMemory(int containerId, SortBy sortBy) {
		SCREEN_INTERACTION_INJECTOR.applySortMemory(containerId, sortBy);
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
