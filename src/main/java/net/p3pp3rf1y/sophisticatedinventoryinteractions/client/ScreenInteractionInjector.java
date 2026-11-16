package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.AnchorLayoutService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.LayoutAdjustmentEditor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets.Anchor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets.Offset;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.network.ContainerInteractionPayload;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.network.RequestSortMemoryPayload;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.network.SetSortMemoryPayload;
import org.lwjgl.sdl.SDLKeycode;

import javax.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.SORT_KEYBIND;
import static net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND;
import static net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND;
import static net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.isActiveAndMatchesIgnoringShift;

public class ScreenInteractionInjector {
	private static final int DISABLED_SLOT_X_POS = -2000;
	private static final int BUTTON_GAP = 0;
	private static final int BUTTON_SIZE = 12;
	private static final int SLOT_SIZE = 18;
	private static final int DEFAULT_NO_RESULTS_BG_COLOR = 0xFF777777;
	private static final int SOPHISTICATED_TRANSFER_SHIFT = BUTTON_SIZE + BUTTON_GAP;

	private final ScreenContextResolver contextResolver = new ScreenContextResolver();
	private final ScreenEligibilityService screenEligibilityService = new ScreenEligibilityService();
	private final ClientScreenHistory screenHistory = new ClientScreenHistory();
	private final AnchorLayoutService anchorLayoutService = new AnchorLayoutService();
	private final LayoutAdjustmentEditor layoutEditor = new LayoutAdjustmentEditor();
	private final Map<AbstractContainerScreen<?>, InjectedScreenState> states = new WeakHashMap<>();
	private final Map<StorageScreenBase<?>, SophisticatedScreenState> sophisticatedStates = new WeakHashMap<>();
	private final Map<AbstractContainerScreen<?>, PlayerOnlyScreenState> playerOnlyStates = new WeakHashMap<>();
	private final Map<AbstractContainerScreen<?>, CachedScreenContext> screenContexts = new WeakHashMap<>();

	public void onScreenInit(ScreenEvent.Init.Post event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		if (screen instanceof CreativeModeInventoryScreen) {
			return;
		}
		String previousSearch = clearInjectedScreenState(screen);
		Optional<CachedScreenContext> resolvedContext = getOrResolveScreenContext(screen);
		if (resolvedContext.isEmpty()) {
			return;
		}

		CachedScreenContext cachedContext = resolvedContext.get();
		ScreenContextResolver.ResolvedScreenContext context = cachedContext.context();
		screenHistory.record(context, screen.getTitle().getString(), cachedContext.eligibility(), cachedContext.transferEligibility(),
				cachedContext.playerSortEligibility());
		if (context.eligibilityDescriptor().sophisticatedNativeScreen() && screen instanceof StorageScreenBase<?> storageScreen) {
			initSophisticatedScreen(event, storageScreen, context.slotRegions(), cachedContext.playerSortEligibility().eligible());
			syncSharedSearchToStorageScreen(storageScreen);
			return;
		}

		initPlayerOnlyScreen(event, cachedContext);
		if (!isPlayerOnlyMenu(screen)) {
			initContainerAndTransferControls(event, screen, cachedContext, previousSearch);
		}
	}

	private String clearInjectedScreenState(AbstractContainerScreen<?> screen) {
		sophisticatedStates.remove(screen);
		playerOnlyStates.remove(screen);
		InjectedScreenState previousState = states.remove(screen);
		if (previousState == null) {
			return "";
		}
		restoreSlotPositions(previousState);
		return previousState.getSearchPhrase();
	}

	private Optional<CachedScreenContext> getOrResolveScreenContext(AbstractContainerScreen<?> screen) {
		CachedScreenContext cachedContext = screenContexts.get(screen);
		if (cachedContext != null) {
			return Optional.of(cachedContext);
		}
		return contextResolver.resolve(screen).map(context -> {
			CachedScreenContext resolvedContext = new CachedScreenContext(context, screenEligibilityService.evaluate(context.eligibilityDescriptor()),
					screenEligibilityService.evaluateTransfers(context.eligibilityDescriptor()),
					screenEligibilityService.evaluatePlayerSort(context.eligibilityDescriptor(), context.slotRegions()));
			screenContexts.put(screen, resolvedContext);
			return resolvedContext;
		});
	}

	private void initContainerAndTransferControls(ScreenEvent.Init.Post event, AbstractContainerScreen<?> screen, CachedScreenContext cachedContext,
			String previousSearch) {
		if (!cachedContext.eligibility().eligible() && !cachedContext.transferEligibility().eligible()) {
			return;
		}

		SlotRegions regions = cachedContext.context().slotRegions();
		Optional<AnchorLayoutService.AnchorLayout> layout = anchorLayoutService.getLayout(screen, regions, getContainerControls(regions));
		if (layout.isEmpty()) {
			return;
		}

		InjectedScreenState state = createState(screen, regions, layout.get(), cachedContext.eligibility().eligible(),
				cachedContext.transferEligibility().eligible());
		if (state.hasSearchBox() && isRememberSearchPhraseEnabled() && previousSearch.isEmpty()) {
			previousSearch = SharedSearchPhraseMemory.getSearchPhrase();
		}
		if (state.hasSearchBox() && !previousSearch.isEmpty()) {
			state.searchBox.setValue(previousSearch);
		}
		addInjectedWidgets(event, state);
		states.put(screen, state);
		ClientPacketDistributor.sendToServer(new RequestSortMemoryPayload());
		applySearchFilter(state);
	}

	private void addInjectedWidgets(ScreenEvent.Init.Post event, InjectedScreenState state) {
		if (state.searchBox != null) {
			event.addListener(state.searchBox);
		}
		if (state.sortContainerButton != null) {
			event.addListener(state.sortContainerButton);
		}
		if (state.sortByButton != null) {
			event.addListener(state.sortByButton);
		}
		if (state.transferToContainerButton != null && state.transferToPlayerButton != null) {
			event.addListener(state.transferToContainerButton);
			event.addListener(state.transferToPlayerButton);
		}
		if (state.noResultsLabel != null) {
			event.addListener(state.noResultsLabel);
		}
	}

