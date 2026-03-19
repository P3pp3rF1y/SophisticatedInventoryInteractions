package net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

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

	public Optional<AnchorLayout> getLayout(AbstractContainerScreen<?> screen, SlotRegions slotRegions) {
		if (slotRegions.containerAnchor() == null || slotRegions.playerAnchor() == null) {
			return Optional.empty();
		}

		Slot containerAnchor = slotRegions.containerAnchor();
		int containerAnchorAbsX = screen.getGuiLeft() + containerAnchor.x;
		int containerAnchorAbsY = screen.getGuiTop() + containerAnchor.y;
		int containerSortX = containerAnchorAbsX - 7;
		int rowY = containerAnchorAbsY - SMALL_BUTTON_SIZE - BUTTON_VERTICAL_MARGIN;
		int containerLeftAbsX = slotRegions.containerSlotIndexes().stream()
				.mapToInt(slotIndex -> screen.getGuiLeft() + screen.getMenu().getSlot(slotIndex).x)
				.min()
				.orElse(containerSortX - SEARCH_MIN_WIDTH);
		int searchX = containerLeftAbsX;
		int searchWidth = Math.max(SEARCH_MIN_WIDTH, containerSortX - SEARCH_TO_SORT_GAP - searchX);

		Slot playerAnchor = slotRegions.playerAnchor();
		int playerAnchorAbsX = screen.getGuiLeft() + playerAnchor.x;
		int playerAnchorAbsY = screen.getGuiTop() + playerAnchor.y;
		int playerSortX = playerAnchorAbsX + 5;
		int transferToContainerX = playerSortX - SMALL_BUTTON_SIZE - BUTTON_HORIZONTAL_GAP;
		int transferToPlayerX = transferToContainerX - SMALL_BUTTON_SIZE - BUTTON_HORIZONTAL_GAP;
		int playerRowY = playerAnchorAbsY - SMALL_BUTTON_SIZE - BUTTON_VERTICAL_MARGIN;

		Offset override = getOffsetOverride(screen.getClass().getName());
		searchX += override.x();
		rowY += override.y();
		containerSortX += override.x();
		playerSortX += override.x();
		transferToContainerX += override.x();
		transferToPlayerX += override.x();
		playerRowY += override.y();

		return Optional.of(new AnchorLayout(searchX, rowY + 1, searchWidth, SEARCH_HEIGHT, containerSortX, rowY, transferToPlayerX, playerRowY, transferToContainerX, playerRowY, playerSortX, playerRowY));
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

		Offset override = getOffsetOverride(screen.getClass().getName());
		playerSortX += override.x();
		playerSortY += override.y();

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
			int searchX,
			int searchY,
			int searchWidth,
			int searchHeight,
			int containerSortX,
			int containerSortY,
			int transferToPlayerX,
			int transferToPlayerY,
			int transferToContainerX,
			int transferToContainerY,
			int playerSortX,
			int playerSortY
	) {
	}

	public record PlayerSortLayout(int playerSortX, int playerSortY) {
	}
}
