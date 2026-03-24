package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDescriptor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegionClassifier;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots.SlotRegions;

import java.util.Optional;

public class ScreenContextResolver {
	private final SlotRegionClassifier slotRegionClassifier = new SlotRegionClassifier();

	public Optional<ResolvedScreenContext> resolve(AbstractContainerScreen<?> screen) {
		if (screen.getMinecraft().player == null) {
			return Optional.empty();
		}

		AbstractContainerMenu menu = screen.getMenu();
		SlotRegions regions = slotRegionClassifier.classify(menu);
		String screenClassName = screen.getClass().getName();
		String menuClassName = menu.getClass().getName();
		boolean sophisticatedNativeScreen = screen instanceof StorageScreenBase<?> || menu instanceof StorageContainerMenuBase<?>;

		EligibilityDescriptor descriptor = new EligibilityDescriptor(
				screenClassName,
				menuClassName,
				regions.actionableContainerSlotCount(),
				regions.hasContainerRegion(),
				regions.hasPlayerRegion(),
				regions.containerAnchor() != null,
				regions.playerAnchor() != null,
				sophisticatedNativeScreen
		);

		return Optional.of(new ResolvedScreenContext(screen, regions, descriptor));
	}

	public record ResolvedScreenContext(AbstractContainerScreen<?> screen, SlotRegions slotRegions, EligibilityDescriptor eligibilityDescriptor) {
	}
}
