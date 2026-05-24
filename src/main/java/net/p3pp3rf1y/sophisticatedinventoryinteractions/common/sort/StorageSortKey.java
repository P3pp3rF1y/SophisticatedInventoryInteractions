package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record StorageSortKey(Type type, ResourceLocation dimension, @Nullable BlockPos blockPos, @Nullable UUID entityId, @Nullable String compoundId) {
	private static final String TYPE_TAG = "type";
	private static final String DIMENSION_TAG = "dimension";
	private static final String BLOCK_POS_TAG = "blockPos";
	private static final String ENTITY_ID_TAG = "entityId";
	private static final String COMPOUND_ID_TAG = "compoundId";

	public static StorageSortKey block(ResourceLocation dimension, BlockPos blockPos) {
		return new StorageSortKey(Type.BLOCK, dimension, blockPos.immutable(), null, null);
	}

	public static StorageSortKey entity(ResourceLocation dimension, UUID entityId) {
		return new StorageSortKey(Type.ENTITY, dimension, null, entityId, null);
	}

	public static StorageSortKey compound(List<StorageSortKey> parts) {
		List<String> partIds = parts.stream().map(StorageSortKey::toStableId).sorted().toList();
		ResourceLocation dimension = parts.stream()
				.map(StorageSortKey::dimension)
				.min(Comparator.comparing(ResourceLocation::toString))
				.orElseThrow();
		return new StorageSortKey(Type.COMPOUND, dimension, null, null, String.join(";", partIds));
	}

	public CompoundTag serialize() {
		CompoundTag tag = new CompoundTag();
		tag.putString(TYPE_TAG, type.getSerializedName());
		tag.putString(DIMENSION_TAG, dimension.toString());
		if (type == Type.BLOCK && blockPos != null) {
			tag.putLong(BLOCK_POS_TAG, blockPos.asLong());
		} else if (type == Type.ENTITY && entityId != null) {
			tag.putUUID(ENTITY_ID_TAG, entityId);
		} else if (type == Type.COMPOUND && compoundId != null) {
			tag.putString(COMPOUND_ID_TAG, compoundId);
		}
		return tag;
	}

	public static Optional<StorageSortKey> deserialize(CompoundTag tag) {
		Type type = Type.fromName(tag.getString(TYPE_TAG));
		if (type == null || !tag.contains(DIMENSION_TAG)) {
			return Optional.empty();
		}

		ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION_TAG));
		if (dimension == null) {
			return Optional.empty();
		}

		return switch (type) {
			case BLOCK -> tag.contains(BLOCK_POS_TAG) ? Optional.of(block(dimension, BlockPos.of(tag.getLong(BLOCK_POS_TAG)))) : Optional.empty();
			case ENTITY -> tag.hasUUID(ENTITY_ID_TAG) ? Optional.of(entity(dimension, tag.getUUID(ENTITY_ID_TAG))) : Optional.empty();
			case COMPOUND -> tag.contains(COMPOUND_ID_TAG) ? Optional.of(new StorageSortKey(Type.COMPOUND, dimension, null, null, tag.getString(COMPOUND_ID_TAG))) : Optional.empty();
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
