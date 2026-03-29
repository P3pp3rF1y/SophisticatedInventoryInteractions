package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SlotRegionClassifier {
	private static final Comparator<Slot> TOP_RIGHT_SLOT_COMPARATOR = Comparator
			.comparingInt((Slot slot) -> slot.y)
			.thenComparingInt(slot -> -slot.x);

	private static final Comparator<Slot> SLOT_ORDER = Comparator
			.comparingInt((Slot slot) -> slot.y)
			.thenComparingInt(slot -> slot.x)
			.thenComparingInt(slot -> slot.index);

	private final MenuSlotExclusionResolver menuSlotExclusionResolver = new MenuSlotExclusionResolver();

	public SlotRegions classify(AbstractContainerMenu menu) {
		List<SlotWithId> rawContainerSlots = new ArrayList<>();
		List<SlotWithId> actionableContainerSlots = new ArrayList<>();
		List<SlotWithId> excludedContainerSlots = new ArrayList<>();
		List<SlotWithId> playerSlots = new ArrayList<>();
		List<SlotWithId> playerMainSlots = new ArrayList<>();
		var excludedSlotIds = menuSlotExclusionResolver.getExcludedSlotIds(menu.getClass().getName());

		for (Slot slot : menu.slots) {
			int slotId = slot.index;
			SlotWithId slotWithId = new SlotWithId(slotId, slot);
			if (slot.container instanceof Inventory) {
				playerSlots.add(slotWithId);
				int containerSlot = slot.getContainerSlot();
				if (containerSlot >= 9 && containerSlot <= 35) {
					playerMainSlots.add(slotWithId);
				}
			} else {
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

		return new SlotRegions(
				rawContainerSlots.stream().map(SlotWithId::slotId).toList(),
				actionableContainerSlots.stream().map(SlotWithId::slotId).toList(),
				excludedContainerSlots.stream().map(SlotWithId::slotId).toList(),
				playerSlots.stream().map(SlotWithId::slotId).toList(),
				playerMainSlots.stream().map(SlotWithId::slotId).toList(),
				actionableContainerSlots.stream().map(SlotWithId::slot).min(TOP_RIGHT_SLOT_COMPARATOR).orElse(null),
				playerSlots.stream().map(SlotWithId::slot).min(TOP_RIGHT_SLOT_COMPARATOR).orElse(null)
		);
	}

	private record SlotWithId(int slotId, Slot slot) {
	}
}
