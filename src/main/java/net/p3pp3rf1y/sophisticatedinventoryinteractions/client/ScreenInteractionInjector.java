package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.*;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.util.Easing;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.AnchorLayoutService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.MenuEligibilityService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.network.ContainerInteractionPayload;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

public class ScreenInteractionInjector {
	private static final int DISABLED_SLOT_X_POS = -2000;
	private static final int BUTTON_GAP = 0;
	private static final int BUTTON_SIZE = 12;
	private static final int SLOT_SIZE = 18;
	private static final int DEFAULT_NO_RESULTS_BG_COLOR = 0xFF777777;
	private static final int SOPHISTICATED_TRANSFER_SHIFT = BUTTON_SIZE + BUTTON_GAP;
	private static final ByteBuffer PIXEL_SAMPLE_BUFFER = MemoryUtil.memAlloc(4);
	private int cachedSampledPixelColor = DEFAULT_NO_RESULTS_BG_COLOR;

	private final ScreenContextResolver contextResolver = new ScreenContextResolver();
	private final MenuEligibilityService menuEligibilityService = new MenuEligibilityService();
	private final AnchorLayoutService anchorLayoutService = new AnchorLayoutService();
	private final Map<AbstractContainerScreen<?>, InjectedScreenState> states = new WeakHashMap<>();
	private final Map<StorageScreenBase<?>, SophisticatedScreenState> sophisticatedStates = new WeakHashMap<>();
	private final Map<AbstractContainerScreen<?>, PlayerOnlyScreenState> playerOnlyStates = new WeakHashMap<>();

	public void onScreenInit(ScreenEvent.Init.Post event) {
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		sophisticatedStates.remove(screen);

		InjectedScreenState previousState = states.remove(screen);
		if (previousState != null) {
			restoreSlotPositions(previousState);
		}
		playerOnlyStates.remove(screen);
		String previousSearch = previousState == null ? "" : previousState.getSearchPhrase();

		Optional<ScreenContextResolver.ResolvedScreenContext> resolvedContext = contextResolver.resolve(screen);
		if (resolvedContext.isEmpty()) {
			return;
		}

		ScreenContextResolver.ResolvedScreenContext context = resolvedContext.get();
		if (context.eligibilityDescriptor().sophisticatedNativeScreen() && screen instanceof StorageScreenBase<?> storageScreen) {
			initSophisticatedScreen(event, storageScreen, context.slotRegions());
			syncSharedSearchToStorageScreen(storageScreen);
			return;
		}

		if (isPlayerOnlyMenu(screen)) {
			initPlayerOnlyScreen(event, screen, context.slotRegions());
			return;
		}

		if (!menuEligibilityService.evaluate(context.eligibilityDescriptor()).eligible()) {
			states.remove(screen);
			return;
		}

		AnchorLayoutService.ContainerControls containerControls = getContainerControls(context.slotRegions());
		Optional<AnchorLayoutService.AnchorLayout> layout = anchorLayoutService.getLayout(screen, context.slotRegions(), containerControls);
		if (layout.isEmpty()) {
			states.remove(screen);
			return;
		}

		InjectedScreenState state = createState(screen, context.slotRegions(), layout.get());
		if (state.hasSearchBox() && isRememberSearchPhraseEnabled() && previousSearch.isEmpty()) {
			previousSearch = SharedSearchPhraseMemory.getSearchPhrase();
		}
		if (state.hasSearchBox() && !previousSearch.isEmpty()) {
			state.searchBox.setValue(previousSearch);
		}
		if (state.searchBox != null) {
			event.addListener(state.searchBox);
		}
		event.addListener(state.sortContainerButton);
		if (state.sortByButton != null) {
			event.addListener(state.sortByButton);
		}
		event.addListener(state.transferToContainerButton);
		event.addListener(state.transferToPlayerButton);
		event.addListener(state.sortPlayerButton);
		if (state.noResultsLabel != null) {
			event.addListener(state.noResultsLabel);
		}
		states.put(screen, state);
		applySearchFilter(state);
	}

