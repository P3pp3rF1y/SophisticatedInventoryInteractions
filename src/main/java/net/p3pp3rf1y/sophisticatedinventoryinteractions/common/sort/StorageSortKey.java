package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record StorageSortKey(Type type, String dimension, @Nullable BlockPos blockPos, @Nullable UUID entityId, @Nullable String compoundId) {
	private static final String TYPE_TAG = "type";
	private static final String DIMENSION_TAG = "dimension";
	private static final String BLOCK_POS_TAG = "blockPos";
	private static final String ENTITY_ID_TAG = "entityId";
	private static final String COMPOUND_ID_TAG = "compoundId";

	public static StorageSortKey block(String dimension, BlockPos blockPos) {
		return new StorageSortKey(Type.BLOCK, dimension, blockPos.immutable(), null, null);
	}

	public static StorageSortKey entity(String dimension, UUID entityId) {
		return new StorageSortKey(Type.ENTITY, dimension, null, entityId, null);
	}

	public static StorageSortKey compound(List<StorageSortKey> parts) {
		List<String> partIds = parts.stream().map(StorageSortKey::toStableId).sorted().toList();
		String dimension = parts.stream()
				.map(StorageSortKey::dimension)
				.min(Comparator.naturalOrder())
				.orElseThrow();
		return new StorageSortKey(Type.COMPOUND, dimension, null, null, String.join(";", partIds));
	}

	public CompoundTag serialize() {
		CompoundTag tag = new CompoundTag();
		tag.putString(TYPE_TAG, type.getSerializedName());
		tag.putString(DIMENSION_TAG, dimension);
		if (type == Type.BLOCK && blockPos != null) {
			tag.putLong(BLOCK_POS_TAG, blockPos.asLong());
		} else if (type == Type.ENTITY && entityId != null) {
			tag.putString(ENTITY_ID_TAG, entityId.toString());
		} else if (type == Type.COMPOUND && compoundId != null) {
			tag.putString(COMPOUND_ID_TAG, compoundId);
		}
		return tag;
	}

	public static Optional<StorageSortKey> deserialize(CompoundTag tag) {
		Type type = tag.getString(TYPE_TAG).map(Type::fromName).orElse(null);
		if (type == null) {
			return Optional.empty();
		}

		String dimension = tag.getString(DIMENSION_TAG).orElse(null);
		if (dimension == null) {
			return Optional.empty();
		}

		return switch (type) {
			case BLOCK -> tag.getLong(BLOCK_POS_TAG).map(pos -> block(dimension, BlockPos.of(pos)));
			case ENTITY -> tag.getString(ENTITY_ID_TAG).flatMap(entityId -> {
				try {
					return Optional.of(entity(dimension, UUID.fromString(entityId)));
				} catch (IllegalArgumentException e) {
					return Optional.empty();
				}
			});
			case COMPOUND -> tag.getString(COMPOUND_ID_TAG).map(compoundId -> new StorageSortKey(Type.COMPOUND, dimension, null, null, compoundId));
		};
	}

	private String toStableId() {
		return switch (type) {
			case BLOCK -> type.getSerializedName() + ":" + dimension + ":" + (blockPos == null ? 0 : blockPos.asLong());
			case ENTITY -> type.getSerializedName() + ":" + dimension + ":" + entityId;
			case COMPOUND -> type.getSerializedName() + ":" + dimension + ":" + compoundId;
		};
	}

	public enum Type {
		BLOCK("block"),
		ENTITY("entity"),
		COMPOUND("compound");

		private final String serializedName;

		Type(String serializedName) {
			this.serializedName = serializedName;
		}

		private String getSerializedName() {
			return serializedName;
		}

		@Nullable
		private static Type fromName(String name) {
			for (Type type : values()) {
				if (type.serializedName.equals(name)) {
					return type;
				}
			}
			return null;
		}
	}
}
