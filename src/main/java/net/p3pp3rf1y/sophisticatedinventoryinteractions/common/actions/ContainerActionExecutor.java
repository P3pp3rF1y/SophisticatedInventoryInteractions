package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

import java.util.*;
import java.util.stream.Collectors;

public class ContainerActionExecutor {
	public boolean execute(ServerPlayer player, AbstractContainerMenu menu, SlotRegions regions, InteractionActionType action, boolean filterByContents, SortBy sortBy) {
		boolean changed = switch (action) {
			case SORT_CONTAINER -> sortRegion(player, menu, regions.containerSlotIndexes(), sortBy);
			case SORT_PLAYER -> sortRegion(player, menu, regions.playerMainSlotIndexes(), SortBy.NAME);
			case TRANSFER_TO_CONTAINER -> transferBetweenRegions(player, menu, regions.playerMainSlotIndexes(), regions.containerSlotIndexes(), filterByContents);
			case TRANSFER_TO_PLAYER -> transferBetweenRegions(player, menu, regions.containerSlotIndexes(), regions.playerSlotIndexes(), filterByContents);
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

		List<Integer> sortableSlotIndexes = slotIndexes.stream()
				.filter(slotIndex -> !fixedSlotIndexes.contains(slotIndex))
				.toList();
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

		mergeStacks(stacks);

		stacks.sort(getComparator(sortBy));

		for (ItemStack stack : stacks) {
			if (!insertIntoSlots(menu, sortableSlotIndexes, stack, changedSlotIndexes)) {
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

	private boolean transferBetweenRegions(ServerPlayer player, AbstractContainerMenu menu, List<Integer> sourceSlots, List<Integer> targetSlots, boolean filterByContents) {
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
			if (target.isEmpty() || !targetSlot.mayPlace(source) || !ItemStack.isSameItemSameComponents(source, target)) {
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

	private void mergeStacks(List<ItemStack> stacks) {
		Map<ItemStackKey, ItemStack> stackTemplates = new LinkedHashMap<>();
		Map<ItemStackKey, Integer> countsByType = new LinkedHashMap<>();

		for (ItemStack stack : stacks) {
			ItemStackKey key = toItemStackKey(stack);
			stackTemplates.putIfAbsent(key, stack.copy());
			countsByType.merge(key, stack.getCount(), Integer::sum);
		}

		stacks.clear();
		for (Map.Entry<ItemStackKey, Integer> entry : countsByType.entrySet()) {
			ItemStack template = stackTemplates.get(entry.getKey());
			int remaining = entry.getValue();
			int maxStackSize = template.getMaxStackSize();
			while (remaining > 0) {
				ItemStack mergedStack = template.copy();
				int count = Math.min(remaining, maxStackSize);
				mergedStack.setCount(count);
				stacks.add(mergedStack);
				remaining -= count;
			}
		}
	}

	private boolean insertIntoSlots(AbstractContainerMenu menu, List<Integer> targetSlots, ItemStack stack, Set<Integer> changedSlotIndexes) {
		for (int targetIndex : targetSlots) {
			if (stack.isEmpty()) {
				return true;
			}
			Slot slot = menu.getSlot(targetIndex);
			ItemStack target = slot.getItem();
			if (!target.isEmpty() || !slot.mayPlace(stack)) {
				continue;
			}
			int toMove = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize()));
			if (toMove <= 0) {
				continue;
			}
			ItemStack moved = stack.copy();
			moved.setCount(toMove);
			slot.set(moved);
			changedSlotIndexes.add(targetIndex);
			stack.shrink(toMove);
		}

		for (int targetIndex : targetSlots) {
			if (stack.isEmpty()) {
				return true;
			}
			Slot slot = menu.getSlot(targetIndex);
			ItemStack target = slot.getItem();
			if (target.isEmpty() || !slot.mayPlace(stack) || !ItemStack.isSameItemSameComponents(target, stack)) {
				continue;
			}
			int maxCount = Math.min(slot.getMaxStackSize(target), target.getMaxStackSize());
			if (target.getCount() >= maxCount) {
				continue;
			}

			int toMove = Math.min(stack.getCount(), maxCount - target.getCount());
			target.grow(toMove);
			changedSlotIndexes.add(targetIndex);
			stack.shrink(toMove);
		}

		return stack.isEmpty();
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

	private Comparator<ItemStack> getComparator(SortBy sortBy) {
		return switch (sortBy) {
			case MOD -> Comparator
					.comparing((ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
					.thenComparing(stack -> stack.getHoverName().getString().toLowerCase())
					.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			case COUNT -> Comparator
					.comparingInt(ItemStack::getCount)
					.reversed()
					.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			case TAGS -> Comparator
					.comparingInt((ItemStack stack) -> -stack.getTags().collect(Collectors.toSet()).size())
					.thenComparing(stack -> stack.getTags().map(tag -> tag.location().toString()).sorted().collect(Collectors.joining("|")))
					.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			case NAME -> Comparator
					.comparing((ItemStack stack) -> stack.getHoverName().getString().toLowerCase())
					.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
					.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		};
	}

}
