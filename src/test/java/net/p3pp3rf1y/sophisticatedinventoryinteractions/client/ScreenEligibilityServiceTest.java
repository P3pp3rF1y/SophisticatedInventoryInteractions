package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
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
	void excludedScreenWinsOverIncludedScreen() {
		assertFalse(new ScreenEligibilityService()
				.evaluate(ELIGIBLE_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, List.of("example.screen.*"), List.of("example.screen.StorageScreen"))
				.eligible());
	}

	@Test
	void includedScreenOverridesDenyByDefault() {
		assertTrue(new ScreenEligibilityService().evaluate(ELIGIBLE_SCREEN, Config.DefaultPolicy.DENY_BY_DEFAULT, 9, List.of("example.screen.*"), List.of())
				.eligible());
	}

	@Test
	void craftingRegionCannotBeForcedByScreenRule() {
		assertFalse(new ScreenEligibilityService()
				.evaluate(CRAFTING_SCREEN, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, List.of("example.screen.CraftingScreen"), List.of()).eligible());
	}

	@Test
	void screenRulesCannotOverrideMinimumContainerSlotCount() {
		EligibilityDescriptor anvilLikeScreen = new EligibilityDescriptor("example.screen.AnvilScreen", "example.menu.AnvilMenu", 2, true, false, true, true,
				true, false);

		assertFalse(new ScreenEligibilityService()
				.evaluate(anvilLikeScreen, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, List.of("example.screen.AnvilScreen"), List.of()).eligible());
	}

	@Test
	void minimumContainerSlotCountIncludesTheThreshold() {
		EligibilityDescriptor dispenserLikeScreen = new EligibilityDescriptor("example.screen.DispenserScreen", "example.menu.DispenserMenu", 9, true, false,
				true, true, true, false);

		assertTrue(new ScreenEligibilityService().evaluate(dispenserLikeScreen, Config.DefaultPolicy.ALLOW_BY_DEFAULT, 9, List.of(), List.of()).eligible());
	}
}
