package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ActionValidationService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ContainerActionExecutor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;

import java.util.List;
import java.util.Set;

public record ContainerInteractionPayload(InteractionActionType actionType, boolean filterByContents, SortBy sortBy,
		List<Integer> excludedContainerSlotIndexes) implements CustomPacketPayload {
	private static final int MAX_EXCLUDED_CONTAINER_SLOTS = 512;
	private static final ActionValidationService VALIDATION_SERVICE = new ActionValidationService();
	private static final ContainerActionExecutor ACTION_EXECUTOR = new ContainerActionExecutor();

	public static final Type<ContainerInteractionPayload> TYPE = new Type<>(SophisticatedInventoryInteractions.getRL("container_interaction"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ContainerInteractionPayload> STREAM_CODEC = StreamCodec.composite(
			NeoForgeStreamCodecs.enumCodec(InteractionActionType.class), ContainerInteractionPayload::actionType, ByteBufCodecs.BOOL,
			ContainerInteractionPayload::filterByContents, NeoForgeStreamCodecs.enumCodec(SortBy.class), ContainerInteractionPayload::sortBy,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list(MAX_EXCLUDED_CONTAINER_SLOTS)), ContainerInteractionPayload::excludedContainerSlotIndexes,
			ContainerInteractionPayload::new);

	public ContainerInteractionPayload(InteractionActionType actionType, boolean filterByContents, SortBy sortBy) {
		this(actionType, filterByContents, sortBy, List.of());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(ContainerInteractionPayload payload, IPayloadContext context) {
		if (!(context.player() instanceof ServerPlayer serverPlayer)) {
			return;
		}
		context.enqueueWork(() -> {
			ActionValidationService.ValidationResult validationResult = VALIDATION_SERVICE.validate(serverPlayer, payload.actionType(),
					Set.copyOf(payload.excludedContainerSlotIndexes()));
			if (!validationResult.valid()) {
				return;
			}
			ACTION_EXECUTOR.execute(serverPlayer, validationResult.menu(), validationResult.regions(), payload.actionType(), payload.filterByContents(),
					payload.sortBy());
		});
	}
}
