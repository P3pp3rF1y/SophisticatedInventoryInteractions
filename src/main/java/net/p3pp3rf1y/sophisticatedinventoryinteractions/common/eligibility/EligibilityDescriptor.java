package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility;

import javax.annotation.Nullable;

public record EligibilityDescriptor(
		@Nullable String screenClassName,
		String menuClassName,
		int actionableContainerSlotCount,
		boolean hasContainerRegion,
		boolean hasPlayerRegion,
		boolean hasContainerAnchor,
		boolean hasPlayerAnchor,
		boolean sophisticatedNativeScreen
) {
}
