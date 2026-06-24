package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.actions;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
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
	}

	@Test
	void mergeAndSortStacksOrdersByTotalCountWhenSortingByCount() {
		ContainerActionExecutor executor = new ContainerActionExecutor();
		List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.ANDESITE, 64), new ItemStack(Items.ANDESITE, 23),
				new ItemStack(Items.CLAY_BALL, 64), new ItemStack(Items.CLAY_BALL, 64), new ItemStack(Items.CLAY_BALL, 64), new ItemStack(Items.CLAY_BALL, 64),
				new ItemStack(Items.CLAY_BALL, 64), new ItemStack(Items.CLAY_BALL, 64), new ItemStack(Items.CLAY_BALL, 64),
				new ItemStack(Items.CLAY_BALL, 64)));

		List<Map.Entry<ItemStackKey, Integer>> sortedStacks = executor.mergeAndSortStacks(stacks, SortBy.COUNT);

		assertEquals(Items.CLAY_BALL, sortedStacks.get(0).getKey().getStack().getItem());
		assertEquals(512, sortedStacks.get(0).getValue());
		assertEquals(Items.ANDESITE, sortedStacks.get(1).getKey().getStack().getItem());
		assertEquals(87, sortedStacks.get(1).getValue());
	}
}
