package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDescriptor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

import java.util.List;

public class ScreenEligibilityService {
	public EligibilityDecision evaluate(EligibilityDescriptor descriptor) {
		EligibilityDecision safety = evaluate(descriptor, Config.DefaultPolicy.ALLOW_BY_DEFAULT, Config.CLIENT.minimumActionableContainerSlots.get(), List.of(),
				List.of());
		return safety.eligible() ? evaluateGroupRules(descriptor, ScreenControlScope.CONTAINER) : safety;
	}

	EligibilityDecision evaluate(EligibilityDescriptor descriptor, Config.DefaultPolicy defaultPolicy, int minimumActionableContainerSlots,
			List<? extends String> includedScreenClasses, List<? extends String> excludedScreenClasses) {
		if (descriptor.sophisticatedNativeScreen() || descriptor.hasCraftingRegion()
				|| descriptor.actionableContainerSlotCount() < minimumActionableContainerSlots || !descriptor.hasContainerRegion()
				|| !descriptor.hasPlayerRegion() || !descriptor.hasContainerAnchor() || !descriptor.hasPlayerAnchor()) {
			return EligibilityDecision.deny("structural_safety_deny");
		}
		return evaluateScreenRules(descriptor, defaultPolicy, includedScreenClasses, excludedScreenClasses);
	}

	public EligibilityDecision evaluateTransfers(EligibilityDescriptor descriptor) {
		return evaluateTransfers(descriptor, Config.CLIENT.defaultScreenPolicy.get(), ScreenControlScope.TRANSFERS.included().get(),
				ScreenControlScope.TRANSFERS.excluded().get(), Config.CLIENT.includeScreenClasses.get(), Config.CLIENT.excludeScreenClasses.get());
	}

	EligibilityDecision evaluateTransfers(EligibilityDescriptor descriptor, Config.DefaultPolicy policy, List<? extends String> included,
			List<? extends String> excluded, List<? extends String> legacyIncluded, List<? extends String> legacyExcluded) {
		if (descriptor.sophisticatedNativeScreen() || descriptor.hasCraftingRegion() || descriptor.actionableContainerSlotCount() == 0
				|| !descriptor.hasContainerRegion() || !descriptor.hasPlayerRegion() || !descriptor.hasContainerAnchor() || !descriptor.hasPlayerAnchor()) {
			return EligibilityDecision.deny("structural_safety_deny");
		}
		return evaluateGroupRules(descriptor, policy, included, excluded, legacyIncluded, legacyExcluded);
	}

	public EligibilityDecision evaluatePlayerSort(EligibilityDescriptor descriptor, SlotRegions regions) {
		if (!regions.hasPlayerMainRegion()) {
			return EligibilityDecision.deny("structural_safety_deny");
		}
		return evaluateGroupRules(descriptor, ScreenControlScope.PLAYER_SORT);
	}

	private EligibilityDecision evaluateGroupRules(EligibilityDescriptor descriptor, ScreenControlScope scope) {
		boolean player = scope == ScreenControlScope.PLAYER_SORT;
		return evaluateGroupRules(descriptor, player ? Config.DefaultPolicy.ALLOW_BY_DEFAULT : Config.CLIENT.defaultScreenPolicy.get(), scope.included().get(),
				scope.excluded().get(), player ? List.of() : Config.CLIENT.includeScreenClasses.get(),
				player ? List.of() : Config.CLIENT.excludeScreenClasses.get());
	}

	EligibilityDecision evaluateGroupRules(EligibilityDescriptor descriptor, Config.DefaultPolicy policy, List<? extends String> included,
			List<? extends String> excluded, List<? extends String> legacyIncluded, List<? extends String> legacyExcluded) {
		String className = descriptor.screenClassName();
		if (className != null && (matchesClassRule(included, className) || matchesClassRule(excluded, className))) {
			return evaluateScreenRules(descriptor, policy, included, excluded);
		}
		return evaluateScreenRules(descriptor, policy, legacyIncluded, legacyExcluded);
	}

	private EligibilityDecision evaluateScreenRules(EligibilityDescriptor descriptor, Config.DefaultPolicy defaultPolicy,
			List<? extends String> includedScreenClasses, List<? extends String> excludedScreenClasses) {
		String screenClassName = descriptor.screenClassName();
		if (screenClassName == null) {
			return EligibilityDecision.deny("missing_screen_class");
		}
		if (matchesClassRule(excludedScreenClasses, screenClassName)) {
			return EligibilityDecision.deny("exclude");
		}
		if (matchesClassRule(includedScreenClasses, screenClassName)) {
			return EligibilityDecision.allow("include");
		}

		return defaultPolicy == Config.DefaultPolicy.ALLOW_BY_DEFAULT ? EligibilityDecision.allow("default_allow") : EligibilityDecision.deny("default_deny");
	}

	public static boolean matchesClassRule(List<? extends String> rules, String className) {
		for (String rawRule : rules) {
			String rule = rawRule == null ? "" : rawRule.trim();
			if (rule.isEmpty()) {
				continue;
			}
			if (rule.endsWith("*")) {
				if (className.startsWith(rule.substring(0, rule.length() - 1))) {
					return true;
				}
			} else if (className.equals(rule)) {
				return true;
			}
		}
		return false;
	}
}
