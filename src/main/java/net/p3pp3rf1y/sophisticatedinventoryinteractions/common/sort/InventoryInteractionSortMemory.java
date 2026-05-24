package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
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
	private static final SavedDataType<InventoryInteractionSortMemory> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(SophisticatedInventoryInteractions.MOD_ID, "sort_memory"), InventoryInteractionSortMemory::new, CompoundTag.CODEC.xmap(InventoryInteractionSortMemory::load, InventoryInteractionSortMemory::serialize));

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
		SavedDataStorage storage = overworld.getDataStorage();
		return storage.computeIfAbsent(TYPE);
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

	private CompoundTag serialize() {
		CompoundTag tag = new CompoundTag();
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
		ListTag entries = tag.getList(ENTRIES_TAG).orElse(new ListTag());
		for (int i = 0; i < entries.size(); i++) {
			Optional<CompoundTag> entryTag = entries.getCompound(i);
			if (entryTag.isEmpty()) {
				continue;
			}
			SortBy sortBy = entryTag.get().getString(SORT_BY_TAG).map(SortBy::fromName).orElse(SortBy.NAME);
			if (sortBy == SortBy.NAME) {
				continue;
			}
			entryTag.get().getCompound(KEY_TAG).flatMap(StorageSortKey::deserialize).ifPresent(storageKey -> sortByStorage.put(storageKey, sortBy));
		}
		return new InventoryInteractionSortMemory(sortByStorage);
	}
}
