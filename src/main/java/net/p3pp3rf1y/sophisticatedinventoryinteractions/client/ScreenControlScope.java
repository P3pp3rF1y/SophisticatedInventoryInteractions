package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import net.minecraftforge.common.ForgeConfigSpec;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;

import java.util.List;

public enum ScreenControlScope {
	CONTAINER("container"), TRANSFERS("transfers"), PLAYER_SORT("player-sort");

	private final String commandName;

	ScreenControlScope(String commandName) {
		this.commandName = commandName;
	}

	public String commandName() {
		return commandName;
	}

	public ForgeConfigSpec.ConfigValue<List<? extends String>> included() {
		return switch (this) {
			case CONTAINER -> Config.CLIENT.includeContainerScreenClasses;
			case TRANSFERS -> Config.CLIENT.includeTransferScreenClasses;
			case PLAYER_SORT -> Config.CLIENT.includePlayerSortScreenClasses;
		};
	}

	public ForgeConfigSpec.ConfigValue<List<? extends String>> excluded() {
		return switch (this) {
			case CONTAINER -> Config.CLIENT.excludeContainerScreenClasses;
			case TRANSFERS -> Config.CLIENT.excludeTransferScreenClasses;
			case PLAYER_SORT -> Config.CLIENT.excludePlayerSortScreenClasses;
		};
	}
}
