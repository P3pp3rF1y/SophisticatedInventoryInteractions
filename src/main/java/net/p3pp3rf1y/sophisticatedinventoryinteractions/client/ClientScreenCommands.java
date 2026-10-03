package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ClientScreenCommands {
	private final ClientScreenHistory screenHistory;

	public ClientScreenCommands(ClientScreenHistory screenHistory) {
		this.screenHistory = screenHistory;
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> screens = Commands.literal("screens")
				.then(Commands.literal("recent").executes(context -> showRecent(context, ClientScreenHistory.MAX_ENTRIES))
						.then(Commands.argument("count", IntegerArgumentType.integer(1, ClientScreenHistory.MAX_ENTRIES))
								.executes(context -> showRecent(context, IntegerArgumentType.getInteger(context, "count")))))
				.then(Commands.literal("allow-last").executes(context -> updateLast(context, RuleAction.ALLOW)))
				.then(Commands.literal("block-last").executes(context -> updateLast(context, RuleAction.BLOCK)))
				.then(Commands.literal("reset-last").executes(context -> updateLast(context, RuleAction.RESET))).then(ruleCommand("allow", RuleAction.ALLOW))
				.then(ruleCommand("block", RuleAction.BLOCK)).then(ruleCommand("reset", RuleAction.RESET))
				.then(Commands.literal("rules").executes(this::showRules));
		dispatcher.register(Commands.literal("sii").then(screens));
	}

	private LiteralArgumentBuilder<CommandSourceStack> ruleCommand(String name, RuleAction action) {
		return Commands.literal(name).then(Commands.argument("screen-class", StringArgumentType.word()).suggests(this::suggestScreenClasses)
				.executes(context -> updateRule(context, StringArgumentType.getString(context, "screen-class"), action)));
	}

	private int showRecent(CommandContext<CommandSourceStack> context, int count) {
		List<ClientScreenHistory.RecentScreen> recentScreens = screenHistory.recent(count);
		if (recentScreens.isEmpty()) {
			context.getSource().sendSystemMessage(Component.literal("No container screens have been opened this session."));
			return 0;
		}
		for (ClientScreenHistory.RecentScreen recentScreen : recentScreens) {
			String decision = recentScreen.eligible() ? "allowed" : "blocked";
			context.getSource().sendSystemMessage(Component.literal(decision + " (" + recentScreen.decisionReason() + "): " + recentScreen.screenClassName()
					+ " | " + recentScreen.menuClassName() + " | " + recentScreen.title()));
		}
		return recentScreens.size();
	}

	private int updateLast(CommandContext<CommandSourceStack> context, RuleAction action) {
		Optional<ClientScreenHistory.RecentScreen> lastScreen = screenHistory.last();
		if (lastScreen.isEmpty()) {
			context.getSource().sendSystemMessage(Component.literal("No container screen is available in this session history."));
			return 0;
		}
		return updateRule(context, lastScreen.get().screenClassName(), action);
	}

	private int updateRule(CommandContext<CommandSourceStack> context, String screenClassName, RuleAction action) {
		List<String> included = mutableRules(Config.CLIENT.includeScreenClasses.get());
		List<String> excluded = mutableRules(Config.CLIENT.excludeScreenClasses.get());
		included.remove(screenClassName);
		excluded.remove(screenClassName);
		if (action == RuleAction.ALLOW) {
			included.add(screenClassName);
		} else if (action == RuleAction.BLOCK) {
			excluded.add(screenClassName);
		}
		Config.CLIENT.includeScreenClasses.set(List.copyOf(included));
		Config.CLIENT.excludeScreenClasses.set(List.copyOf(excluded));
		Config.CLIENT.includeScreenClasses.save();
		Config.CLIENT.excludeScreenClasses.save();
		context.getSource().sendSystemMessage(Component.literal(action.description + " " + screenClassName + ". Changes apply when the next container opens."));
		return 1;
	}

	private int showRules(CommandContext<CommandSourceStack> context) {
		List<? extends String> included = Config.CLIENT.includeScreenClasses.get();
		List<? extends String> excluded = Config.CLIENT.excludeScreenClasses.get();
		context.getSource().sendSystemMessage(Component.literal("Allowed screens: " + formatRules(included)));
		context.getSource().sendSystemMessage(Component.literal("Blocked screens: " + formatRules(excluded)));
		return included.size() + excluded.size();
	}

	private CompletableFuture<Suggestions> suggestScreenClasses(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		for (String screenClassName : screenClassNames()) {
			if (screenClassName.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
				builder.suggest(screenClassName);
			}
		}
		return builder.buildFuture();
	}

	private List<String> screenClassNames() {
		LinkedHashSet<String> screenClassNames = new LinkedHashSet<>();
		for (ClientScreenHistory.RecentScreen recentScreen : screenHistory.recent(ClientScreenHistory.MAX_ENTRIES)) {
			screenClassNames.add(recentScreen.screenClassName());
		}
		screenClassNames.addAll(Config.CLIENT.includeScreenClasses.get());
		screenClassNames.addAll(Config.CLIENT.excludeScreenClasses.get());
		return List.copyOf(screenClassNames);
	}

	private List<String> mutableRules(List<? extends String> rules) {
		return new ArrayList<>(new LinkedHashSet<>(rules));
	}

	private String formatRules(List<? extends String> rules) {
		return rules.isEmpty() ? "(none)" : String.join(", ", rules);
	}

	private enum RuleAction {
		ALLOW("Allowed"), BLOCK("Blocked"), RESET("Reset");

		private final String description;

		RuleAction(String description) {
			this.description = description;
		}
	}
}
