package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ActionValidationService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ContainerActionExecutor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record ContainerInteractionPayload(InteractionActionType actionType, boolean filterByContents, SortBy sortBy,
		List<Integer> excludedContainerSlotIndexes) {
	private static final int MAX_EXCLUDED_CONTAINER_SLOTS = 512;
	private static final ActionValidationService VALIDATION_SERVICE = new ActionValidationService();
	private static final ContainerActionExecutor ACTION_EXECUTOR = new ContainerActionExecutor();

	public ContainerInteractionPayload {
		if (excludedContainerSlotIndexes.size() > MAX_EXCLUDED_CONTAINER_SLOTS) {
			throw new IllegalArgumentException("Too many excluded container slots");
		}
		excludedContainerSlotIndexes = List.copyOf(excludedContainerSlotIndexes);
	}

	public ContainerInteractionPayload(InteractionActionType actionType, boolean filterByContents, SortBy sortBy) {
		this(actionType, filterByContents, sortBy, List.of());
	}

	public static void encode(ContainerInteractionPayload payload, FriendlyByteBuf buffer) {
		buffer.writeEnum(payload.actionType());
		buffer.writeBoolean(payload.filterByContents());
		buffer.writeEnum(payload.sortBy());
		buffer.writeVarInt(payload.excludedContainerSlotIndexes().size());
		for (int index = 0; index < payload.excludedContainerSlotIndexes().size(); index++) {
			buffer.writeVarInt(payload.excludedContainerSlotIndexes().get(index));
		}
	}

	public static ContainerInteractionPayload decode(FriendlyByteBuf buffer) {
		InteractionActionType actionType = buffer.readEnum(InteractionActionType.class);
		boolean filterByContents = buffer.readBoolean();
		SortBy sortBy = buffer.readEnum(SortBy.class);
		int excludedCount = buffer.readVarInt();
		if (excludedCount < 0 || excludedCount > MAX_EXCLUDED_CONTAINER_SLOTS) {
			throw new IllegalArgumentException("Too many excluded container slots");
		}
		List<Integer> excludedContainerSlotIndexes = new ArrayList<>(excludedCount);
		for (int index = 0; index < excludedCount; index++) {
			excludedContainerSlotIndexes.add(buffer.readVarInt());
		}
		return new ContainerInteractionPayload(actionType, filterByContents, sortBy, List.copyOf(excludedContainerSlotIndexes));
	}

	public static void onMessage(ContainerInteractionPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(payload, context));
		context.setPacketHandled(true);
	}

	private static void handleMessage(ContainerInteractionPayload payload, NetworkEvent.Context context) {
		ServerPlayer serverPlayer = context.getSender();
		if (serverPlayer == null) {
			return;
		}
		ActionValidationService.ValidationResult validationResult = VALIDATION_SERVICE.validate(serverPlayer, payload.actionType(),
				Set.copyOf(payload.excludedContainerSlotIndexes()));
		if (!validationResult.valid()) {
			return;
		}
		ACTION_EXECUTOR.execute(serverPlayer, validationResult.menu(), validationResult.regions(), payload.actionType(), payload.filterByContents(),
				payload.sortBy());
	}
}