	public void onScreenClosing(ScreenEvent.Closing event) {
		if (event.getScreen() instanceof StorageScreenBase<?> storageScreen) {
			syncSharedSearchFromStorageScreen(storageScreen);
			sophisticatedStates.remove(storageScreen);
		}
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}

		InjectedScreenState state = states.remove(screen);
		playerOnlyStates.remove(screen);
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
				sophisticatedState.sortPlayerButton.extractTooltip(storageScreen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
			}
			syncSharedSearchFromStorageScreen(storageScreen);
			return;
		}

		InjectedScreenState state = states.get(screen);
		PlayerOnlyScreenState playerOnlyState = playerOnlyStates.get(screen);
		if (playerOnlyState != null) {
			playerOnlyState.sortPlayerButton.extractTooltip(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
		}
		if (state == null) {
			return;
		}

		applySearchFilter(state);
		updateNoResultsBackgroundColor(state);
		if (state.searchBox != null) {
			state.searchBox.extractRenderStateInLatePass(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0);
		}
		if (state.noResultsLabel != null) {
			state.noResultsLabel.extractRenderStateInLatePass(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), 0);
		}
		renderTooltips(event, state);
	}

	private void updateNoResultsBackgroundColor(InjectedScreenState state) {
		if (state.noResultsLabel == null) {
			return;
		}
		if (!state.noResultsLabel.isVisibleLabel()) {
			state.hasNoResultsSampledBackgroundColor = false;
			state.noResultsVisibleFrames = 0;
			return;
		}
		state.noResultsVisibleFrames++;

		if (!state.hasNoResultsSampledBackgroundColor && state.noResultsVisibleFrames > 2) {
			SlotPosition topLeftSlotPosition = state.containerTopLeftSlotPosition;
			int sampleGuiX = state.screen.getGuiLeft() + topLeftSlotPosition.x();
			int sampleGuiY = state.screen.getGuiTop() + topLeftSlotPosition.y() - 2;
			state.noResultsSampledBackgroundColor = samplePixelColor(sampleGuiX, sampleGuiY);
			state.hasNoResultsSampledBackgroundColor = true;
		}
		state.noResultsLabel.setBackgroundColor(state.noResultsSampledBackgroundColor);
	}

	private int samplePixelColor(int guiX, int guiY) {
		Window window = Minecraft.getInstance().getWindow();
		int scaledWidth = window.getGuiScaledWidth();
		int scaledHeight = window.getGuiScaledHeight();
		if (scaledWidth <= 0 || scaledHeight <= 0) {
			return DEFAULT_NO_RESULTS_BG_COLOR;
		}

		int clampedGuiX = Math.max(0, Math.min(guiX, scaledWidth - 1));
		int clampedGuiY = Math.max(0, Math.min(guiY, scaledHeight - 1));
		int fbX = (int) Math.floor((double) clampedGuiX * window.getScreenWidth() / scaledWidth);
		int fbYTop = (int) Math.floor((double) clampedGuiY * window.getScreenHeight() / scaledHeight);
		int fbY = Math.max(0, Math.min(window.getScreenHeight() - 1, window.getScreenHeight() - 1 - fbYTop));

		readPixelToSampleBuffer(fbX, fbY);
		return cachedSampledPixelColor;
	}

	private int decodeArgb(ByteBuffer data, int pixelOffset) {
		int red = Byte.toUnsignedInt(data.get(pixelOffset));
		int green = Byte.toUnsignedInt(data.get(pixelOffset + 1));
		int blue = Byte.toUnsignedInt(data.get(pixelOffset + 2));
		int alpha = Byte.toUnsignedInt(data.get(pixelOffset + 3));
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

	private void readPixelToSampleBuffer(int fbX, int fbY) {
		PIXEL_SAMPLE_BUFFER.clear();
		GlStateManager._readPixels(fbX, fbY, 1, 1, 6408, 5121, MemoryUtil.memAddress(PIXEL_SAMPLE_BUFFER));
		if (PIXEL_SAMPLE_BUFFER.capacity() >= 4) {
			cachedSampledPixelColor = decodeArgb(PIXEL_SAMPLE_BUFFER, 0);
		}
	}

	private void renderTooltips(ScreenEvent.Render.Post event, InjectedScreenState state) {
		int mouseX = event.getMouseX();
		int mouseY = event.getMouseY();
		GuiGraphicsExtractor guiGraphics = event.getGuiGraphics();
		if (state.searchBox != null) {
			state.searchBox.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		}
		state.sortContainerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		if (state.sortByButton != null) {
			state.sortByButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		}
		state.transferToPlayerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		state.transferToContainerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
		state.sortPlayerButton.extractTooltip(state.screen, guiGraphics, mouseX, mouseY);
	}

	private void applySearchFilter(InjectedScreenState state) {
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
		if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
			return;
		}
		if (tryHandleSortKeybind(event.getScreen(), InputConstants.Type.MOUSE.getOrCreate(event.getButton()))
				|| tryHandleTransferKeybind(event.getScreen(), InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
			event.setCanceled(true);
			return;
		}
		InjectedScreenState state = states.get(screen);
		if (state == null) {
			return;
		}

		if (state.searchBox != null && event.getButton() == 0 && state.searchBox.isFocused() && !state.searchBox.isMouseOver(event.getMouseX(), event.getMouseY())) {
			state.searchBox.setFocused(false);
		}

		if (state.searchPhrase.isBlank()) {
			return;
		}

		Slot slot = screen.getSlotUnderMouse();
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
		InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
		if (tryHandleSortKeybind(event.getScreen(), key) || tryHandleTransferKeybind(event.getScreen(), key)) {
			event.setCanceled(true);
		}
	}

	private boolean tryHandleTransferKeybind(Screen screen, InputConstants.Key inputKey) {
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return false;
		}
		if (screen instanceof StorageScreenBase<?>) {
			return false;
		}
		if (!isEligibleForInjectedInteractions(containerScreen)) {
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

		ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(actionType, !Minecraft.getInstance().hasShiftDown(), SortBy.NAME));
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
		if (isPlayerOnlyMenu(containerScreen)) {
			ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_PLAYER, true, SortBy.NAME));
			GuiSoundHelper.playButtonClickSound();
			return true;
		}
		if (!(screen instanceof StorageScreenBase<?>) && !isEligibleForInjectedInteractions(containerScreen)) {
			return false;
		}

		Slot slotUnderMouse = getHoveredSlot(containerScreen);
		if (slotUnderMouse != null && isPlayerInventorySlot(slotUnderMouse)) {
			ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_PLAYER, true, SortBy.NAME));
			GuiSoundHelper.playButtonClickSound();
			return true;
		}

		if (screen instanceof StorageScreenBase<?>) {
			return false;
		}

		ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_CONTAINER, true, SortBy.NAME));
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
		return screen.getSlotUnderMouse();
	}

	private boolean matchesSortKeybind(InputConstants.Key inputKey) {
		return matchesKeybind(net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.SORT_KEYBIND, inputKey);
	}

	private boolean matchesTransferToStorageKeybind(InputConstants.Key inputKey) {
		return matchesKeybind(net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.TRANSFER_TO_STORAGE_KEYBIND, inputKey);
	}

	private boolean matchesTransferToInventoryKeybind(InputConstants.Key inputKey) {
		return matchesKeybind(net.p3pp3rf1y.sophisticatedcore.client.ClientEventHandler.TRANSFER_TO_INVENTORY_KEYBIND, inputKey);
	}

	private boolean matchesKeybind(net.minecraft.client.KeyMapping keyMapping, InputConstants.Key inputKey) {
		return !keyMapping.isUnbound() && keyMapping.getKey().equals(inputKey);
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
		return contextResolver.resolve(screen)
				.map(ScreenContextResolver.ResolvedScreenContext::eligibilityDescriptor)
				.map(menuEligibilityService::evaluate)
				.map(EligibilityDecision::eligible)
				.orElse(false);
	}

	private void initPlayerOnlyScreen(ScreenEvent.Init.Post event, AbstractContainerScreen<?> screen, SlotRegions regions) {
		Optional<AnchorLayoutService.PlayerSortLayout> layout = anchorLayoutService.getPlayerOnlySortLayout(screen, regions);
		if (layout.isEmpty()) {
			return;
		}

		Button sortPlayerButton = buildPlayerSortButton(layout.get().playerSortX(), layout.get().playerSortY());
		event.addListener(sortPlayerButton);
		playerOnlyStates.put(screen, new PlayerOnlyScreenState(sortPlayerButton));
	}

	private void initSophisticatedScreen(ScreenEvent.Init.Post event, StorageScreenBase<?> storageScreen, SlotRegions regions) {
		Optional<AnchorLayoutService.AnchorLayout> layout = anchorLayoutService.getLayout(storageScreen, regions, new AnchorLayoutService.ContainerControls(false, false));
		if (layout.isEmpty()) {
			return;
		}

		int sortPlayerX = layout.get().playerSortX();
		int sortPlayerY = layout.get().playerSortY();
		storageScreen.setTransferButtonsShift(0);
		Optional<Position> transferToInventoryPosition = storageScreen.getTransferToInventoryButtonPosition();
		if (transferToInventoryPosition.isPresent()) {
			storageScreen.setTransferButtonsShift(-SOPHISTICATED_TRANSFER_SHIFT);
			sortPlayerX = transferToInventoryPosition.get().x();
			sortPlayerY = transferToInventoryPosition.get().y();
		}

		Button sortPlayerButton = buildPlayerSortButton(sortPlayerX, sortPlayerY);
		event.addListener(sortPlayerButton);
		sophisticatedStates.put(storageScreen, new SophisticatedScreenState(sortPlayerButton));
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
		return net.p3pp3rf1y.sophisticatedinventoryinteractions.Config.CLIENT.rememberSearchPhrase.get();
	}

	private AnchorLayoutService.ContainerControls getContainerControls(SlotRegions regions) {
		int actionableContainerSlotCount = regions.actionableContainerSlotCount();
		return new AnchorLayoutService.ContainerControls(
				actionableContainerSlotCount > net.p3pp3rf1y.sophisticatedinventoryinteractions.Config.COMMON.hideSearchAtOrBelowActionableContainerSlots.get(),
				actionableContainerSlotCount > net.p3pp3rf1y.sophisticatedinventoryinteractions.Config.COMMON.hideSortByAtOrBelowActionableContainerSlots.get()
		);
	}

	private InjectedScreenState createState(AbstractContainerScreen<?> screen, SlotRegions regions, AnchorLayoutService.AnchorLayout layout) {
		List<Integer> filteredContainerSlotIndexes = new java.util.ArrayList<>(regions.actionableContainerSlotIndexes());
		Map<Integer, SlotPosition> originalSlotPositions = new HashMap<>();
		for (int slotIndex : filteredContainerSlotIndexes) {
			Slot slot = screen.getMenu().getSlot(slotIndex);
			originalSlotPositions.put(slotIndex, new SlotPosition(slot.x, slot.y));
		}
		List<SlotPosition> containerVisiblePositions = filteredContainerSlotIndexes.stream()
				.map(originalSlotPositions::get)
				.toList();
		SlotPosition containerTopLeftSlotPosition = containerVisiblePositions.stream()
				.min((p1, p2) -> p1.y() == p2.y() ? Integer.compare(p1.x(), p2.x()) : Integer.compare(p1.y(), p2.y()))
				.orElse(new SlotPosition(0, 0));
		int containerVisibleWidth = containerVisiblePositions.stream()
				.mapToInt(SlotPosition::x)
				.max()
				.orElse(containerTopLeftSlotPosition.x()) - containerTopLeftSlotPosition.x() + SLOT_SIZE;

		@Nullable InteractionSearchBox searchBox = null;
		@Nullable NoResultsLabel noResultsLabel = null;
		if (layout.searchLayout() != null) {
			searchBox = new InteractionSearchBox(new Position(layout.searchLayout().x(), layout.searchLayout().y()), new Dimension(layout.searchLayout().width(), layout.searchLayout().height()));
			searchBox.setRenderInDefaultPass(false);
			noResultsLabel = new NoResultsLabel(
					new Position(layout.searchLayout().x(), layout.searchLayout().y() + 13),
					containerVisibleWidth,
					Component.translatable(TranslationHelper.INSTANCE.translGui("label.no_search_results"))
			);
			noResultsLabel.setRenderInDefaultPass(false);
			noResultsLabel.setVisible(false);
		}
		SortByState sortByState = new SortByState();
		Button sortContainer = buildSortButton(layout.containerSortLayout().x(), layout.containerSortLayout().y(), InteractionActionType.SORT_CONTAINER, sortByState);
		@Nullable ToggleButton<SortBy> sortByButton = layout.sortByLayout() == null ? null : buildSortByButton(layout.sortByLayout().x(), layout.sortByLayout().y(), sortByState);
		Button transferToContainer = buildTransferButton(layout.transferToPlayerX(), layout.transferToPlayerY(), InteractionActionType.TRANSFER_TO_CONTAINER, true);
		Button transferToPlayer = buildTransferButton(layout.transferToContainerX(), layout.transferToContainerY(), InteractionActionType.TRANSFER_TO_PLAYER, false);
		Button sortPlayer = buildPlayerSortButton(layout.playerSortX(), layout.playerSortY());

		InjectedScreenState state = new InjectedScreenState(screen, filteredContainerSlotIndexes, Set.copyOf(filteredContainerSlotIndexes), containerVisiblePositions,
				containerTopLeftSlotPosition,
				originalSlotPositions, searchBox, noResultsLabel, sortContainer, sortByButton, transferToPlayer, transferToContainer, sortPlayer);
		if (searchBox != null) {
			searchBox.setResponder(v -> applySearchFilter(state));
		}
		return state;
	}

	private Button buildSortButton(int x, int y, InteractionActionType actionType, SortByState sortByState) {
		Button button = new Button(new Position(x, y), ButtonDefinitions.SORT, mouseButton -> {
			if (mouseButton == 0) {
				ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(actionType, true, sortByState.getSortBy()));
			}
		});
		return button;
	}

	private ToggleButton<SortBy> buildSortByButton(int x, int y, SortByState sortByState) {
		return new ToggleButton<>(new Position(x, y), ButtonDefinitions.SORT_BY, mouseButton -> {
			if (mouseButton == 0) {
				sortByState.nextSortBy();
			}
		}, sortByState::getSortBy);
	}

	private Button buildPlayerSortButton(int x, int y) {
		return new Button(new Position(x, y), ButtonDefinitions.SORT, mouseButton -> {
			if (mouseButton == 0) {
				ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(InteractionActionType.SORT_PLAYER, true, SortBy.NAME));
			}
		});
	}

	private Button buildTransferButton(int x, int y, InteractionActionType actionType, boolean toContainer) {
		ButtonDefinition filteredDefinition = toContainer ? ButtonDefinitions.TRANSFER_TO_STORAGE_FILTERED : ButtonDefinitions.TRANSFER_TO_INVENTORY_FILTERED;
		ButtonDefinition allDefinition = toContainer ? ButtonDefinitions.TRANSFER_TO_STORAGE : ButtonDefinitions.TRANSFER_TO_INVENTORY;
		return new TransferButton(new Position(x, y), filterByContents -> ClientPacketDistributor.sendToServer(new ContainerInteractionPayload(actionType, filterByContents, SortBy.NAME)), filteredDefinition, allDefinition);
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
		private final Button sortContainerButton;
		@Nullable
		private final ToggleButton<SortBy> sortByButton;
		private final Button transferToPlayerButton;
		private final Button transferToContainerButton;
		private final Button sortPlayerButton;
		private int noResultsSampledBackgroundColor = DEFAULT_NO_RESULTS_BG_COLOR;
		private boolean hasNoResultsSampledBackgroundColor = false;
		private int noResultsVisibleFrames = 0;
		private String searchPhrase = "";

		private InjectedScreenState(AbstractContainerScreen<?> screen, List<Integer> filteredContainerSlotIndexes,
				Set<Integer> filteredContainerSlotIndexesSet, List<SlotPosition> containerVisiblePositions,
				SlotPosition containerTopLeftSlotPosition,
				Map<Integer, SlotPosition> originalSlotPositions,
				@Nullable InteractionSearchBox searchBox, @Nullable NoResultsLabel noResultsLabel, Button sortContainerButton, @Nullable ToggleButton<SortBy> sortByButton,
				Button transferToPlayerButton, Button transferToContainerButton, Button sortPlayerButton) {
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
			this.sortPlayerButton = sortPlayerButton;
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
	}

	private record SophisticatedScreenState(Button sortPlayerButton) {
	}

	private record PlayerOnlyScreenState(Button sortPlayerButton) {
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

	private static class TransferButton extends Button {
		private final ButtonDefinition filteredDefinition;
		private final ButtonDefinition allDefinition;

		private TransferButton(Position position, Consumer<Boolean> transferAction, ButtonDefinition filteredDefinition, ButtonDefinition allDefinition) {
			super(position, filteredDefinition, mouseButton -> {
				if (mouseButton == 0) {
					transferAction.accept(!Minecraft.getInstance().hasShiftDown());
				}
			});
			this.filteredDefinition = filteredDefinition;
			this.allDefinition = allDefinition;
		}

		@Override
		protected void extractWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
			if (Minecraft.getInstance().hasShiftDown()) {
				net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.blit(guiGraphics, x, y, allDefinition.getForegroundTexture());
			} else {
				net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper.blit(guiGraphics, x, y, filteredDefinition.getForegroundTexture());
			}
		}

		@Override
		protected List<Component> getTooltip() {
			return Minecraft.getInstance().hasShiftDown() ? allDefinition.getTooltip() : filteredDefinition.getTooltip();
		}
	}

	private static class InteractionSearchBox extends TextBox {
		private static final String MAGNIFYING_GLASS = "\uD83D\uDD0D";
		private static final int UNFOCUSED_COLOR = ARGB.opaque(0xBBBBBB);
		private long lastFocusChangeTime = 0;
		private final int maximizedX;
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

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
			if (!isMouseOver(event.x(), event.y())) {
				return false;
			}

			if (isEditable()) {
				if (event.button() == 0) {
					setFocused(true);
				} else if (event.button() == 1) {
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
				int currentWidth = isFocused() ? (int) (minWidth + (maximizedWidth - minWidth) * ratio) : (int) (maximizedWidth - (maximizedWidth - minWidth) * ratio);
				setPosition(new Position(maximizedX + maximizedWidth - currentWidth, y));
				updateDimensions(currentWidth, getHeight());
			}

			guiGraphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF777777);
		}

		@Override
		public void extractTooltip(Screen screen, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
			if (!isFocused() && isMouseOver(mouseX, mouseY)) {
				GuiHelper.extractTooltip(screen, guiGraphics, List.of(
						Component.translatable("gui.sophisticatedcore.text_box.search_box"),
						Component.translatable("gui.sophisticatedcore.text_box.search_box_detail").withStyle(ChatFormatting.GRAY)
				), mouseX, mouseY);
			}
		}
	}
}
