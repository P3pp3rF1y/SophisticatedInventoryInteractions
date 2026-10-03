package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenSlotExclusionResolverTest {
	@Test
	void resolvesExactAndWildcardScreenRules() {
		Set<Integer> excludedSlotIds = new ScreenSlotExclusionResolver().getExcludedSlotIds("example.client.StorageScreen",
				List.of("example.client.*=0,2-3", "example.client.StorageScreen=5"));

		assertEquals(Set.of(0, 2, 3, 5), excludedSlotIds);
	}
}
