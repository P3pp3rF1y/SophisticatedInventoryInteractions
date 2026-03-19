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
		DENY_BY_DEFAULT,
		ALLOW_BY_DEFAULT
	}

	public static class Common {
		public final ForgeConfigSpec.EnumValue<DefaultPolicy> defaultPolicy;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> forceIncludeMenuClasses;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> excludeMenuClasses;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> includeMenuClasses;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> safeMenuClasses;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> anchorOffsetOverrides;

		public Common(ForgeConfigSpec.Builder builder) {
			builder.comment("Common settings").push("common");
			defaultPolicy = builder.comment("Default eligibility policy when no force/include/exclude/safe-list rule matches")
					.defineEnum("defaultPolicy", DefaultPolicy.DENY_BY_DEFAULT);
			builder.comment("Menu class rules support exact class names and prefix matches using '*' suffix, e.g. net.minecraft.world.inventory.*");
			forceIncludeMenuClasses = defineStringList(builder, "forceInclude.menuClasses", List.of());
			excludeMenuClasses = defineStringList(builder, "exclude.menuClasses", List.of());
			includeMenuClasses = defineStringList(builder, "include.menuClasses", List.of());
			safeMenuClasses = defineStringList(builder, "safeList.menuClasses", List.of(
					"net.minecraft.world.inventory.ChestMenu",
					"net.minecraft.world.inventory.HopperMenu",
					"net.minecraft.world.inventory.DispenserMenu",
					"net.minecraft.world.inventory.ShulkerBoxMenu"
			));
			anchorOffsetOverrides = defineStringList(builder, "layout.anchorOffsetOverrides", List.of());
			builder.pop();
		}

		private ForgeConfigSpec.ConfigValue<List<? extends String>> defineStringList(ForgeConfigSpec.Builder builder, String key, List<String> defaults) {
			return builder.defineListAllowEmpty(key, defaults, o -> o instanceof String);
		}
	}

	public static class Client {
		public final ForgeConfigSpec.BooleanValue rememberSearchPhrase;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.comment("Client settings").push("client");
			rememberSearchPhrase = builder.comment("Whether search phrase is remembered and shared between Sophisticated and non-Sophisticated container screens")
					.define("rememberSearchPhrase", true);
			builder.pop();
		}
	}
}
