package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;

public record SetSortMemoryPayload(SortBy sortBy) implements CustomPacketPayload {
	public static final Type<SetSortMemoryPayload> TYPE = new Type<>(SophisticatedInventoryInteractions.getRL("set_sort_memory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetSortMemoryPayload> STREAM_CODEC = StreamCodec
			.composite(NeoForgeStreamCodecs.enumCodec(SortBy.class), SetSortMemoryPayload::sortBy, SetSortMemoryPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(SetSortMemoryPayload payload, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer serverPlayer)) {
			return;
		}
		context.enqueueWork(() -> SortMemoryPayloadHandler.handleSet(serverPlayer, payload.sortBy()));
	}
}
