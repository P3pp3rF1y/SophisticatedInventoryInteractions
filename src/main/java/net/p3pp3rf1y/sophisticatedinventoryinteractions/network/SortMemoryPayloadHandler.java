package net.p3pp3rf1y.sophisticatedinventoryinteractions.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.ActionValidationService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions.InteractionActionType;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort.InventoryInteractionSortMemory;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort.StorageSortKey;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.sort.StorageSortKeyResolver;

import java.util.Optional;

public class SortMemoryPayloadHandler {
	private static final ActionValidationService VALIDATION_SERVICE = new ActionValidationService();
	private static final StorageSortKeyResolver STORAGE_SORT_KEY_RESOLVER = new StorageSortKeyResolver();

	private SortMemoryPayloadHandler() {
	}

	public static void handleRequest(ServerPlayer player) {
		resolveCurrentStorageKey(player).ifPresent(storageKey -> InventoryInteractionSortMemory.get((ServerLevel) player.level()).getSortBy(storageKey)
				.ifPresent(sortBy -> PacketDistributor.sendToPlayer(player, SyncSortMemoryPayload.saved(player.containerMenu.containerId, sortBy))));
	}

	public static void handleSet(ServerPlayer player, SortBy sortBy) {
		resolveCurrentStorageKey(player).ifPresent(storageKey -> {
			InventoryInteractionSortMemory.get((ServerLevel) player.level()).setSortBy(storageKey, sortBy);
			syncOpenPlayers(player, storageKey, sortBy);
		});
	}

	private static void syncOpenPlayers(ServerPlayer sourcePlayer, StorageSortKey changedStorageKey, SortBy sortBy) {
		for (ServerPlayer player : sourcePlayer.level().getServer().getPlayerList().getPlayers()) {
			resolveCurrentStorageKey(player).filter(changedStorageKey::equals)
					.ifPresent(storageKey -> PacketDistributor.sendToPlayer(player,
							sortBy == SortBy.NAME
									? SyncSortMemoryPayload.notSaved(player.containerMenu.containerId)
									: SyncSortMemoryPayload.saved(player.containerMenu.containerId, sortBy)));
		}
	}

	private static Optional<StorageSortKey> resolveCurrentStorageKey(ServerPlayer player) {
		ActionValidationService.ValidationResult validationResult = VALIDATION_SERVICE.validate(player, InteractionActionType.SORT_CONTAINER);
		if (!validationResult.valid()) {
			return Optional.empty();
		}
		return STORAGE_SORT_KEY_RESOLVER.resolve(validationResult.menu(), validationResult.regions());
	}
}
