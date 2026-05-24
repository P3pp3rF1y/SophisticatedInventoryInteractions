package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InventoryInteractionSortMemory extends SavedData {
	private static final String SAVED_DATA_NAME = SophisticatedInventoryInteractions.MOD_ID + "_sort_memory";
	private static final String ENTRIES_TAG = "entries";
	private static final String KEY_TAG = "key";
	private static final String SORT_BY_TAG = "sortBy";

	private final Map<StorageSortKey, SortBy> sortByStorage;

	private InventoryInteractionSortMemory() {
		this(new HashMap<>());
	}

	private InventoryInteractionSortMemory(Map<StorageSortKey, SortBy> sortByStorage) {
		this.sortByStorage = sortByStorage;
	}

	public static InventoryInteractionSortMemory get(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		//noinspection ConstantConditions - overworld is loaded while server levels are available
		DimensionDataStorage storage = overworld.getDataStorage();
		return storage.computeIfAbsent(InventoryInteractionSortMemory::load, InventoryInteractionSortMemory::new, SAVED_DATA_NAME);
	}

	public Optional<SortBy> getSortBy(StorageSortKey storageKey) {
		return Optional.ofNullable(sortByStorage.get(storageKey));
	}

	public void setSortBy(StorageSortKey storageKey, SortBy sortBy) {
		if (sortBy == SortBy.NAME) {
			if (sortByStorage.remove(storageKey) != null) {
				setDirty();
			}
			return;
		}

		if (sortByStorage.put(storageKey, sortBy) != sortBy) {
			setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag entries = new ListTag();
		for (Map.Entry<StorageSortKey, SortBy> entry : sortByStorage.entrySet()) {
			if (entry.getValue() == SortBy.NAME) {
				continue;
			}
			CompoundTag entryTag = new CompoundTag();
			entryTag.put(KEY_TAG, entry.getKey().serialize());
			entryTag.putString(SORT_BY_TAG, entry.getValue().getSerializedName());
			entries.add(entryTag);
		}
		tag.put(ENTRIES_TAG, entries);
		return tag;
	}

	private static InventoryInteractionSortMemory load(CompoundTag tag) {
		Map<StorageSortKey, SortBy> sortByStorage = new HashMap<>();
		ListTag entries = tag.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
		for (int i = 0; i < entries.size(); i++) {
			CompoundTag entryTag = entries.getCompound(i);
			SortBy sortBy = SortBy.fromName(entryTag.getString(SORT_BY_TAG));
			if (sortBy == SortBy.NAME) {
				continue;
			}
			StorageSortKey.deserialize(entryTag.getCompound(KEY_TAG)).ifPresent(storageKey -> sortByStorage.put(storageKey, sortBy));
		}
		return new InventoryInteractionSortMemory(sortByStorage);
	}
}
