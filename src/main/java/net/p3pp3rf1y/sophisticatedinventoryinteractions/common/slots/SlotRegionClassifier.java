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

	public SlotRegions classify(AbstractContainerMenu menu) {
		List<SlotWithId> containerSlots = new ArrayList<>();
		List<SlotWithId> playerSlots = new ArrayList<>();
		List<SlotWithId> playerMainSlots = new ArrayList<>();

		for (int slotId = 0; slotId < menu.slots.size(); slotId++) {
			Slot slot = menu.slots.get(slotId);
			SlotWithId slotWithId = new SlotWithId(slotId, slot);
			if (slot.container instanceof Inventory) {
				playerSlots.add(slotWithId);
				int containerSlot = slot.getContainerSlot();
				if (containerSlot >= 9 && containerSlot <= 35) {
					playerMainSlots.add(slotWithId);
				}
			} else {
				containerSlots.add(slotWithId);
			}
		}

		containerSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));
		playerSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));
		playerMainSlots.sort(Comparator.comparing(SlotWithId::slot, SLOT_ORDER));

		return new SlotRegions(
				containerSlots.stream().map(SlotWithId::slotId).toList(),
				playerSlots.stream().map(SlotWithId::slotId).toList(),
				playerMainSlots.stream().map(SlotWithId::slotId).toList(),
				containerSlots.stream().map(SlotWithId::slot).min(TOP_RIGHT_SLOT_COMPARATOR).orElse(null),
				playerSlots.stream().map(SlotWithId::slot).min(TOP_RIGHT_SLOT_COMPARATOR).orElse(null)
		);
	}

	private record SlotWithId(int slotId, Slot slot) {
	}
}
