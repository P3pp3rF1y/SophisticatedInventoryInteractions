package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SlotRegionClassifier {
	private static final Comparator<Slot> TOP_RIGHT_SLOT_COMPARATOR = Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> -slot.x);

	private static final Comparator<Slot> SLOT_ORDER = Comparator.comparingInt((Slot slot) -> slot.y).thenComparingInt(slot -> slot.x)
			.thenComparingInt(slot -> slot.index);

	public SlotRegions classify(AbstractContainerMenu menu) {
		return classify(menu, null, Set.of());
	}

	public SlotRegions classify(AbstractContainerMenu menu, @Nullable Player player) {
		return classify(menu, player, Set.of());
	}

	public SlotRegions classify(AbstractContainerMenu menu, @Nullable Player player, Set<Integer> excludedSlotIds) {
		List<SlotWithId> rawContainerSlots = new ArrayList<>();
		List<SlotWithId> actionableContainerSlots = new ArrayList<>();
		List<SlotWithId> excludedContainerSlots = new ArrayList<>();
		List<SlotWithId> playerSlots = new ArrayList<>();
		List<SlotWithId> playerMainSlots = new ArrayList<>();
		boolean hasCraftingRegion = false;
		for (Slot slot : menu.slots) {
			int slotId = slot.index;
			SlotWithId slotWithId = new SlotWithId(slotId, slot);
			if (slot instanceof ResultSlot || slot.container instanceof CraftingContainer) {
				hasCraftingRegion = true;
			}
			if (slot.container instanceof Inventory) {
				playerSlots.add(slotWithId);
				int containerSlot = slot.getContainerSlot();
				if (containerSlot >= 9 && containerSlot <= 35) {
					playerMainSlots.add(slotWithId);
				}
			} else if (isUsableStorageSlot(slot, player)) {
				rawContainerSlots.add(slotWithId);
				if (excludedSlotIds.contains(slotId)) {
					excludedContainerSlots.add(slotWithId);
				} else {
					actionableContainerSlots.add(slotWithId);
				}
			}
		}

		rawContainerSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));
		actionableContainerSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));
		excludedContainerSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));
		playerSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));
		playerMainSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));

		return new SlotRegions(rawContainerSlots.stream().map(SlotWithId::slotId).toList(), actionableContainerSlots.stream().map(SlotWithId::slotId).toList(),
				excludedContainerSlots.stream().map(SlotWithId::slotId).toList(), playerSlots.stream().map(SlotWithId::slotId).toList(),
				playerMainSlots.stream().map(SlotWithId::slotId).toList(), hasCraftingRegion,
				actionableContainerSlots.stream().map(SlotWithId::slot).min(TOP_RIGHT_SLOT_COMPARATOR).orElse(null),
				playerSlots.stream().map(SlotWithId::slot).min(TOP_RIGHT_SLOT_COMPARATOR).orElse(null));
	}

	private boolean isUsableStorageSlot(Slot slot, @Nullable Player player) {
		int containerSlot = slot.getContainerSlot();
		return slot.isActive() && containerSlot >= 0 && containerSlot < slot.container.getContainerSize() && slot.getMaxStackSize() > 0
				&& (player == null || slot.mayPickup(player) && slot.mayPlace(slot.getItem()));
	}

	private record SlotWithId(int slotId, Slot slot) {
	}
}
