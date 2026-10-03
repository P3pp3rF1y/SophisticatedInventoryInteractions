package net.p3pp3rf1y.sophisticatedinventoryinteractions;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.gui.InventoryInteractionsTranslationHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Locale;

public class Config {
	private Config() {
	}

	public static final Common COMMON;
	public static final ModConfigSpec COMMON_SPEC;
	public static final Client CLIENT;
	public static final ModConfigSpec CLIENT_SPEC;

	static {
		Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
		COMMON = specPair.getLeft();
		COMMON_SPEC = specPair.getRight();
		Pair<Client, ModConfigSpec> clientSpecPair = new ModConfigSpec.Builder().configure(Client::new);
		CLIENT = clientSpecPair.getLeft();
		CLIENT_SPEC = clientSpecPair.getRight();
	}

	public enum DefaultPolicy implements TranslatableEnum {
		DENY_BY_DEFAULT, ALLOW_BY_DEFAULT;

		@Override
		public Component getTranslatedName() {
			return Component.translatable(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("defaultPolicy." + name().toLowerCase(Locale.ROOT)));
		}
	}

	public static class Common {
		public final ModConfigSpec.IntValue hideSearchAtOrBelowActionableContainerSlots;
		public final ModConfigSpec.IntValue hideSortByAtOrBelowActionableContainerSlots;

		public Common(ModConfigSpec.Builder builder) {
			builder.comment("Common settings").translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("common")).push("common");
			hideSearchAtOrBelowActionableContainerSlots = builder
					.comment("Hides the search box in containers with this many usable slots or fewer.",
							"Usable slots are non-player slots, excluding anything listed in Screen Slot Exclusions.",
							"With the default, search is hidden in hoppers (5) and dispensers/droppers (9), but shown in chests (27).")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("controls.search.hideAtOrBelowActionableContainerSlots"))
					.defineInRange("controls.search.hideAtOrBelowActionableContainerSlots", 9, 0, Integer.MAX_VALUE);
			hideSortByAtOrBelowActionableContainerSlots = builder
					.comment("Hides the sort-by button in containers with this many usable slots or fewer.",
							"When hidden, the sort button moves into its place.",
							"With the default, sort-by is hidden in hoppers (5) and dispensers/droppers (9), but shown in chests (27).")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("controls.sortBy.hideAtOrBelowActionableContainerSlots"))
					.defineInRange("controls.sortBy.hideAtOrBelowActionableContainerSlots", 9, 0, Integer.MAX_VALUE);
			builder.pop();
		}
	}

	public static class Client {
		private static final int DEFAULT_MINIMUM_ACTIONABLE_CONTAINER_SLOTS = 9;

		public final ModConfigSpec.BooleanValue rememberSearchPhrase;
		public final ModConfigSpec.EnumValue<DefaultPolicy> defaultScreenPolicy;
		public final ModConfigSpec.IntValue minimumActionableContainerSlots;
		public final ModConfigSpec.ConfigValue<List<? extends String>> excludeScreenClasses;
		public final ModConfigSpec.ConfigValue<List<? extends String>> includeScreenClasses;
		public final ModConfigSpec.ConfigValue<List<? extends String>> screenSlotExclusions;
		public final ModConfigSpec.ConfigValue<List<? extends String>> anchorOffsetOverrides;

		public Client(ModConfigSpec.Builder builder) {
			builder.comment("Client settings").translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("client")).push("client");
			rememberSearchPhrase = builder.comment("Remembers your search phrase when you open another inventory.")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("rememberSearchPhrase")).define("rememberSearchPhrase", true);
			builder.comment("Screen rules accept full class names or prefixes ending in '*'.",
					"They only apply to screens that are safe for inventory interactions.").push("compatibility");
			defaultScreenPolicy = builder.comment("Choose whether compatible screens are allowed unless listed below.")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("defaultScreenPolicy"))
					.defineEnum("defaultScreenPolicy", DefaultPolicy.ALLOW_BY_DEFAULT);
			minimumActionableContainerSlots = builder
					.comment("Only adds inventory controls to containers with at least this many usable slots.",
							"With the default, anvils (2) are skipped while dispensers and droppers (9) are included.")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("minimumActionableContainerSlots"))
					.defineInRange("minimumActionableContainerSlots", DEFAULT_MINIMUM_ACTIONABLE_CONTAINER_SLOTS, 0, Integer.MAX_VALUE);
			excludeScreenClasses = defineStringList(builder, "exclude.screenClasses",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("exclude.screenClasses"), List.of());
			includeScreenClasses = defineStringList(builder, "include.screenClasses",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("include.screenClasses"), List.of());
			builder.comment("Lets you leave specific slots out of inventory interactions.",
					"Use screenClass=menuSlotId[,menuSlotId|-range...]. Screen class prefixes can end in '*'.",
					"Excluded slots are ignored by the slot count, search, sorting, and transfer buttons.",
					"Examples: net.minecraft.client.gui.screens.inventory.HorseInventoryScreen=0,1 or com.example.client.*=0-2,5");
			screenSlotExclusions = defineStringList(builder, "slotExclusions.screenSlotOverrides",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("slotExclusions.screenSlotOverrides"),
					List.of("net.minecraft.client.gui.screens.inventory.HorseInventoryScreen=0,1"));
			builder.pop();
			builder.comment("Moves the inventory buttons for specific screens.", "Use screenClass=x,y.",
					"Example: net.minecraft.client.gui.screens.inventory.DispenserScreen=54,0").push("layout");
			anchorOffsetOverrides = defineStringList(builder, "anchorOffsetOverrides",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("layout.anchorOffsetOverrides"),
					List.of("net.minecraft.client.gui.screens.inventory.DispenserScreen=54,0"));
			builder.pop();
			builder.pop();
		}

		private ModConfigSpec.ConfigValue<List<? extends String>> defineStringList(ModConfigSpec.Builder builder, String key, String translationKey,
				List<String> defaults) {
			return builder.translation(translationKey).defineListAllowEmpty(key, defaults, o -> o instanceof String);
		}
	}
}
