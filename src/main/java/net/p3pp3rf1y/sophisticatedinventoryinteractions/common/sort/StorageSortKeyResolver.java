package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

import java.util.List;
import java.util.Optional;

public class StorageSortKeyResolver {
	public Optional<StorageSortKey> resolve(AbstractContainerMenu menu, SlotRegions regions) {
		StorageSortKey resolvedKey = null;
		for (int slotIndex : regions.actionableContainerSlotIndexes()) {
			Slot slot = menu.getSlot(slotIndex);
			Optional<StorageSortKey> slotKey = resolve(slot.container);
			if (slotKey.isEmpty()) {
				return Optional.empty();
			}

			if (resolvedKey == null) {
				resolvedKey = slotKey.get();
			} else if (!resolvedKey.equals(slotKey.get())) {
				return Optional.empty();
			}
		}
		return Optional.ofNullable(resolvedKey);
	}

	private Optional<StorageSortKey> resolve(Container container) {
		if (container instanceof CompoundContainer compoundContainer) {
			return resolveCompound(compoundContainer);
		}

		if (container instanceof BlockEntity blockEntity && blockEntity.getLevel() != null) {
			ResourceLocation dimension = blockEntity.getLevel().dimension().location();
			return Optional.of(StorageSortKey.block(dimension, blockEntity.getBlockPos()));
		}

		if (container instanceof Entity entity) {
			ResourceLocation dimension = entity.level().dimension().location();
			return Optional.of(StorageSortKey.entity(dimension, entity.getUUID()));
		}
		return Optional.empty();
	}

	private Optional<StorageSortKey> resolveCompound(CompoundContainer compoundContainer) {
		Optional<StorageSortKey> key1 = resolve(compoundContainer.container1);
		Optional<StorageSortKey> key2 = resolve(compoundContainer.container2);
		if (key1.isEmpty() || key2.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(StorageSortKey.compound(List.of(key1.get(), key2.get())));
	}
}
