package net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout;

import net.minecraftforge.common.ForgeConfigSpec;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;

import java.util.ArrayList;
import java.util.List;

public final class ScreenLayoutOffsets {
	private ScreenLayoutOffsets() {
	}

	public enum Anchor {
		CONTAINER("container"), PLAYER("player");

		private final String commandName;

		Anchor(String commandName) {
			this.commandName = commandName;
		}

		public String commandName() {
			return commandName;
		}

		public ForgeConfigSpec.ConfigValue<List<? extends String>> config() {
			return this == CONTAINER ? Config.CLIENT.anchorOffsetOverrides : Config.CLIENT.playerOffsetOverrides;
		}
	}

	public record Offset(int x, int y) {
		public static final Offset ZERO = new Offset(0, 0);

		public Offset plus(int dx, int dy) {
			return new Offset(x + dx, y + dy);
		}
	}

	public static Offset get(String screenClass, Anchor anchor) {
		return parse(screenClass, anchor.config().get());
	}

	public static Offset parse(String screenClass, List<? extends String> entries) {
		for (String entry : entries) {
			String[] parts = entry.split("=", 2);
			if (parts.length != 2 || !screenClass.equals(parts[0].trim())) {
				continue;
			}
			String[] xy = parts[1].split(",");
			if (xy.length == 2) {
				try {
					return new Offset(Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim()));
				} catch (NumberFormatException ignored) {
					return Offset.ZERO;
				}
			}
		}
		return Offset.ZERO;
	}

	public static void set(String screenClass, Anchor anchor, Offset offset) {
		List<String> entries = new ArrayList<>(anchor.config().get());
		entries.removeIf(entry -> entry.split("=", 2)[0].trim().equals(screenClass));
		entries.add(screenClass + "=" + offset.x() + "," + offset.y());
		anchor.config().set(List.copyOf(entries));
	}

	public static void save() {
		for (Anchor anchor : Anchor.values()) {
			anchor.config().save();
		}
	}
}
