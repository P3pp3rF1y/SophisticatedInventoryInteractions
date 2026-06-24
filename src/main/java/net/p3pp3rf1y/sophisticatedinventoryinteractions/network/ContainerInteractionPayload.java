package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ActionValidationService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ContainerActionExecutor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;

import java.util.function.Supplier;

public record ContainerInteractionPayload(InteractionActionType actionType, boolean filterByContents, SortBy sortBy) {
	private static final ActionValidationService VALIDATION_SERVICE = new ActionValidationService();
	private static final ContainerActionExecutor ACTION_EXECUTOR = new ContainerActionExecutor();

	public static void encode(ContainerInteractionPayload payload, FriendlyByteBuf buffer) {
		buffer.writeEnum(payload.actionType());
		buffer.writeBoolean(payload.filterByContents());
		buffer.writeEnum(payload.sortBy());
	}

	public static ContainerInteractionPayload decode(FriendlyByteBuf buffer) {
		return new ContainerInteractionPayload(buffer.readEnum(InteractionActionType.class), buffer.readBoolean(), buffer.readEnum(SortBy.class));
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
		ActionValidationService.ValidationResult validationResult = VALIDATION_SERVICE.validate(serverPlayer, payload.actionType());
		if (!validationResult.valid()) {
			return;
		}
		ACTION_EXECUTOR.execute(serverPlayer, validationResult.menu(), validationResult.regions(), payload.actionType(), payload.filterByContents(),
				payload.sortBy());
	}
}
