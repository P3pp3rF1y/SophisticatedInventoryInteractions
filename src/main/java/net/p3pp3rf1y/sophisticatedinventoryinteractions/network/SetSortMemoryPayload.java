package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;

import java.util.function.Supplier;

public record SetSortMemoryPayload(SortBy sortBy) {
	public static void encode(SetSortMemoryPayload payload, FriendlyByteBuf buffer) {
		buffer.writeEnum(payload.sortBy());
	}

	public static SetSortMemoryPayload decode(FriendlyByteBuf buffer) {
		return new SetSortMemoryPayload(buffer.readEnum(SortBy.class));
	}

	public static void onMessage(SetSortMemoryPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		ServerPlayer serverPlayer = context.getSender();
		if (serverPlayer == null) {
			return;
		}
		context.enqueueWork(() -> SortMemoryPayloadHandler.handleSet(serverPlayer, payload.sortBy()));
		context.setPacketHandled(true);
	}
}
