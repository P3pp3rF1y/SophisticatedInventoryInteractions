package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.ClientEventHandler;

import java.util.function.Supplier;

public record SyncSortMemoryPayload(int containerId, boolean hasSortBy, SortBy sortBy) {
	public static SyncSortMemoryPayload saved(int containerId, SortBy sortBy) {
		return new SyncSortMemoryPayload(containerId, true, sortBy);
	}

	public static SyncSortMemoryPayload notSaved(int containerId) {
		return new SyncSortMemoryPayload(containerId, false, SortBy.NAME);
	}

	public static void encode(SyncSortMemoryPayload payload, FriendlyByteBuf buffer) {
		buffer.writeInt(payload.containerId());
		buffer.writeBoolean(payload.hasSortBy());
		buffer.writeEnum(payload.sortBy());
	}

	public static SyncSortMemoryPayload decode(FriendlyByteBuf buffer) {
		return new SyncSortMemoryPayload(buffer.readInt(), buffer.readBoolean(), buffer.readEnum(SortBy.class));
	}

	public static void onMessage(SyncSortMemoryPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handlePayload(payload));
		context.setPacketHandled(true);
	}

	private static void handlePayload(SyncSortMemoryPayload payload) {
		ClientEventHandler.applySortMemory(payload.containerId(), payload.hasSortBy() ? payload.sortBy() : SortBy.NAME);
	}
}
