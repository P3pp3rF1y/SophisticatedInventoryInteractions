package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class ClientScreenHistory {
	public static final int MAX_ENTRIES = 16;
	private final Deque<RecentScreen> entries = new ArrayDeque<>();

	public void record(ScreenContextResolver.ResolvedScreenContext context, String title, EligibilityDecision container, EligibilityDecision transfers,
			EligibilityDecision playerSort) {
		String className = context.eligibilityDescriptor().screenClassName();
		entries.removeIf(entry -> entry.screenClassName().equals(className));
		entries.addFirst(new RecentScreen(className, context.eligibilityDescriptor().menuClassName(), title, container, transfers, playerSort));
		while (entries.size() > MAX_ENTRIES) {
			entries.removeLast();
		}
	}

	public Optional<RecentScreen> last() {
		return Optional.ofNullable(entries.peekFirst());
	}

	public List<RecentScreen> recent(int count) {
		return new ArrayList<>(entries).stream().limit(count).toList();
	}

	public record RecentScreen(String screenClassName, String menuClassName, String title, EligibilityDecision container, EligibilityDecision transfers,
			EligibilityDecision playerSort) {
	}
}
