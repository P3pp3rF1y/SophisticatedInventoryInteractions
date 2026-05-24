package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;

public record RequestSortMemoryPayload() implements CustomPacketPayload {
	public static final Type<RequestSortMemoryPayload> TYPE = new Type<>(SophisticatedInventoryInteractions.getRL("request_sort_memory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestSortMemoryPayload> STREAM_CODEC = StreamCodec.unit(new RequestSortMemoryPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestSortMemoryPayload payload, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer serverPlayer)) {
			return;
		}
		context.enqueueWork(() -> SortMemoryPayloadHandler.handleRequest(serverPlayer));
	}
}
