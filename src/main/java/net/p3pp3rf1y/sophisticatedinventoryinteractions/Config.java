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
		public final ModConfigSpec.EnumValue<DefaultPolicy> defaultPolicy;
		public final ModConfigSpec.ConfigValue<List<? extends String>> forceIncludeMenuClasses;
		public final ModConfigSpec.ConfigValue<List<? extends String>> excludeMenuClasses;
		public final ModConfigSpec.ConfigValue<List<? extends String>> includeMenuClasses;
		public final ModConfigSpec.ConfigValue<List<? extends String>> safeMenuClasses;
		public final ModConfigSpec.IntValue hideSearchAtOrBelowActionableContainerSlots;
		public final ModConfigSpec.IntValue hideSortByAtOrBelowActionableContainerSlots;
		public final ModConfigSpec.ConfigValue<List<? extends String>> menuSlotExclusions;
		public final ModConfigSpec.ConfigValue<List<? extends String>> anchorOffsetOverrides;

		public Common(ModConfigSpec.Builder builder) {
			builder.comment("Common settings");
			defaultPolicy = builder.comment("Default eligibility policy when no force/include/exclude/safe-list rule matches")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("defaultPolicy"))
					.defineEnum("defaultPolicy", DefaultPolicy.DENY_BY_DEFAULT);
			builder.comment("Menu class rules support exact class names and prefix matches using '*' suffix, e.g. net.minecraft.world.inventory.*");
			forceIncludeMenuClasses = defineStringList(builder, "forceInclude.menuClasses",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("forceInclude.menuClasses"), List.of());
			excludeMenuClasses = defineStringList(builder, "exclude.menuClasses",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("exclude.menuClasses"), List.of());
			includeMenuClasses = defineStringList(builder, "include.menuClasses",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("include.menuClasses"), List.of());
			safeMenuClasses = defineStringList(builder, "safeList.menuClasses",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("safeList.menuClasses"),
					List.of("net.minecraft.world.inventory.ChestMenu", "net.minecraft.world.inventory.HopperMenu",
							"net.minecraft.world.inventory.DispenserMenu", "net.minecraft.world.inventory.ShulkerBoxMenu",
							"net.minecraft.world.inventory.HorseInventoryMenu"));
			hideSearchAtOrBelowActionableContainerSlots = builder
					.comment("Hide the injected search box when the actionable container slot count is at or below this value.",
							"Actionable container slots are non-player menu slots after configured slot exclusions are removed.",
							"Examples with defaults: hopper (5) and dispenser/dropper (9) hide search, chest (27) keeps search.")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("controls.search.hideAtOrBelowActionableContainerSlots"))
					.defineInRange("controls.search.hideAtOrBelowActionableContainerSlots", 9, 0, Integer.MAX_VALUE);
			hideSortByAtOrBelowActionableContainerSlots = builder
					.comment("Hide the injected sort-by toggle when the actionable container slot count is at or below this value.",
							"When sort-by is absent, the sort button shifts into the rightmost container-control position.",
							"Examples with defaults: hopper (5) and dispenser/dropper (9) hide sort-by, chest (27) keeps it.")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("controls.sortBy.hideAtOrBelowActionableContainerSlots"))
					.defineInRange("controls.sortBy.hideAtOrBelowActionableContainerSlots", 9, 0, Integer.MAX_VALUE);
			builder.comment("Menu slot exclusions use menuClass=slotId[,slotId|-range...] and support '*' suffix prefix rules.",
					"Excluded menu slot ids are removed from actionable container counting, search, container sort, and transfers.",
					"Vanilla horse-like screens share HorseInventoryMenu; excluding 0,1 keeps saddle/armor or carpet slots out while leaving donkey and llama cargo actionable.",
					"Examples: net.minecraft.world.inventory.HorseInventoryMenu=0,1 or com.example.menu.*=0-2,5");
			menuSlotExclusions = defineStringList(builder, "slotExclusions.menuSlotOverrides",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("slotExclusions.menuSlotOverrides"),
					List.of("net.minecraft.world.inventory.HorseInventoryMenu=0,1"));
			builder.comment("Anchor offsets still use screenClass=x,y.",
					"Offsets now move the whole visible container-button group after search/sort-by visibility is decided.",
					"Default dispenser/dropper offset places the container sort button above player sort.",
					"Example: net.minecraft.client.gui.screens.inventory.DispenserScreen=54,0");
			anchorOffsetOverrides = defineStringList(builder, "layout.anchorOffsetOverrides",
					InventoryInteractionsTranslationHelper.INSTANCE.translConfig("layout.anchorOffsetOverrides"),
					List.of("net.minecraft.client.gui.screens.inventory.DispenserScreen=54,0"));
		}

		private ModConfigSpec.ConfigValue<List<? extends String>> defineStringList(ModConfigSpec.Builder builder, String key, String translationKey,
				List<String> defaults) {
			return builder.translation(translationKey).defineListAllowEmpty(key, defaults, o -> o instanceof String);
		}
	}

	public static class Client {
		public final ModConfigSpec.BooleanValue rememberSearchPhrase;

		public Client(ModConfigSpec.Builder builder) {
			builder.comment("Client settings");
			rememberSearchPhrase = builder
					.comment("Whether search phrase is remembered and shared between Sophisticated and non-Sophisticated container screens")
					.translation(InventoryInteractionsTranslationHelper.INSTANCE.translConfig("rememberSearchPhrase")).define("rememberSearchPhrase", true);
		}
	}
}