	public void applySortMemory(int containerId, SortBy sortBy) {
		Screen currentScreen = Minecraft.getInstance().gui.screen();
		if (!(currentScreen instanceof AbstractContainerScreen<?> containerScreen) || containerScreen.getMenu().containerId != containerId) {
			return;
		}
		InjectedScreenState state = states.get(containerScreen);
		if (state != null) {
			state.sortByState.setSortBy(sortBy);
		}
	}

	public void onScreenClosing(ScreenEvent.Closing event) {
		layoutEditor.onClosing(event.getScreen());
		if (event.getScreen() instanceof StorageScreenBase<?> storageScreen) {
			syncSharedSearchFromStorageScreen(storageScreen);
			sophisticatedStates.remove(storageScreen);
		}
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}

		InjectedScreenState state = states.remove(screen);
		playerOnlyStates.remove(screen);
		screenContexts.remove(screen);
		if (state != null) {
			restoreSlotPositions(state);
		}
	}

	public void onScreenRendered(ScreenEvent.Render.Post event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		if (screen instanceof StorageScreenBase<?> storageScreen) {
			SophisticatedScreenState sophisticatedState = sophisticatedStates.get(storageScreen);
			if (sophisticatedState != null) {
				updateSophisticatedLayout(storageScreen, sophisticatedState);
				if (sophisticatedState.sortPlayerButton != null && !layoutEditor.isActive(screen)) {
					sophisticatedState.sortPlayerButton.extractTooltip(storageScreen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
				}
			}
			renderLayoutEditor(event, screen);
			syncSharedSearchFromStorageScreen(storageScreen);
			return;
		}

		InjectedScreenState state = states.get(screen);
		PlayerOnlyScreenState playerOnlyState = playerOnlyStates.get(screen);
		if (playerOnlyState != null) {
			updateDynamicLayout(screen, playerOnlyState);
			if (!layoutEditor.isActive(screen)) {
				playerOnlyState.sortPlayerButton.extractTooltip(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
			}
		}
		if (state == null) {
			renderLayoutEditor(event, screen);
			return;
		}

		updateDynamicLayout(screen, state);
		applySearchFilter(state);
		updateNoResultsBackgroundColor(state);
		if (state.searchBox != null) {
			state.searchBox.extractRenderStateInLatePass(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0);
		}
		if (state.noResultsLabel != null) {
			state.noResultsLabel.extractRenderStateInLatePass(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0);
		}
		if (!layoutEditor.isActive(screen)) {
			renderTooltips(event, state);
		}
		renderLayoutEditor(event, screen);
	}

	private void updateDynamicLayout(AbstractContainerScreen<?> screen, InjectedScreenState state) {
		int deltaX = screen.getLeftPos() - state.lastGuiLeft;
		int deltaY = screen.getTopPos() - state.lastGuiTop;
		if (deltaX == 0 && deltaY == 0) {
			return;
		}

		if (state.searchBox != null) {
			state.searchBox.moveBy(deltaX, deltaY);
		}
		// The no-results label is anchored to the container slot, not the control group.
		moveWidget(state.noResultsLabel, deltaX, deltaY);
		moveWidget(state.sortContainerButton, deltaX, deltaY);
		moveWidget(state.sortByButton, deltaX, deltaY);
		moveWidget(state.transferToPlayerButton, deltaX, deltaY);
		moveWidget(state.transferToContainerButton, deltaX, deltaY);
		state.lastGuiLeft = screen.getLeftPos();
		state.lastGuiTop = screen.getTopPos();
		state.hasNoResultsSampledBackgroundColor = false;
		state.noResultsSampleGeneration++;
	}

	private void updateDynamicLayout(AbstractContainerScreen<?> screen, PlayerOnlyScreenState state) {
		int deltaX = screen.getLeftPos() - state.lastGuiLeft;
		int deltaY = screen.getTopPos() - state.lastGuiTop;
		if (deltaX == 0 && deltaY == 0) {
			return;
		}

		moveWidget(state.sortPlayerButton, deltaX, deltaY);
		state.lastGuiLeft = screen.getLeftPos();
		state.lastGuiTop = screen.getTopPos();
	}

	private void moveWidget(@Nullable WidgetBase widget, int deltaX, int deltaY) {
		if (widget != null) {
			widget.setPosition(new Position(widget.getX() + deltaX, widget.getY() + deltaY));
		}
	}

	private void updateNoResultsBackgroundColor(InjectedScreenState state) {
		if (state.noResultsLabel == null) {
			return;
		}
		if (!state.noResultsLabel.isVisibleLabel()) {
			if (state.noResultsVisibleFrames > 0) {
				state.noResultsSampleGeneration++;
			}
			state.hasNoResultsSampledBackgroundColor = false;
			state.noResultsVisibleFrames = 0;
			return;
		}
		state.noResultsVisibleFrames++;

		if (!state.hasNoResultsSampledBackgroundColor && !state.noResultsSamplePending && state.noResultsVisibleFrames > 2) {
			SlotPosition topLeftSlotPosition = state.containerTopLeftSlotPosition;
			int sampleGuiX = state.screen.getLeftPos() + topLeftSlotPosition.x();
			int sampleGuiY = state.screen.getTopPos() + topLeftSlotPosition.y() - 2;
			samplePixelColor(state, sampleGuiX, sampleGuiY);
		}
		state.noResultsLabel.setBackgroundColor(state.noResultsSampledBackgroundColor);
	}

	private void samplePixelColor(InjectedScreenState state, int guiX, int guiY) {
		Window window = Minecraft.getInstance().getWindow();
		int scaledWidth = window.getGuiScaledWidth();
		int scaledHeight = window.getGuiScaledHeight();
		if (scaledWidth <= 0 || scaledHeight <= 0) {
			return;
		}
		GpuTexture colorTexture = Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTexture();
		if (colorTexture == null || colorTexture.getFormat() != GpuFormat.RGBA8_UNORM || colorTexture.getWidth(0) != window.getScreenWidth()
				|| colorTexture.getHeight(0) != window.getScreenHeight()) {
			return;
		}

		int clampedGuiX = Math.max(0, Math.min(guiX, scaledWidth - 1));
		int clampedGuiY = Math.max(0, Math.min(guiY, scaledHeight - 1));
		int fbX = (int) Math.floor((double) clampedGuiX * window.getScreenWidth() / scaledWidth);
		int fbYTop = (int) Math.floor((double) clampedGuiY * window.getScreenHeight() / scaledHeight);
		int fbY = Math.max(0, Math.min(window.getScreenHeight() - 1, window.getScreenHeight() - 1 - fbYTop));

		// ScreenEvent.Render.Post runs during GUI extraction, before this frame is drawn; read the previous completed frame.
		GpuBuffer pixelBuffer = RenderSystem.getDevice().createBuffer(() -> "SII no-results background pixel",
				GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, 4);
		int sampleGeneration = state.noResultsSampleGeneration;
		state.noResultsSamplePending = true;
		RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(colorTexture, pixelBuffer, 0L, () -> {
			try (GpuBufferSlice.MappedView mappedPixel = pixelBuffer.map(true, false)) {
				if (states.get(state.screen) == state && state.noResultsSampleGeneration == sampleGeneration && state.noResultsLabel.isVisibleLabel()) {
					var data = mappedPixel.data();
					state.noResultsSampledBackgroundColor = 0xFF000000 | Byte.toUnsignedInt(data.get(0)) << 16 | Byte.toUnsignedInt(data.get(1)) << 8
							| Byte.toUnsignedInt(data.get(2));
					state.hasNoResultsSampledBackgroundColor = true;
				}
			} finally {
				pixelBuffer.close();
				state.noResultsSamplePending = false;
			}
		}, 0, fbX, fbY, 1, 1);
	}

	private void renderTooltips(ScreenEvent.Render.Post event, InjectedScreenState state) {
		int mouseX = event.getMouseX();
		int mouseY = event.getMouseY();
		GuiGraphicsExtractor guiGraphics = event.getGuiGraphics();
		if (state.searchBox != null) {
			state.searchBox.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		}
		if (state.sortContainerButton != null) {
			state.sortContainerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		}
		if (state.sortByButton != null) {
			state.sortByButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		}
		if (state.transferToPlayerButton != null && state.transferToContainerButton != null) {
			state.transferToPlayerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
			state.transferToContainerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		}
	}

	private void applySearchFilter(InjectedScreenState state) {
		if (layoutEditor.isActive(state.screen)) {
			restoreSlotPositions(state);
			return;
		}
		if (state.searchBox == null) {
			state.searchPhrase = "";
			return;
		}
		String phrase = state.searchBox.getValue();
		state.searchPhrase = phrase;
		if (isRememberSearchPhraseEnabled()) {
			SharedSearchPhraseMemory.setSearchPhrase(phrase);
		}
		if (phrase.isBlank()) {
			restoreSlotPositions(state);
			if (state.noResultsLabel != null) {
				state.noResultsLabel.setVisible(false);
			}
			return;
		}
		int visibleSlots = 0;
		int nextVisiblePosition = 0;
		for (int slotIndex : state.filteredContainerSlotIndexes) {
			Slot slot = state.screen.getMenu().getSlot(slotIndex);
			if (slot.hasItem() && SearchPhraseMatcher.matches(Minecraft.getInstance(), slot.getItem(), phrase)) {
				SlotPosition targetPosition = state.containerVisiblePositions.get(nextVisiblePosition);
				setSlotPositionIfDifferent(slot, targetPosition.x(), targetPosition.y());
				nextVisiblePosition++;
				visibleSlots++;
			} else if (slot.x != DISABLED_SLOT_X_POS) {
				slot.x = DISABLED_SLOT_X_POS;
			}
		}
		if (state.noResultsLabel != null) {
			state.noResultsLabel.setVisible(visibleSlots == 0);
		}
	}

	private void restoreSlotPositions(InjectedScreenState state) {
		for (int slotIndex : state.filteredContainerSlotIndexes) {
			SlotPosition originalPosition = state.originalSlotPositions.get(slotIndex);
			if (originalPosition == null) {
				continue;
			}
			Slot slot = state.screen.getMenu().getSlot(slotIndex);
			setSlotPositionIfDifferent(slot, originalPosition.x(), originalPosition.y());
		}
	}

	private void setSlotPositionIfDifferent(Slot slot, int x, int y) {
		if (slot.x != x) {
			slot.x = x;
		}
		if (slot.y != y) {
			slot.y = y;
		}
	}

	public void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
		if (layoutEditor.mousePressed(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
			event.setCanceled(true);
			return;
		}
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		if (tryHandleSortKeybind(event.getScreen(), InputConstants.Type.MOUSE.getOrCreate(event.getButton())) || tryHandleTransferKeybind(event.getScreen(),
				InputConstants.Type.MOUSE.getOrCreate(event.getButton()), !Minecraft.getInstance().hasShiftDown())) {
			event.setCanceled(true);
			return;
		}
		InjectedScreenState state = states.get(screen);
		if (state == null) {
			return;
		}

		if (state.searchBox != null && event.getButton() == InputConstants.MOUSE_BUTTON_LEFT && state.searchBox.isFocused()
				&& !state.searchBox.isMouseOver(event.getMouseX(), event.getMouseY())) {
			state.searchBox.setFocused(false);
		}

		if (state.searchPhrase.isBlank()) {
			return;
		}

		Slot slot = screen.getHoveredSlot();
		if (slot == null) {
			return;
		}
		int slotId = screen.getMenu().slots.indexOf(slot);
		if (!state.filteredContainerSlotIndexesSet.contains(slotId)) {
			return;
		}
		if (slot.hasItem() && SearchPhraseMatcher.matches(Minecraft.getInstance(), slot.getItem(), state.searchPhrase)) {
			return;
		}
		event.setCanceled(true);
	}

	public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (layoutEditor.isActive(event.getScreen())) {
			if (event.getKeyEvent().key() == InputConstants.KEY_ESCAPE || Minecraft.getInstance().options.keyInventory.matches(event.getKeyEvent())) {
				event.getScreen().onClose();
			}
			event.setCanceled(true);
			return;
		}
		InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
		boolean shiftDown = Minecraft.getInstance().hasShiftDown() || (event.getModifiers() & SDLKeycode.SDL_KMOD_SHIFT) != 0;
		if (tryHandleSortKeybind(event.getScreen(), key) || tryHandleTransferKeybind(event.getScreen(), key, !shiftDown)) {
			event.setCanceled(true);
		}
	}

	private boolean tryHandleTransferKeybind(Screen screen, InputConstants.Key inputKey, boolean transferAll) {
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return false;
		}
		if (screen instanceof StorageScreenBase<?>) {
			return false;
		}
		CachedScreenContext context = screenContexts.get(containerScreen);
		if (context == null || !context.transferEligibility().eligible()) {
			return false;
		}

		InteractionActionType actionType;
		if (matchesTransferToStorageKeybind(inputKey)) {
			actionType = InteractionActionType.TRANSFER_TO_CONTAINER;
		} else if (matchesTransferToInventoryKeybind(inputKey)) {
			actionType = InteractionActionType.TRANSFER_TO_PLAYER;
		} else {
			return false;
		}

		sendContainerInteraction(containerScreen, actionType, transferAll, SortBy.NAME);
		GuiSoundHelper.playButtonClickSound();
		return true;
	}

	private boolean tryHandleSortKeybind(Screen screen, InputConstants.Key inputKey) {
		if (!matchesSortKeybind(inputKey)) {
			return false;
		}
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return false;
		}
		if (screen instanceof CreativeModeInventoryScreen) {
			return false;
		}
		if (isPlayerOnlyMenu(containerScreen)) {
			if (!isEligibleForPlayerSort(containerScreen)) {
				return false;
			}
			ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_PLAYER, true, SortBy.NAME));
			GuiSoundHelper.playButtonClickSound();
			return true;
		}

		Slot slotUnderMouse = getHoveredSlot(containerScreen);
		if (slotUnderMouse != null && isPlayerInventorySlot(slotUnderMouse)) {
			if (isEligibleForPlayerSort(containerScreen)) {
				ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_PLAYER, true, SortBy.NAME));
				GuiSoundHelper.playButtonClickSound();
				return true;
			}
			return false;
		}
		if (!(screen instanceof StorageScreenBase<?>) && !isEligibleForInjectedInteractions(containerScreen)) {
			return false;
		}

		if (screen instanceof StorageScreenBase<?>) {
			return false;
		}

		InjectedScreenState state = states.get(containerScreen);
		sendContainerInteraction(containerScreen, InteractionActionType.SORT_CONTAINER, true, state == null ? SortBy.NAME : state.sortByState.getSortBy());
		GuiSoundHelper.playButtonClickSound();
		return true;
	}

	private Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
		if (screen instanceof StorageScreenBase<?> storageScreen) {
			Minecraft mc = Minecraft.getInstance();
			double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
			double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
			return storageScreen.findSlot(mouseX, mouseY);
		}
		return screen.getHoveredSlot();
	}

	private boolean matchesSortKeybind(InputConstants.Key inputKey) {
		return !SORT_KEYBIND.isUnbound() && SORT_KEYBIND.getKey().equals(inputKey);
	}

	private boolean matchesTransferToStorageKeybind(InputConstants.Key inputKey) {
		return matchesKeybind(TRANSFER_TO_STORAGE_KEYBIND, inputKey);
	}

	private boolean matchesTransferToInventoryKeybind(InputConstants.Key inputKey) {
		return matchesKeybind(TRANSFER_TO_INVENTORY_KEYBIND, inputKey);
	}

	private boolean matchesKeybind(net.minecraft.client.KeyMapping keyMapping, InputConstants.Key inputKey) {
		return isActiveAndMatchesIgnoringShift(keyMapping, inputKey);
	}

	private boolean isPlayerInventorySlot(Slot slot) {
		if (!(slot.container instanceof Inventory)) {
			return false;
		}
		int containerSlot = slot.getContainerSlot();
		return containerSlot >= 0 && containerSlot <= 35;
	}

	private boolean isPlayerOnlyMenu(AbstractContainerScreen<?> screen) {
		return screen.getMenu() instanceof InventoryMenu;
	}

	private boolean isEligibleForInjectedInteractions(AbstractContainerScreen<?> screen) {
		CachedScreenContext context = screenContexts.get(screen);
		return context != null && context.eligibility().eligible();
	}

	private boolean isEligibleForPlayerSort(AbstractContainerScreen<?> screen) {
		CachedScreenContext context = screenContexts.get(screen);
		return context != null && context.playerSortEligibility().eligible();
	}

	private void sendContainerInteraction(AbstractContainerScreen<?> screen, InteractionActionType actionType, boolean filterByContents, SortBy sortBy) {
		CachedScreenContext context = screenContexts.get(screen);
		if (context != null) {
			ClientPacketDistributor.sendToServer(
					new ContainerInteractionPayload(actionType, filterByContents, sortBy, context.context().slotRegions().excludedContainerSlotIndexes()));
		}
	}

	private void initPlayerOnlyScreen(ScreenEvent.Init.Post event, CachedScreenContext cachedContext) {
		ScreenContextResolver.ResolvedScreenContext context = cachedContext.context();
		if (!cachedContext.playerSortEligibility().eligible()) {
			return;
		}

		AbstractContainerScreen<?> screen = context.screen();
		Optional<AnchorLayoutService.PlayerSortLayout> layout = anchorLayoutService.getPlayerOnlySortLayout(screen, context.slotRegions());
		if (layout.isEmpty()) {
			return;
		}

		Button sortPlayerButton = buildPlayerSortButton(layout.get().playerSortX(), layout.get().playerSortY());
		event.addListener(sortPlayerButton);
		playerOnlyStates.put(screen, new PlayerOnlyScreenState(sortPlayerButton, screen.getLeftPos(), screen.getTopPos()));
	}

	ClientScreenHistory getScreenHistory() {
		return screenHistory;
	}

	private void renderLayoutEditor(ScreenEvent.Render.Post event, AbstractContainerScreen<?> screen) {
		List<LayoutAdjustmentEditor.Group> groups = new ArrayList<>();
		InjectedScreenState state = states.get(screen);
		if (state != null && state.sortContainerButton != null) {
			List<WidgetBase> widgets = new ArrayList<>();
			widgets.add(state.sortContainerButton);
			if (state.sortByButton != null) {
				widgets.add(state.sortByButton);
			}
			groups.add(new LayoutAdjustmentEditor.Group(Anchor.CONTAINER, () -> {
				LayoutAdjustmentEditor.Bounds bounds = widgetBounds(widgets);
				if (state.searchBox == null) {
					return bounds;
				}
				return union(bounds, new LayoutAdjustmentEditor.Bounds(state.searchBox.maximizedX - 2, state.searchBox.getY() - 2,
						state.searchBox.maximizedWidth + 4, state.searchBox.getHeight() + 4));
			}, (dx, dy) -> {
				widgets.forEach(widget -> moveWidget(widget, dx, dy));
				if (state.searchBox != null) {
					state.searchBox.moveBy(dx, dy);
				}
			}));
		}
		List<WidgetBase> playerWidgets = new ArrayList<>();
		if (state != null && state.transferToPlayerButton != null && state.transferToContainerButton != null) {
			playerWidgets.add(state.transferToPlayerButton);
			playerWidgets.add(state.transferToContainerButton);
		}
		PlayerOnlyScreenState playerState = playerOnlyStates.get(screen);
		if (playerState != null) {
			playerWidgets.add(playerState.sortPlayerButton);
		}
		SophisticatedScreenState nativeState = sophisticatedStates.get(screen);
		if (nativeState != null && nativeState.sortPlayerButton != null) {
			playerWidgets.add(nativeState.sortPlayerButton);
		}
		if (screen instanceof StorageScreenBase<?> storageScreen && storageScreen.getTransferToInventoryButtonPosition().isPresent()) {
			groups.add(new LayoutAdjustmentEditor.Group(Anchor.PLAYER, () -> {
				Position position = storageScreen.getTransferToInventoryButtonPosition().orElseThrow();
				LayoutAdjustmentEditor.Bounds bounds = new LayoutAdjustmentEditor.Bounds(position.x() - BUTTON_SIZE - 2, position.y() - 2, BUTTON_SIZE * 2 + 4,
						BUTTON_SIZE + 4);
				return playerWidgets.isEmpty() ? bounds : union(bounds, widgetBounds(playerWidgets));
			}, (dx, dy) -> {
				if (nativeState != null) {
					updateSophisticatedLayout(storageScreen, nativeState);
				}
			}));
		} else if (!playerWidgets.isEmpty()) {
			groups.add(new LayoutAdjustmentEditor.Group(Anchor.PLAYER, () -> widgetBounds(playerWidgets),
					(dx, dy) -> playerWidgets.forEach(widget -> moveWidget(widget, dx, dy))));
		}
		layoutEditor.render(screen, groups, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
		if (layoutEditor.isActive(screen) && state != null) {
			if (state.searchBox != null) {
				state.searchBox.setFocused(false);
			}
			restoreSlotPositions(state);
			if (state.noResultsLabel != null) {
				state.noResultsLabel.setVisible(false);
			}
		}
	}

	private LayoutAdjustmentEditor.Bounds widgetBounds(List<WidgetBase> widgets) {
		int left = widgets.stream().mapToInt(WidgetBase::getX).min().orElse(0) - 2;
		int top = widgets.stream().mapToInt(WidgetBase::getY).min().orElse(0) - 2;
		int right = widgets.stream().mapToInt(widget -> widget.getX() + widget.getWidth()).max().orElse(0) + 2;
		int bottom = widgets.stream().mapToInt(widget -> widget.getY() + widget.getHeight()).max().orElse(0) + 2;
		return new LayoutAdjustmentEditor.Bounds(left, top, right - left, bottom - top);
	}

	private LayoutAdjustmentEditor.Bounds union(LayoutAdjustmentEditor.Bounds first, LayoutAdjustmentEditor.Bounds second) {
		int left = Math.min(first.x(), second.x());
		int top = Math.min(first.y(), second.y());
		return new LayoutAdjustmentEditor.Bounds(left, top, Math.max(first.right(), second.right()) - left, Math.max(first.bottom(), second.bottom()) - top);
	}

	LayoutAdjustmentEditor getLayoutEditor() {
		return layoutEditor;
	}

	private void initSophisticatedScreen(ScreenEvent.Init.Post event, StorageScreenBase<?> storageScreen, SlotRegions regions, boolean showPlayerSort) {
		Optional<AnchorLayoutService.PlayerSortLayout> layout = anchorLayoutService.getPlayerOnlySortLayout(storageScreen, regions);
		if (layout.isEmpty()) {
			return;
		}

		Button sortPlayerButton = showPlayerSort ? buildPlayerSortButton(layout.get().playerSortX(), layout.get().playerSortY()) : null;
		if (sortPlayerButton != null) {
			event.addListener(sortPlayerButton);
		}
		SophisticatedScreenState state = new SophisticatedScreenState(sortPlayerButton);
		sophisticatedStates.put(storageScreen, state);
		updateSophisticatedLayout(storageScreen, state);
	}

	private void updateSophisticatedLayout(StorageScreenBase<?> screen, SophisticatedScreenState state) {
		Offset offset = ScreenLayoutOffsets.get(screen.getClass().getName(), Anchor.PLAYER);
		int reserve = state.sortPlayerButton == null ? 0 : SOPHISTICATED_TRANSFER_SHIFT;
		screen.setTransferButtonsOffset(offset.x() - reserve, offset.y());
		screen.getTransferToInventoryButtonPosition().ifPresent(position -> {
			if (state.sortPlayerButton != null) {
				Offset legacy = ScreenLayoutOffsets.parse(screen.getClass().getName(), Config.CLIENT.playerSortOffsetOverrides.get());
				state.sortPlayerButton.setPosition(new Position(position.x() + reserve + legacy.x(), position.y() + legacy.y()));
			}
		});
	}

	private void syncSharedSearchFromStorageScreen(StorageScreenBase<?> storageScreen) {
		if (!isRememberSearchPhraseEnabled()) {
			return;
		}
		StorageContainerMenuBase<?> storageMenu = storageScreen.getMenu();
		if (!storageMenu.shouldKeepSearchPhrase()) {
			return;
		}

		String searchPhrase = storageMenu.getSearchPhrase();
		if (!searchPhrase.equals(SharedSearchPhraseMemory.getSearchPhrase())) {
			SharedSearchPhraseMemory.setSearchPhrase(searchPhrase);
		}
	}

	private void syncSharedSearchToStorageScreen(StorageScreenBase<?> storageScreen) {
		if (!isRememberSearchPhraseEnabled()) {
			return;
		}
		StorageContainerMenuBase<?> storageMenu = storageScreen.getMenu();
		if (!storageMenu.shouldKeepSearchPhrase()) {
			return;
		}

		String sharedSearchPhrase = SharedSearchPhraseMemory.getSearchPhrase();
		if (!sharedSearchPhrase.equals(storageMenu.getSearchPhrase())) {
			storageScreen.setExternalSearchPhrase(sharedSearchPhrase);
		}
	}

	private boolean isRememberSearchPhraseEnabled() {
		return Config.CLIENT.rememberSearchPhrase.get();
	}

	private AnchorLayoutService.ContainerControls getContainerControls(SlotRegions regions) {
		int actionableContainerSlotCount = regions.actionableContainerSlotCount();
		return new AnchorLayoutService.ContainerControls(actionableContainerSlotCount > Config.COMMON.hideSearchAtOrBelowActionableContainerSlots.get(),
				actionableContainerSlotCount > Config.COMMON.hideSortByAtOrBelowActionableContainerSlots.get());
	}

	private InjectedScreenState createState(AbstractContainerScreen<?> screen, SlotRegions regions, AnchorLayoutService.AnchorLayout layout, boolean container,
			boolean transfers) {
		List<Integer> filteredContainerSlotIndexes = new ArrayList<>(regions.actionableContainerSlotIndexes());
		Map<Integer, SlotPosition> originalSlotPositions = new HashMap<>();
		for (int slotIndex : filteredContainerSlotIndexes) {
			Slot slot = screen.getMenu().getSlot(slotIndex);
			originalSlotPositions.put(slotIndex, new SlotPosition(slot.x, slot.y));
		}
		List<SlotPosition> containerVisiblePositions = filteredContainerSlotIndexes.stream().map(originalSlotPositions::get).toList();
		SlotPosition containerTopLeftSlotPosition = containerVisiblePositions.stream()
				.min((p1, p2) -> p1.y() == p2.y() ? Integer.compare(p1.x(), p2.x()) : Integer.compare(p1.y(), p2.y())).orElse(new SlotPosition(0, 0));
		int containerVisibleWidth = containerVisiblePositions.stream().mapToInt(SlotPosition::x).max().orElse(containerTopLeftSlotPosition.x())
				- containerTopLeftSlotPosition.x() + SLOT_SIZE;

		@Nullable
		InteractionSearchBox searchBox = null;
		@Nullable
		NoResultsLabel noResultsLabel = null;
		if (container && layout.searchLayout() != null) {
			searchBox = new InteractionSearchBox(new Position(layout.searchLayout().x(), layout.searchLayout().y()),
					new Dimension(layout.searchLayout().width(), layout.searchLayout().height()));
			searchBox.setRenderInDefaultPass(false);
			noResultsLabel = new NoResultsLabel(
					new Position(screen.getLeftPos() + containerTopLeftSlotPosition.x(), screen.getTopPos() + containerTopLeftSlotPosition.y()),
					containerVisibleWidth, Component.translatable(TranslationHelper.INSTANCE.translGui("label.no_search_results")));
			noResultsLabel.setRenderInDefaultPass(false);
			noResultsLabel.setVisible(false);
		}
		SortByState sortByState = new SortByState();
		@Nullable
		Button sortContainer = container
				? buildSortButton(screen, layout.containerSortLayout().x(), layout.containerSortLayout().y(), InteractionActionType.SORT_CONTAINER, sortByState)
				: null;
		@Nullable
		ToggleButton<SortBy> sortByButton = !container || layout.sortByLayout() == null
				? null
				: buildSortByButton(layout.sortByLayout().x(), layout.sortByLayout().y(), sortByState);
		@Nullable
		Button transferToContainer = transfers
				? buildTransferButton(screen, layout.transferToPlayerX(), layout.transferToPlayerY(), InteractionActionType.TRANSFER_TO_CONTAINER, true)
				: null;
		@Nullable
		Button transferToPlayer = transfers
				? buildTransferButton(screen, layout.transferToContainerX(), layout.transferToContainerY(), InteractionActionType.TRANSFER_TO_PLAYER, false)
				: null;

		InjectedScreenState state = new InjectedScreenState(screen, filteredContainerSlotIndexes, Set.copyOf(filteredContainerSlotIndexes),
				containerVisiblePositions, containerTopLeftSlotPosition, originalSlotPositions, searchBox, noResultsLabel, sortContainer, sortByButton,
				transferToPlayer, transferToContainer, sortByState);
		if (searchBox != null) {
			searchBox.setResponder(v -> applySearchFilter(state));
		}
		return state;
	}

	private Button buildSortButton(AbstractContainerScreen<?> screen, int x, int y, InteractionActionType actionType, SortByState sortByState) {
		return new ImmediateTooltipButton(new Position(x, y), ButtonDefinitions.SORT, mouseButton -> {
			if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
				sendContainerInteraction(screen, actionType, true, sortByState.getSortBy());
			}
		});
	}

	private ToggleButton<SortBy> buildSortByButton(int x, int y, SortByState sortByState) {
		return new ToggleButton<>(new Position(x, y), ButtonDefinitions.SORT_BY, mouseButton -> {
			if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
				sortByState.nextSortBy();
				ClientPacketDistributor.sendToServer(new SetSortMemoryPayload(sortByState.getSortBy()));
			}
		}, sortByState::getSortBy);
	}

	private Button buildPlayerSortButton(int x, int y) {
		return new ImmediateTooltipButton(new Position(x, y), ButtonDefinitions.SORT, mouseButton -> {
			if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
				ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_PLAYER, true, SortBy.NAME));
			}
		});
	}

	private Button buildTransferButton(AbstractContainerScreen<?> screen, int x, int y, InteractionActionType actionType, boolean toContainer) {
		ButtonDefinition filteredDefinition = toContainer ? ButtonDefinitions.TRANSFER_TO_STORAGE_FILTERED : ButtonDefinitions.TRANSFER_TO_INVENTORY_FILTERED;
		ButtonDefinition allDefinition = toContainer ? ButtonDefinitions.TRANSFER_TO_STORAGE : ButtonDefinitions.TRANSFER_TO_INVENTORY;
		return new TransferButton(new Position(x, y), filterByContents -> sendContainerInteraction(screen, actionType, filterByContents, SortBy.NAME),
				filteredDefinition, allDefinition);
	}

	private static class InjectedScreenState {
		private final AbstractContainerScreen<?> screen;
		private final List<Integer> filteredContainerSlotIndexes;
		private final Set<Integer> filteredContainerSlotIndexesSet;
		private final List<SlotPosition> containerVisiblePositions;
		private final SlotPosition containerTopLeftSlotPosition;
		private final Map<Integer, SlotPosition> originalSlotPositions;
		@Nullable
		private final InteractionSearchBox searchBox;
		@Nullable
		private final NoResultsLabel noResultsLabel;
		@Nullable
		private final Button sortContainerButton;
		@Nullable
		private final ToggleButton<SortBy> sortByButton;
		@Nullable
		private final Button transferToPlayerButton;
		@Nullable
		private final Button transferToContainerButton;
		private final SortByState sortByState;
		private int lastGuiLeft;
		private int lastGuiTop;
		private int noResultsSampledBackgroundColor = DEFAULT_NO_RESULTS_BG_COLOR;
		private boolean hasNoResultsSampledBackgroundColor = false;
		private boolean noResultsSamplePending = false;
		private int noResultsSampleGeneration = 0;
		private int noResultsVisibleFrames = 0;
		private String searchPhrase = "";

		private InjectedScreenState(AbstractContainerScreen<?> screen, List<Integer> filteredContainerSlotIndexes, Set<Integer> filteredContainerSlotIndexesSet,
				List<SlotPosition> containerVisiblePositions, SlotPosition containerTopLeftSlotPosition, Map<Integer, SlotPosition> originalSlotPositions,
				@Nullable InteractionSearchBox searchBox, @Nullable NoResultsLabel noResultsLabel, @Nullable Button sortContainerButton,
				@Nullable ToggleButton<SortBy> sortByButton, @Nullable Button transferToPlayerButton, @Nullable Button transferToContainerButton,
				SortByState sortByState) {
			this.screen = screen;
			this.filteredContainerSlotIndexes = filteredContainerSlotIndexes;
			this.filteredContainerSlotIndexesSet = filteredContainerSlotIndexesSet;
			this.containerVisiblePositions = containerVisiblePositions;
			this.containerTopLeftSlotPosition = containerTopLeftSlotPosition;
			this.originalSlotPositions = originalSlotPositions;
			this.searchBox = searchBox;
			this.noResultsLabel = noResultsLabel;
			this.sortContainerButton = sortContainerButton;
			this.sortByButton = sortByButton;
			this.transferToPlayerButton = transferToPlayerButton;
			this.transferToContainerButton = transferToContainerButton;
			this.sortByState = sortByState;
			lastGuiLeft = screen.getLeftPos();
			lastGuiTop = screen.getTopPos();
		}

		private boolean hasSearchBox() {
			return searchBox != null;
		}

		private String getSearchPhrase() {
			return searchBox == null ? searchPhrase : searchBox.getValue();
		}
	}

	private static class SortByState {
		private SortBy sortBy = SortBy.NAME;

		private SortBy getSortBy() {
			return sortBy;
		}

		private void nextSortBy() {
			sortBy = sortBy.next();
		}

		private void setSortBy(SortBy sortBy) {
			this.sortBy = sortBy;
		}
	}

	private record SophisticatedScreenState(@Nullable Button sortPlayerButton) {
	}

	private record CachedScreenContext(ScreenContextResolver.ResolvedScreenContext context, EligibilityDecision eligibility,
			EligibilityDecision transferEligibility, EligibilityDecision playerSortEligibility) {
	}

	private static class PlayerOnlyScreenState {
		private final Button sortPlayerButton;
		private int lastGuiLeft;
		private int lastGuiTop;

		private PlayerOnlyScreenState(Button sortPlayerButton, int lastGuiLeft, int lastGuiTop) {
			this.sortPlayerButton = sortPlayerButton;
			this.lastGuiLeft = lastGuiLeft;
			this.lastGuiTop = lastGuiTop;
		}
	}

	private static class NoResultsLabel extends WidgetBase {
		private static final int TEXT_COLOR = ARGB.opaque(4210752);
		private static final int LINE_HEIGHT = 9;
		private int backgroundColor = DEFAULT_NO_RESULTS_BG_COLOR;
		private final List<net.minecraft.util.FormattedCharSequence> lines;

		private NoResultsLabel(Position position, int maxWidth, Component labelText) {
			super(position, new Dimension(maxWidth, LINE_HEIGHT));
			lines = Minecraft.getInstance().font.split(labelText, maxWidth);
			updateDimensions(maxWidth, Math.max(LINE_HEIGHT, lines.size() * LINE_HEIGHT));
		}

		private void setBackgroundColor(int backgroundColor) {
			this.backgroundColor = backgroundColor;
		}

		private boolean isVisibleLabel() {
			return visible;
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
			return false;
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return false;
		}

		@Override
		protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
			for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
				guiGraphics.text(minecraft.font, lines.get(lineIndex), x, y + lineIndex * LINE_HEIGHT, TEXT_COLOR, false);
			}
		}

		@Override
		protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			guiGraphics.fill(x - 2, y - 1, x + getWidth() + 2, y + getHeight() + 1, backgroundColor);
		}

		@Override
		public void updateNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
			// no-op
		}
	}

	private record SlotPosition(int x, int y) {
	}

	private static class TransferButton extends ImmediateTooltipButton {
		private final ButtonDefinition filteredDefinition;
		private final ButtonDefinition allDefinition;

		private TransferButton(Position position, Consumer<Boolean> transferAction, ButtonDefinition filteredDefinition, ButtonDefinition allDefinition) {
			super(position, filteredDefinition, mouseButton -> {
				if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
					transferAction.accept(!Minecraft.getInstance().hasShiftDown());
				}
			});
			this.filteredDefinition = filteredDefinition;
			this.allDefinition = allDefinition;
		}

		@Override
		protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
			if (Minecraft.getInstance().hasShiftDown()) {
				GuiHelper.blit(guiGraphics, x, y, allDefinition.getForegroundTexture());
			} else {
				GuiHelper.blit(guiGraphics, x, y, filteredDefinition.getForegroundTexture());
			}
		}

		@Override
		protected List<Component> getTooltip() {
			return Minecraft.getInstance().hasShiftDown() ? allDefinition.getTooltip() : filteredDefinition.getTooltip();
		}
	}

	private static class ImmediateTooltipButton extends Button {
		private ImmediateTooltipButton(Position position, ButtonDefinition buttonDefinition, IntConsumer onClick) {
			super(position, buttonDefinition, onClick);
		}

		@Override
		public void extractTooltip(Screen screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
			if (visible && isMouseOver(mouseX, mouseY)) {
				GuiHelper.extractTooltip(screen, guiGraphics, getTooltip(), mouseX, mouseY);
			}
		}
	}

	private static class InteractionSearchBox extends TextBox {
		private static final String MAGNIFYING_GLASS = "\uD83D\uDD0D";
		private static final int UNFOCUSED_COLOR = ARGB.opaque(0xBBBBBB);
		private long lastFocusChangeTime = 0;
		private int maximizedX;
		private final int maximizedWidth;

		private InteractionSearchBox(Position position, Dimension dimension) {
			super(position, dimension);
			setTextColor(UNFOCUSED_COLOR);
			setTextColorUneditable(UNFOCUSED_COLOR);
			setBordered(false);
			setMaxLength(50);
			setUnfocusedEmptyHint(MAGNIFYING_GLASS);
			maximizedX = position.x();
			maximizedWidth = dimension.width();
		}

		private void moveBy(int deltaX, int deltaY) {
			maximizedX += deltaX;
			setPosition(new Position(getX() + deltaX, getY() + deltaY));
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
			if (!isMouseOver(event.x(), event.y())) {
				return false;
			}

			if (isEditable()) {
				if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
					setFocused(true);
				} else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
					setValue("");
				}
				return true;
			}
			return super.mouseClicked(event, doubleClicked);
		}

		@Override
		public void setFocused(boolean focused) {
			if (isFocused() != focused) {
				lastFocusChangeTime = System.currentTimeMillis();
			}
			super.setFocused(focused);
			setTextColor(focused ? -1 : UNFOCUSED_COLOR);
		}

		@Override
		protected void extractBg(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
			int minWidth = getHeight();
			if ((isFocused() && maximizedWidth > getWidth()) || (!isFocused() && getValue().isEmpty() && getWidth() > minWidth)) {
				float ratio = Easing.EASE_IN_OUT_CUBIC.ease(Math.min((System.currentTimeMillis() - lastFocusChangeTime) / 200f, 1));
				int currentWidth = isFocused()
						? (int) (minWidth + (maximizedWidth - minWidth) * ratio)
						: (int) (maximizedWidth - (maximizedWidth - minWidth) * ratio);
				setPosition(new Position(maximizedX + maximizedWidth - currentWidth, y));
				updateDimensions(currentWidth, getHeight());
			}

			guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF777777);
		}

		@Override
		public void extractTooltip(Screen screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
			if (!isFocused() && isMouseOver(mouseX, mouseY)) {
				GuiHelper.extractTooltip(screen, guiGraphics, List.of(Component.translatable("gui.sophisticatedcore.text_box.search_box"),
						Component.translatable("gui.sophisticatedcore.text_box.search_box_detail").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			}
		}
	}
}
