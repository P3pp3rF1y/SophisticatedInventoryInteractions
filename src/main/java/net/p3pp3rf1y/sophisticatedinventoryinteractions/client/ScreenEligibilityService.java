package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDescriptor;

import java.util.List;

public class ScreenEligibilityService {
	public EligibilityDecision evaluate(EligibilityDescriptor descriptor) {
		return evaluate(descriptor, Config.CLIENT.defaultScreenPolicy.get(), Config.CLIENT.minimumActionableContainerSlots.get(),
				Config.CLIENT.includeScreenClasses.get(), Config.CLIENT.excludeScreenClasses.get());
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

	public EligibilityDecision evaluatePlayerSort(EligibilityDescriptor descriptor) {
		if (descriptor.sophisticatedNativeScreen() || !descriptor.hasPlayerRegion() || !descriptor.hasPlayerAnchor()) {
			return EligibilityDecision.deny("structural_safety_deny");
		}
		return evaluateScreenRules(descriptor, Config.CLIENT.defaultScreenPolicy.get(), Config.CLIENT.includeScreenClasses.get(),
				Config.CLIENT.excludeScreenClasses.get());
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
