package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

import java.util.*;

public class ContainerActionExecutor {
	public boolean execute(ServerPlayer player, AbstractContainerMenu menu, SlotRegions regions, InteractionActionType action, boolean filterByContents,
			SortBy sortBy) {
		boolean changed = switch (action) {
			case SORT_CONTAINER -> sortRegion(player, menu, regions.actionableContainerSlotIndexes(), sortBy);
			case SORT_PLAYER -> sortRegion(player, menu, regions.playerMainSlotIndexes(), SortBy.NAME);
			case TRANSFER_TO_CONTAINER ->
				transferBetweenRegions(player, menu, regions.playerMainSlotIndexes(), regions.actionableContainerSlotIndexes(), filterByContents);
			case TRANSFER_TO_PLAYER ->
				transferBetweenRegions(player, menu, regions.actionableContainerSlotIndexes(), regions.playerSlotIndexes(), filterByContents);
		};

		if (changed) {
			menu.broadcastChanges();
		}
		return changed;
	}

	private boolean sortRegion(ServerPlayer player, AbstractContainerMenu menu, List<Integer> slotIndexes, SortBy sortBy) {
		if (slotIndexes.isEmpty()) {
			return false;
		}

		Map<Integer, ItemStack> snapshot = new HashMap<>();
		Set<Integer> fixedSlotIndexes = new HashSet<>();
		for (int slotIndex : slotIndexes) {
			Slot slot = menu.getSlot(slotIndex);
			snapshot.put(slotIndex, slot.getItem().copy());
			if (isFixedSortSlot(player, slot)) {
				fixedSlotIndexes.add(slotIndex);
			}
		}

		List<Integer> sortableSlotIndexes = slotIndexes.stream().filter(slotIndex -> !fixedSlotIndexes.contains(slotIndex)).toList();
		if (sortableSlotIndexes.isEmpty()) {
			return false;
		}

		List<ItemStack> stacks = new ArrayList<>();
		Set<Integer> changedSlotIndexes = new HashSet<>();
		for (int slotIndex : sortableSlotIndexes) {
			Slot slot = menu.getSlot(slotIndex);
			if (!slot.getItem().isEmpty() && slot.mayPickup(player)) {
				stacks.add(slot.getItem().copy());
				slot.set(ItemStack.EMPTY);
				changedSlotIndexes.add(slotIndex);
			}
		}

		if (stacks.isEmpty()) {
			return false;
		}

		List<Map.Entry<ItemStackKey, Integer>> sortedEntries = mergeAndSortStacks(stacks, sortBy);

		for (Map.Entry<ItemStackKey, Integer> entry : sortedEntries) {
			if (!insertIntoSlots(menu, sortableSlotIndexes, entry, changedSlotIndexes)) {
				restoreSnapshot(menu, snapshot, changedSlotIndexes);
				markSlotsChanged(menu, changedSlotIndexes);
				return false;
			}
		}
		markSlotsChanged(menu, changedSlotIndexes);

		return true;
	}

	private boolean isFixedSortSlot(ServerPlayer player, Slot slot) {
		if (!slot.hasItem()) {
			return false;
		}

		ItemStack stack = slot.getItem().copy();
		return !slot.mayPickup(player) || !slot.mayPlace(stack);
	}

	private boolean transferBetweenRegions(ServerPlayer player, AbstractContainerMenu menu, List<Integer> sourceSlots, List<Integer> targetSlots,
			boolean filterByContents) {
		boolean changed = false;
		Set<ItemStackKey> targetStacks = filterByContents ? getUniqueStacks(menu, targetSlots) : Set.of();
		if (filterByContents && targetStacks.isEmpty()) {
			return false;
		}

		for (int sourceIndex : sourceSlots) {
			Slot sourceSlot = menu.getSlot(sourceIndex);
			ItemStack source = sourceSlot.getItem();
			if (source.isEmpty() || !sourceSlot.mayPickup(player) || (filterByContents && !targetStacks.contains(toItemStackKey(source)))) {
				continue;
			}

			if (moveStackToTargets(sourceSlot, source, menu, targetSlots)) {
				changed = true;
			}
		}
		return changed;
	}

	private Set<ItemStackKey> getUniqueStacks(AbstractContainerMenu menu, List<Integer> slotIndexes) {
		Set<ItemStackKey> stacks = new HashSet<>();
		for (int slotIndex : slotIndexes) {
			ItemStack stack = menu.getSlot(slotIndex).getItem();
			if (!stack.isEmpty()) {
				stacks.add(toItemStackKey(stack));
			}
		}
		return stacks;
	}

