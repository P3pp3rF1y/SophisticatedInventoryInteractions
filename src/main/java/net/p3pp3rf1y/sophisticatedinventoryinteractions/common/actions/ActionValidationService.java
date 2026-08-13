package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDescriptor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.MenuEligibilityService;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegionClassifier;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

public class ActionValidationService {
	private final MenuEligibilityService menuEligibilityService = new MenuEligibilityService();
	private final SlotRegionClassifier slotRegionClassifier = new SlotRegionClassifier();

	public ValidationResult validate(ServerPlayer player, InteractionActionType actionType) {
		AbstractContainerMenu menu = player.containerMenu;
		if (menu == null) {
			return ValidationResult.invalid("missing_menu");
		}

		SlotRegions regions = slotRegionClassifier.classify(menu);
		if (actionType == InteractionActionType.SORT_PLAYER && isSophisticatedMenu(menu)) {
			if (!regions.hasPlayerRegion()) {
				return ValidationResult.invalid("missing_player_region");
			}
			return ValidationResult.valid(menu, regions);
		}

		EligibilityDescriptor descriptor = new EligibilityDescriptor(null, menu.getClass().getName(), regions.actionableContainerSlotCount(),
				regions.hasContainerRegion(), regions.hasPlayerRegion(), regions.containerAnchor() != null, regions.playerAnchor() != null,
				menu instanceof StorageContainerMenuBase<?>);
		if (actionType == InteractionActionType.SORT_PLAYER) {
			if (!regions.hasPlayerMainRegion()) {
				return ValidationResult.invalid("missing_player_main_region");
			}
			EligibilityDecision playerSortEligibility = menuEligibilityService.evaluatePlayerSort(descriptor);
			if (!playerSortEligibility.eligible()) {
				return ValidationResult.invalid("ineligible_" + playerSortEligibility.reason());
			}
			return ValidationResult.valid(menu, regions);
		}

		EligibilityDecision eligibility = menuEligibilityService.evaluate(descriptor);
		if (!eligibility.eligible()) {
			return ValidationResult.invalid("ineligible_" + eligibility.reason());
		}

		if (actionType == InteractionActionType.SORT_CONTAINER || actionType == InteractionActionType.TRANSFER_TO_PLAYER) {
			if (!regions.hasContainerRegion()) {
				return ValidationResult.invalid("missing_container_region");
			}
		}
		if (actionType == InteractionActionType.TRANSFER_TO_CONTAINER) {
			if (!regions.hasPlayerRegion()) {
				return ValidationResult.invalid("missing_player_region");
			}
		}

		return ValidationResult.valid(menu, regions);
	}

	private boolean isSophisticatedMenu(AbstractContainerMenu menu) {
		return menu instanceof StorageContainerMenuBase<?>;
	}

	public record ValidationResult(boolean valid, String reason, AbstractContainerMenu menu, SlotRegions regions) {
		public static ValidationResult valid(AbstractContainerMenu menu, SlotRegions regions) {
			return new ValidationResult(true, "ok", menu, regions);
		}

		public static ValidationResult invalid(String reason) {
			return new ValidationResult(false, reason, null, null);
		}
	}
}
