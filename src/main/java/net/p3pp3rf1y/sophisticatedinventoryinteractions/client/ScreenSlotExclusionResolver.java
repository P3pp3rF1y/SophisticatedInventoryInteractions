package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ScreenSlotExclusionResolver {
	public Set<Integer> getExcludedSlotIds(String screenClassName) {
		return getExcludedSlotIds(screenClassName, Config.CLIENT.screenSlotExclusions.get());
	}

	Set<Integer> getExcludedSlotIds(String screenClassName, List<? extends String> entries) {
		Set<Integer> excludedSlotIds = new LinkedHashSet<>();
		for (String rawEntry : entries) {
			parseEntry(rawEntry, screenClassName, excludedSlotIds);
		}
		return excludedSlotIds;
	}

	private void parseEntry(String rawEntry, String screenClassName, Set<Integer> excludedSlotIds) {
		String entry = rawEntry == null ? "" : rawEntry.trim();
		int separatorIndex = entry.indexOf('=');
		if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1
				|| !ScreenEligibilityService.matchesClassRule(List.of(entry.substring(0, separatorIndex).trim()), screenClassName)) {
			return;
		}

		for (String rawSlotSpec : entry.substring(separatorIndex + 1).split(",")) {
			parseSlotSpec(rawSlotSpec, excludedSlotIds);
		}
	}

	private void parseSlotSpec(String rawSlotSpec, Set<Integer> excludedSlotIds) {
		String slotSpec = rawSlotSpec.trim();
		int dashIndex = slotSpec.indexOf('-');
		if (dashIndex < 0) {
			addSlotId(slotSpec, excludedSlotIds);
			return;
		}

		try {
			int start = Integer.parseInt(slotSpec.substring(0, dashIndex).trim());
			int end = Integer.parseInt(slotSpec.substring(dashIndex + 1).trim());
			if (start >= 0 && end >= start) {
				for (int slotId = start; slotId <= end; slotId++) {
					excludedSlotIds.add(slotId);
				}
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
