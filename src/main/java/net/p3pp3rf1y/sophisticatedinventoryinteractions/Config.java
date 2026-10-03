package net.p3pp3rf1y.sophisticatedinventoryinteractions;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class Config {
	private Config() {
	}

	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Client CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	static {
		Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON = specPair.getLeft();
		COMMON_SPEC = specPair.getRight();
		Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT = clientSpecPair.getLeft();
		CLIENT_SPEC = clientSpecPair.getRight();
	}

	public enum DefaultPolicy {
		DENY_BY_DEFAULT, ALLOW_BY_DEFAULT
	}

	public static class Common {
		public final ForgeConfigSpec.IntValue hideSearchAtOrBelowActionableContainerSlots;
		public final ForgeConfigSpec.IntValue hideSortByAtOrBelowActionableContainerSlots;

		public Common(ForgeConfigSpec.Builder builder) {
			builder.comment("Common settings").push("common");
			hideSearchAtOrBelowActionableContainerSlots = builder
					.comment("Hide the injected search box when the actionable container slot count is at or below this value.",
							"Actionable container slots are non-player menu slots after configured slot exclusions are removed.",
							"Examples with defaults: hopper (5) and dispenser/dropper (9) hide search, chest (27) keeps search.")
					.defineInRange("controls.search.hideAtOrBelowActionableContainerSlots", 9, 0, Integer.MAX_VALUE);
			hideSortByAtOrBelowActionableContainerSlots = builder
					.comment("Hide the injected sort-by toggle when the actionable container slot count is at or below this value.",
							"When sort-by is absent, the sort button shifts into the rightmost container-control position.",
							"Examples with defaults: hopper (5) and dispenser/dropper (9) hide sort-by, chest (27) keeps it.")
					.defineInRange("controls.sortBy.hideAtOrBelowActionableContainerSlots", 9, 0, Integer.MAX_VALUE);
			builder.pop();
		}
	}

	public static class Client {
		private static final int DEFAULT_MINIMUM_ACTIONABLE_CONTAINER_SLOTS = 9;

		public final ForgeConfigSpec.BooleanValue rememberSearchPhrase;
		public final ForgeConfigSpec.EnumValue<DefaultPolicy> defaultScreenPolicy;
		public final ForgeConfigSpec.IntValue minimumActionableContainerSlots;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> excludeScreenClasses;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> includeScreenClasses;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> screenSlotExclusions;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> anchorOffsetOverrides;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.comment("Client settings").push("client");
			rememberSearchPhrase = builder
					.comment("Whether search phrase is remembered and shared between Sophisticated and non-Sophisticated container screens")
					.define("rememberSearchPhrase", true);
			builder.comment("Screen rules accept full class names or prefixes ending in '*'.",
					"They only apply to screens that are safe for inventory interactions.").push("compatibility");
			defaultScreenPolicy = builder.comment("Choose whether compatible screens are allowed unless listed below.").defineEnum("defaultScreenPolicy",
					DefaultPolicy.ALLOW_BY_DEFAULT);
			minimumActionableContainerSlots = builder
					.comment("Only adds inventory controls to containers with at least this many usable slots.",
							"With the default, anvils (2) are skipped while dispensers and droppers (9) are included.")
					.defineInRange("minimumActionableContainerSlots", DEFAULT_MINIMUM_ACTIONABLE_CONTAINER_SLOTS, 0, Integer.MAX_VALUE);
			excludeScreenClasses = defineStringList(builder, "exclude.screenClasses", List.of());
			includeScreenClasses = defineStringList(builder, "include.screenClasses", List.of());
			builder.comment("Lets you leave specific slots out of inventory interactions.",
					"Use screenClass=menuSlotId[,menuSlotId|-range...]. Screen class prefixes can end in '*'.",
					"Excluded slots are ignored by the slot count, search, sorting, and transfer buttons.",
					"Examples: net.minecraft.client.gui.screens.inventory.HorseInventoryScreen=0,1 or com.example.client.*=0-2,5");
			screenSlotExclusions = defineStringList(builder, "slotExclusions.screenSlotOverrides",
					List.of("net.minecraft.client.gui.screens.inventory.HorseInventoryScreen=0,1"));
			builder.pop();
			builder.comment("Moves the inventory buttons for specific screens.", "Use screenClass=x,y.",
					"Example: net.minecraft.client.gui.screens.inventory.DispenserScreen=54,0").push("layout");
			anchorOffsetOverrides = defineStringList(builder, "anchorOffsetOverrides",
					List.of("net.minecraft.client.gui.screens.inventory.DispenserScreen=54,0"));
			builder.pop();
			builder.pop();
		}

		private ForgeConfigSpec.ConfigValue<List<? extends String>> defineStringList(ForgeConfigSpec.Builder builder, String key, List<String> defaults) {
			return builder.defineListAllowEmpty(key, defaults, o -> o instanceof String);
		}
	}
}
