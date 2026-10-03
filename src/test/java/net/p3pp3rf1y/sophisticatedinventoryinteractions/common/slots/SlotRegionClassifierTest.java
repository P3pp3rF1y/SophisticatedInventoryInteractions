package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.slots;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotRegionClassifierTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void acceptsUsableRegisteredStorageSlots() {
		TestMenu menu = new TestMenu();
		menu.addStorageSlot(new SimpleContainer(1), 0);

		SlotRegions regions = new SlotRegionClassifier().classify(menu);

		assertEquals(1, regions.actionableContainerSlotCount());
		assertFalse(regions.hasCraftingRegion());
	}

	@Test
	void rejectsSlotsWithoutUsableBackingCapacity() {
		TestMenu menu = new TestMenu();
		menu.addStorageSlot(new SimpleContainer(0), 0);

		SlotRegions regions = new SlotRegionClassifier().classify(menu);

		assertFalse(regions.hasContainerRegion());
	}

	@Test
	void excludesOnlyConfiguredUsableStorageSlots() {
		TestMenu menu = new TestMenu();
		menu.addStorageSlot(new SimpleContainer(1), 0);

		SlotRegions regions = new SlotRegionClassifier().classify(menu, null, Set.of(0));

		assertFalse(regions.hasContainerRegion());
		assertEquals(1, regions.excludedContainerSlotIndexes().size());
	}

	@Test
	void marksCraftingContainersUnsafeEvenWhenTheirSlotsAreUsable() {
		TestMenu menu = new TestMenu();
		Container craftingContainer = new TransientCraftingContainer(menu, 1, 1);
		menu.addStorageSlot(craftingContainer, 0);

		SlotRegions regions = new SlotRegionClassifier().classify(menu);

		assertTrue(regions.hasContainerRegion());
		assertTrue(regions.hasCraftingRegion());
	}

	private static class TestMenu extends AbstractContainerMenu {
		private TestMenu() {
			super(null, 0);
		}

		private void addStorageSlot(Container container, int containerSlot) {
			addSlot(new Slot(container, containerSlot, 0, 0));
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public ItemStack quickMoveStack(Player player, int index) {
			return ItemStack.EMPTY;
		}
	}
}
