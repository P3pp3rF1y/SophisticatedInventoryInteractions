package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;

import java.util.LinkedHashSet;
import java.util.Set;

public class MenuSlotExclusionResolver {
	public Set<Integer> getExcludedSlotIds(String menuClassName) {
		Set<Integer> excludedSlotIds = new LinkedHashSet<>();
		for (String rawEntry : Config.COMMON.menuSlotExclusions.get()) {
			parseEntry(rawEntry, menuClassName, excludedSlotIds);
		}
		return excludedSlotIds;
	}

	private void parseEntry(String rawEntry, String menuClassName, Set<Integer> excludedSlotIds) {
		String entry = rawEntry == null ? "" : rawEntry.trim();
		if (entry.isEmpty()) {
			return;
		}

		int separatorIndex = entry.indexOf('=');
		if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
			return;
		}

		String classRule = entry.substring(0, separatorIndex).trim();
		if (!matchesClassRule(classRule, menuClassName)) {
			return;
		}

		String[] slotSpecs = entry.substring(separatorIndex + 1).split(",");
		for (String rawSlotSpec : slotSpecs) {
			parseSlotSpec(rawSlotSpec, excludedSlotIds);
		}
	}

	private boolean matchesClassRule(String classRule, String menuClassName) {
		if (classRule.isEmpty()) {
			return false;
		}
		if (classRule.endsWith("*")) {
			return menuClassName.startsWith(classRule.substring(0, classRule.length() - 1));
		}
		return menuClassName.equals(classRule);
	}

	private void parseSlotSpec(String rawSlotSpec, Set<Integer> excludedSlotIds) {
		String slotSpec = rawSlotSpec.trim();
		if (slotSpec.isEmpty()) {
			return;
		}

		int dashIndex = slotSpec.indexOf('-');
		if (dashIndex < 0) {
			addSlotId(slotSpec, excludedSlotIds);
			return;
		}

		String startPart = slotSpec.substring(0, dashIndex).trim();
		String endPart = slotSpec.substring(dashIndex + 1).trim();
		try {
			int start = Integer.parseInt(startPart);
			int end = Integer.parseInt(endPart);
			if (start < 0 || end < start) {
				return;
			}
			for (int slotId = start; slotId <= end; slotId++) {
				excludedSlotIds.add(slotId);
			}
		} catch (NumberFormatException ignored) {
		}
	}

	private void addSlotId(String slotSpec, Set<Integer> excludedSlotIds) {
		try {
			int slotId = Integer.parseInt(slotSpec);
			if (slotId >= 0) {
				excludedSlotIds.add(slotId);
			}
		} catch (NumberFormatException ignored) {
		}
	}
}