	private ItemStackKey toItemStackKey(ItemStack stack) {
		return ItemStackKey.of(stack);
	}

	private boolean moveStackToTargets(Slot sourceSlot, ItemStack source, AbstractContainerMenu menu, List<Integer> targetSlots) {
		int originalCount = source.getCount();

		for (int targetIndex : targetSlots) {
			if (source.isEmpty()) {
				break;
			}
			Slot targetSlot = menu.getSlot(targetIndex);
			ItemStack target = targetSlot.getItem();
			if (target.isEmpty() || !targetSlot.mayPlace(source) || !ItemStack.isSameItemSameTags(source, target)) {
				continue;
			}
			int maxCount = Math.min(targetSlot.getMaxStackSize(target), target.getMaxStackSize());
			if (target.getCount() >= maxCount) {
				continue;
			}

			int moved = Math.min(source.getCount(), maxCount - target.getCount());
			if (moved <= 0) {
				continue;
			}
			target.grow(moved);
			source.shrink(moved);
			targetSlot.setChanged();
		}

		for (int targetIndex : targetSlots) {
			if (source.isEmpty()) {
				break;
			}
			Slot targetSlot = menu.getSlot(targetIndex);
			if (targetSlot.hasItem() || !targetSlot.mayPlace(source)) {
				continue;
			}

			int moved = Math.min(source.getCount(), Math.min(targetSlot.getMaxStackSize(source), source.getMaxStackSize()));
			if (moved <= 0) {
				continue;
			}

			ItemStack movedStack = source.copy();
			movedStack.setCount(moved);
			targetSlot.set(movedStack);
			targetSlot.setChanged();
			source.shrink(moved);
		}

		if (source.isEmpty()) {
			sourceSlot.set(ItemStack.EMPTY);
		} else {
			sourceSlot.setChanged();
		}

		return source.getCount() != originalCount;
	}

	List<Map.Entry<ItemStackKey, Integer>> mergeAndSortStacks(List<ItemStack> stacks, SortBy sortBy) {
		Map<ItemStackKey, Integer> countsByType = new LinkedHashMap<>();

		for (ItemStack stack : stacks) {
			countsByType.merge(toItemStackKey(stack), stack.getCount(), Integer::sum);
		}

		List<Map.Entry<ItemStackKey, Integer>> entries = new ArrayList<>(countsByType.entrySet());
		entries.sort(getComparator(sortBy));
		return entries;
	}

	private boolean insertIntoSlots(AbstractContainerMenu menu, List<Integer> targetSlots, Map.Entry<ItemStackKey, Integer> entry,
			Set<Integer> changedSlotIndexes) {
		int remaining = entry.getValue();
		ItemStack template = entry.getKey().getStack();

		for (int targetIndex : targetSlots) {
			if (remaining <= 0) {
				return true;
			}

			Slot slot = menu.getSlot(targetIndex);
			if (slot.hasItem() || !slot.mayPlace(template)) {
				continue;
			}

			int toMove = Math.min(remaining, Math.min(slot.getMaxStackSize(template), template.getMaxStackSize()));
			if (toMove <= 0) {
				continue;
			}

			ItemStack moved = template.copy();
			moved.setCount(toMove);
			slot.set(moved);
			changedSlotIndexes.add(targetIndex);
			remaining -= toMove;
		}

		return remaining <= 0;
	}

	private void restoreSnapshot(AbstractContainerMenu menu, Map<Integer, ItemStack> snapshot, Set<Integer> changedSlotIndexes) {
		for (Map.Entry<Integer, ItemStack> entry : snapshot.entrySet()) {
			int slotIndex = entry.getKey();
			menu.getSlot(slotIndex).set(entry.getValue());
			changedSlotIndexes.add(slotIndex);
		}
	}

	private void markSlotsChanged(AbstractContainerMenu menu, Set<Integer> changedSlotIndexes) {
		for (int slotIndex : changedSlotIndexes) {
			menu.getSlot(slotIndex).setChanged();
		}
	}

	private Comparator<Map.Entry<ItemStackKey, Integer>> getComparator(SortBy sortBy) {
		return switch (sortBy) {
			case MOD -> InventorySorter.BY_MOD;
			case COUNT -> InventorySorter.BY_COUNT;
			case TAGS -> InventorySorter.BY_TAGS;
			case NAME -> InventorySorter.BY_NAME;
		};
	}

}
