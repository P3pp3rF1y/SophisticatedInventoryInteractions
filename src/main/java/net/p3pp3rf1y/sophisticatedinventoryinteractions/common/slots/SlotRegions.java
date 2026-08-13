package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots;

import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

import java.util.List;

public record SlotRegions(List<Integer> rawContainerSlotIndexes, List<Integer> actionableContainerSlotIndexes, List<Integer> excludedContainerSlotIndexes,
		List<Integer> playerSlotIndexes, List<Integer> playerMainSlotIndexes, @Nullable Slot containerAnchor, @Nullable Slot playerAnchor) {
	public boolean hasContainerRegion() {
		return !actionableContainerSlotIndexes.isEmpty();
	}

	public boolean hasPlayerRegion() {
		return !playerSlotIndexes.isEmpty();
	}

	public boolean hasPlayerMainRegion() {
		return !playerMainSlotIndexes.isEmpty();
	}

	public int actionableContainerSlotCount() {
		return actionableContainerSlotIndexes.size();
	}
}
