package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;

import java.util.List;

public class MenuEligibilityService {
	public EligibilityDecision evaluate(EligibilityDescriptor descriptor) {
		if (isHardDenied(descriptor)) {
			return EligibilityDecision.deny("hard_safety_deny");
		}

		List<? extends String> forceMenuClasses = Config.COMMON.forceIncludeMenuClasses.get();
		if (matchesClassRule(forceMenuClasses, descriptor.menuClassName())) {
			return EligibilityDecision.allow("force_include");
		}

		List<? extends String> excludedMenuClasses = Config.COMMON.excludeMenuClasses.get();
		if (matchesClassRule(excludedMenuClasses, descriptor.menuClassName())) {
			return EligibilityDecision.deny("exclude");
		}

		List<? extends String> includedMenuClasses = Config.COMMON.includeMenuClasses.get();
		if (matchesClassRule(includedMenuClasses, descriptor.menuClassName())) {
			return EligibilityDecision.allow("include");
		}

		List<? extends String> safeListMenuClasses = Config.COMMON.safeMenuClasses.get();
		if (matchesClassRule(safeListMenuClasses, descriptor.menuClassName())) {
			return EligibilityDecision.allow("safe_list");
		}

		if (Config.COMMON.defaultPolicy.get() == Config.DefaultPolicy.ALLOW_BY_DEFAULT) {
			return EligibilityDecision.allow("default_allow");
		}

		return EligibilityDecision.deny("default_deny");
	}

	private boolean isHardDenied(EligibilityDescriptor descriptor) {
		if (descriptor.sophisticatedNativeScreen()) {
			return true;
		}
		if (!descriptor.hasContainerRegion() || !descriptor.hasPlayerRegion()) {
			return true;
		}
		return !descriptor.hasContainerAnchor() || !descriptor.hasPlayerAnchor();
	}

	private boolean matchesClassRule(List<? extends String> rules, String className) {
		for (String rawRule : rules) {
			String rule = rawRule == null ? "" : rawRule.trim();
			if (rule.isEmpty()) {
				continue;
			}
			if (rule.endsWith("*")) {
				String prefix = rule.substring(0, rule.length() - 1);
				if (className.startsWith(prefix)) {
					return true;
				}
			} else if (className.equals(rule)) {
				return true;
			}
		}
		return false;
	}
}
