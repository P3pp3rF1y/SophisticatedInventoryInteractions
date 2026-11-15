package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenEligibilityServiceTest {
	private static final EligibilityDescriptor ELIGIBLE_SCREEN = new EligibilityDescriptor("example.screen.StorageScreen", "example.menu.StorageMenu", 27, true,
			false, true, true, true, false);
	private static final EligibilityDescriptor CRAFTING_SCREEN = new EligibilityDescriptor("example.screen.CraftingScreen", "example.menu.CraftingMenu", 9,
			true, true, true, true, true, false);

	@Test
	void evaluateExcludesScreenWhenBothIncludeAndExcludeMatch() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> included = List.of("example.screen.*");
		List<String> excluded = List.of("example.screen.StorageScreen");
		EligibilityDecision decision = service.evaluate(ELIGIBLE_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, included, excluded);
		assertFalse(decision.eligible());
	}

	@Test
	void evaluateAllowsIncludedScreenDespiteDenyByDefault() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> included = List.of("example.screen.*");
		EligibilityDecision decision = service.evaluate(ELIGIBLE_SCREEN, Config.DefaultPolicy.DENY_BY_DEFAULT, 9, included, List.of());
		assertTrue(decision.eligible());
	}

	@Test
	void evaluateRejectsCraftingRegionDespiteIncludeRule() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> included = List.of("example.screen.CraftingScreen");
		EligibilityDecision decision = service.evaluate(CRAFTING_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, included, List.of());
		assertFalse(decision.eligible());
	}

	@Test
	void evaluateRejectsScreenBelowMinimumSlotCountDespiteIncludeRule() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		EligibilityDescriptor anvilLikeScreen = new EligibilityDescriptor("example.screen.AnvilScreen", "example.menu.AnvilMenu", 2, true, false, true, true,
				true, false);
		List<String> included = List.of("example.screen.AnvilScreen");
		EligibilityDecision decision = service.evaluate(anvilLikeScreen, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, included, List.of());
		assertFalse(decision.eligible());
	}

	@Test
	void evaluateAllowsScreenAtMinimumSlotCount() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		EligibilityDescriptor dispenserLikeScreen = new EligibilityDescriptor("example.screen.DispenserScreen", "example.menu.DispenserMenu", 9, true, false,
				true, true, true, false);
		EligibilityDecision decision = service.evaluate(dispenserLikeScreen, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, List.of(), List.of());
		assertTrue(decision.eligible());
	}

	@Test
	void evaluateTransfersAllowsScreenBelowContainerControlMinimum() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		EligibilityDescriptor smallMachine = new EligibilityDescriptor("example.screen.MachineScreen", "example.menu.MachineMenu", 1, true, false, true, true,
				true, false);
		EligibilityDecision containerDecision = service.evaluate(smallMachine, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, List.of(), List.of());
		EligibilityDecision transferDecision = service.evaluateTransfers(smallMachine, Config.DefaultPolicy.ALLOW_BY_DEFAULT, List.of(), List.of(), List.of(),
				List.of());
		assertFalse(containerDecision.eligible());
		assertTrue(transferDecision.eligible());
	}

	@Test
	void evaluateTransfersRejectsCraftingRegionDespiteIncludeRule() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> included = List.of("example.screen.*");
		EligibilityDecision decision = service.evaluateTransfers(CRAFTING_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, included, List.of(), List.of(),
				List.of());
		assertFalse(decision.eligible());
	}

	@Test
	void evaluateTransfersRejectsScreenMatchingLegacyExclusion() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> legacyExcluded = List.of("example.screen.*");
		EligibilityDecision decision = service.evaluateTransfers(ELIGIBLE_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, List.of(), List.of(), List.of(),
				legacyExcluded);
		assertFalse(decision.eligible());
	}

	@Test
	void evaluateGroupRulesAllowsScopedInclusionDespiteLegacyExclusion() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> included = List.of("example.screen.StorageScreen");
		List<String> legacyExcluded = List.of("example.screen.*");
		EligibilityDecision decision = service.evaluateGroupRules(ELIGIBLE_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, included, List.of(), List.of(),
				legacyExcluded);
		assertTrue(decision.eligible());
	}

	@Test
	void evaluateGroupRulesUsesLegacyExclusionWithoutMatchingScopedRule() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> legacyExcluded = List.of("example.screen.*");
		EligibilityDecision decision = service.evaluateGroupRules(ELIGIBLE_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, List.of(), List.of(), List.of(),
				legacyExcluded);
		assertFalse(decision.eligible());
	}

	@Test
	void evaluateGroupRulesExcludesScreenWhenBothScopedIncludeAndExcludeMatch() {
		ScreenEligibilityService service = new ScreenEligibilityService();
		List<String> included = List.of("example.screen.*");
		List<String> excluded = List.of("example.screen.StorageScreen");
		EligibilityDecision decision = service.evaluateGroupRules(ELIGIBLE_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, included, excluded, List.of(),
				List.of());
		assertFalse(decision.eligible());
	}
}
