package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SearchPhraseMatcher {
	private SearchPhraseMatcher() {
	}

	public static boolean matches(Minecraft minecraft, ItemStack stack, String searchPhrase) {
		if (searchPhrase == null || searchPhrase.trim().isEmpty()) {
			return true;
		}
		if (stack.isEmpty()) {
			return false;
		}

		String[] searchTerms = searchPhrase.trim().split(" ");
		List<Predicate<ItemStack>> filters = new ArrayList<>();
		for (String searchTerm : searchTerms) {
			if (searchTerm.isBlank()) {
				continue;
			}
			if (searchTerm.startsWith("@")) {
				String modName = searchTerm.substring(1).toLowerCase();
				filters.add(itemStack -> modName.isEmpty() || BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getNamespace().contains(modName));
			} else if (searchTerm.startsWith("#")) {
				String tooltipKeyword = searchTerm.substring(1).toLowerCase();
				filters.add(itemStack -> tooltipKeyword.isEmpty() || hasTooltipKeyword(minecraft, itemStack, tooltipKeyword));
			} else {
				String namePart = searchTerm.toLowerCase();
				filters.add(itemStack -> itemStack.getHoverName().getString().toLowerCase().contains(namePart));
			}
		}

		return filters.stream().allMatch(filter -> filter.test(stack));
	}

	private static boolean hasTooltipKeyword(Minecraft minecraft, ItemStack stack, String keyword) {
		for (Component line : Screen.getTooltipFromItem(minecraft, stack)) {
			if (line.getString().toLowerCase().contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
