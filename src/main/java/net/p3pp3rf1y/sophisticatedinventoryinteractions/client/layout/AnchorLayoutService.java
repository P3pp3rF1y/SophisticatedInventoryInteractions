package net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AnchorLayoutService {
	private static final int SMALL_BUTTON_SIZE = 12;
	private static final int BUTTON_VERTICAL_MARGIN = 2;
	private static final int BUTTON_HORIZONTAL_GAP = 0;
	private static final int SEARCH_TO_SORT_GAP = 1;
	private static final int SEARCH_MIN_WIDTH = 10;
	private static final int SEARCH_HEIGHT = 10;

	public Optional<AnchorLayout> getLayout(AbstractContainerScreen<?> screen, SlotRegions slotRegions, ContainerControls containerControls) {
		if (slotRegions.containerAnchor() == null || slotRegions.playerAnchor() == null) {
			return Optional.empty();
		}

		Slot containerAnchor = slotRegions.containerAnchor();
		int containerAnchorAbsX = screen.getGuiLeft() + containerAnchor.x;
		int containerAnchorAbsY = screen.getGuiTop() + containerAnchor.y;
		int rowY = containerAnchorAbsY - SMALL_BUTTON_SIZE - BUTTON_VERTICAL_MARGIN;
		int rightmostContainerButtonX = containerAnchorAbsX + 5;
		@Nullable ButtonLayout sortByLayout = containerControls.showSortBy() ? new ButtonLayout(rightmostContainerButtonX, rowY) : null;
		int sortX = containerControls.showSortBy() ? rightmostContainerButtonX - SMALL_BUTTON_SIZE - BUTTON_HORIZONTAL_GAP : rightmostContainerButtonX;
		ButtonLayout sortLayout = new ButtonLayout(sortX, rowY);
		@Nullable SearchLayout searchLayout = null;
		if (containerControls.showSearch()) {
			int containerLeftAbsX = slotRegions.actionableContainerSlotIndexes().stream()
					.mapToInt(slotIndex -> screen.getGuiLeft() + screen.getMenu().getSlot(slotIndex).x)
					.min()
					.orElse(sortLayout.x() - SEARCH_MIN_WIDTH);
			int searchWidth = Math.max(SEARCH_MIN_WIDTH, sortLayout.x() - SEARCH_TO_SORT_GAP - containerLeftAbsX);
			searchLayout = new SearchLayout(containerLeftAbsX, rowY + 1, searchWidth, SEARCH_HEIGHT);
		}

		Slot playerAnchor = slotRegions.playerAnchor();
		int playerAnchorAbsX = screen.getGuiLeft() + playerAnchor.x;
		int playerAnchorAbsY = screen.getGuiTop() + playerAnchor.y;
		int playerSortX = playerAnchorAbsX + 5;
		int transferToContainerX = playerSortX - SMALL_BUTTON_SIZE - BUTTON_HORIZONTAL_GAP;
		int transferToPlayerX = transferToContainerX - SMALL_BUTTON_SIZE - BUTTON_HORIZONTAL_GAP;
		int playerRowY = playerAnchorAbsY - SMALL_BUTTON_SIZE - BUTTON_VERTICAL_MARGIN;

		Offset override = getOffsetOverride(screen.getClass().getName());
		if (searchLayout != null) {
			searchLayout = searchLayout.offset(override.x(), override.y());
		}
		sortLayout = sortLayout.offset(override.x(), override.y());
		if (sortByLayout != null) {
			sortByLayout = sortByLayout.offset(override.x(), override.y());
		}

		return Optional.of(new AnchorLayout(searchLayout, sortLayout, sortByLayout, transferToPlayerX, playerRowY, transferToContainerX, playerRowY, playerSortX, playerRowY));
	}

	public Optional<PlayerSortLayout> getPlayerOnlySortLayout(AbstractContainerScreen<?> screen, SlotRegions slotRegions) {
		Slot playerMainTopRight = slotRegions.playerMainSlotIndexes().stream()
				.map(slotIndex -> screen.getMenu().getSlot(slotIndex))
				.min(Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> -slot.x))
				.orElse(null);
		if (playerMainTopRight == null) {
			return Optional.empty();
		}

		int playerSortX = screen.getGuiLeft() + playerMainTopRight.x + 5;
		int playerSortY = screen.getGuiTop() + playerMainTopRight.y - SMALL_BUTTON_SIZE - BUTTON_VERTICAL_MARGIN;

		return Optional.of(new PlayerSortLayout(playerSortX, playerSortY));
	}

	private Offset getOffsetOverride(String screenClassName) {
		List<? extends String> overrides = Config.COMMON.anchorOffsetOverrides.get();
		for (String overrideEntry : overrides) {
			String[] classAndOffset = overrideEntry.split("=");
			if (classAndOffset.length != 2 || !screenClassName.equals(classAndOffset[0])) {
				continue;
			}
			String[] offsets = classAndOffset[1].split(",");
			if (offsets.length != 2) {
				continue;
			}
			try {
				return new Offset(Integer.parseInt(offsets[0].trim()), Integer.parseInt(offsets[1].trim()));
			} catch (NumberFormatException ignored) {
				return Offset.ZERO;
			}
		}

		return Offset.ZERO;
	}

	private record Offset(int x, int y) {
		private static final Offset ZERO = new Offset(0, 0);
	}

	public record AnchorLayout(
			@Nullable SearchLayout searchLayout,
			ButtonLayout containerSortLayout,
			@Nullable ButtonLayout sortByLayout,
			int transferToPlayerX,
			int transferToPlayerY,
			int transferToContainerX,
			int transferToContainerY,
			int playerSortX,
			int playerSortY
	) {
	}

	public record ContainerControls(boolean showSearch, boolean showSortBy) {
	}

	public record SearchLayout(int x, int y, int width, int height) {
		private SearchLayout offset(int xOffset, int yOffset) {
			return new SearchLayout(x + xOffset, y + yOffset, width, height);
		}
	}

	public record ButtonLayout(int x, int y) {
		private ButtonLayout offset(int xOffset, int yOffset) {
			return new ButtonLayout(x + xOffset, y + yOffset);
		}
	}

	public record PlayerSortLayout(int playerSortX, int playerSortY) {
	}
}
