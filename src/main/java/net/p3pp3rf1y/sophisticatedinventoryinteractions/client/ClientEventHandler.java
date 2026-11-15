package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;

public class ClientEventHandler {
	private static final ScreenInteractionInjector SCREEN_INTERACTION_INJECTOR = new ScreenInteractionInjector();

	private ClientEventHandler() {
	}

	public static void registerHandlers() {
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onScreenInit);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onScreenClosing);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientEventHandler::onKeyPressed);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onScreenRendered);
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientEventHandler::onMouseButtonPressed);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onRegisterClientCommands);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onMouseDragged);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onMouseReleased);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onMouseScrolled);
		NeoForge.EVENT_BUS.addListener(ClientEventHandler::onCharacterTyped);
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

	private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		new ClientScreenCommands(SCREEN_INTERACTION_INJECTOR.getScreenHistory(), SCREEN_INTERACTION_INJECTOR.getLayoutEditor()).register(event.getDispatcher());
	}

	private static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (SCREEN_INTERACTION_INJECTOR.getLayoutEditor().mouseDragged(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getMouseButton())) {
			event.setCanceled(true);
		}
	}

	private static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		if (SCREEN_INTERACTION_INJECTOR.getLayoutEditor().mouseReleased(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	private static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		if (SCREEN_INTERACTION_INJECTOR.getLayoutEditor().isActive(event.getScreen())) {
			event.setCanceled(true);
		}
	}

	private static void onCharacterTyped(ScreenEvent.CharacterTyped.Pre event) {
		if (SCREEN_INTERACTION_INJECTOR.getLayoutEditor().isActive(event.getScreen())) {
			event.setCanceled(true);
		}
	}
}
