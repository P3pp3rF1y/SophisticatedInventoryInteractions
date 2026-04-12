package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContainerActionExecutorTest {
	@BeforeAll
	static void setup() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Bootstrap.validate();
		bindTestComponents(Items.GOLD_INGOT, Items.IRON_INGOT);
	}

	private static void bindTestComponents(Item... items) {
		DataComponentMap components = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
		for (Item item : items) {
			item.builtInRegistryHolder().bindComponents(components);
		}
	}

	@Test
	void mergeAndSortStacksOrdersByTotalCountWhenSortingByCount() {
		ContainerActionExecutor executor = new ContainerActionExecutor();
		List<ItemStack> stacks = new ArrayList<>(List.of(
				new ItemStack(Items.IRON_INGOT, 64),
				new ItemStack(Items.IRON_INGOT, 23),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64),
				new ItemStack(Items.GOLD_INGOT, 64)
		));

		List<Map.Entry<ItemStackKey, Integer>> sortedStacks = executor.mergeAndSortStacks(stacks, SortBy.COUNT);

		assertEquals(Items.GOLD_INGOT, sortedStacks.getFirst().getKey().stack().getItem());
		assertEquals(512, sortedStacks.getFirst().getValue());
		assertEquals(Items.IRON_INGOT, sortedStacks.get(1).getKey().stack().getItem());
		assertEquals(87, sortedStacks.get(1).getValue());
	}
}
