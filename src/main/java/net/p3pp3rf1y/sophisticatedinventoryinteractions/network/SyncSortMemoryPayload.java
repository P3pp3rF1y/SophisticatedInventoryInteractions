package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.ClientEventHandler;

public record SyncSortMemoryPayload(int containerId, boolean hasSortBy, SortBy sortBy) implements CustomPacketPayload {
	public static final Type<SyncSortMemoryPayload> TYPE = new Type<>(SophisticatedInventoryInteractions.getRL("sync_sort_memory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncSortMemoryPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT,
			SyncSortMemoryPayload::containerId, ByteBufCodecs.BOOL, SyncSortMemoryPayload::hasSortBy, NeoForgeStreamCodecs.enumCodec(SortBy.class),
			SyncSortMemoryPayload::sortBy, SyncSortMemoryPayload::new);

	public static SyncSortMemoryPayload saved(int containerId, SortBy sortBy) {
		return new SyncSortMemoryPayload(containerId, true, sortBy);
	}

	public static SyncSortMemoryPayload notSaved(int containerId) {
		return new SyncSortMemoryPayload(containerId, false, SortBy.NAME);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SyncSortMemoryPayload payload, IPayloadContext context) {
		ClientEventHandler.applySortMemory(payload.containerId(), payload.hasSortBy() ? payload.sortBy() : SortBy.NAME);
	}
}
