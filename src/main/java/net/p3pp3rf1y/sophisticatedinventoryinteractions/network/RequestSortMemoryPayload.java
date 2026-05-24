package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestSortMemoryPayload() {
	public static void encode(RequestSortMemoryPayload payload, FriendlyByteBuf buffer) {
	}

	public static RequestSortMemoryPayload decode(FriendlyByteBuf buffer) {
		return new RequestSortMemoryPayload();
	}

	public static void onMessage(RequestSortMemoryPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		ServerPlayer serverPlayer = context.getSender();
		if (serverPlayer == null) {
			return;
		}
		context.enqueueWork(() -> SortMemoryPayloadHandler.handleRequest(serverPlayer));
		context.setPacketHandled(true);
	}
}
