package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.Config;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.LayoutAdjustmentEditor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets.Anchor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets.Offset;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility.EligibilityDecision;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ClientScreenCommands {
	private final ClientScreenHistory screenHistory;
	private final LayoutAdjustmentEditor layoutEditor;

	public ClientScreenCommands(ClientScreenHistory screenHistory, LayoutAdjustmentEditor layoutEditor) {
		this.screenHistory = screenHistory;
		this.layoutEditor = layoutEditor;
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> screens = Commands.literal("screens")
				.then(Commands.literal("recent").executes(context -> showRecent(context, ClientScreenHistory.MAX_ENTRIES))
						.then(Commands.argument("count", IntegerArgumentType.integer(1, ClientScreenHistory.MAX_ENTRIES))
								.executes(context -> showRecent(context, IntegerArgumentType.getInteger(context, "count")))))
				.then(Commands.literal("rules").executes(this::showRules));
		for (RuleAction action : RuleAction.values()) {
			String name = action.name().toLowerCase(Locale.ROOT);
			screens.then(Commands.literal(name).then(withScopes(
					Commands.argument("screen-class", StringArgumentType.string()).suggests(this::suggestScreenClasses), this::explicitScreen, action)));
			screens.then(withScopes(Commands.literal(name + "-last"), this::lastScreen, action));
		}
		dispatcher.register(Commands.literal("sii").then(screens).then(layoutCommands()));
	}

	private <T extends ArgumentBuilder<CommandSourceStack, T>> T withScopes(T node, Function<CommandContext<CommandSourceStack>, String> screen,
			RuleAction action) {
		node.executes(context -> updateRule(context, screen.apply(context), action, List.of(), true));
		for (ScreenControlScope scope : ScreenControlScope.values()) {
			node.then(Commands.literal(scope.commandName()).executes(context -> updateRule(context, screen.apply(context), action, List.of(scope), false)));
		}
		return node.then(
				Commands.literal("all").executes(context -> updateRule(context, screen.apply(context), action, List.of(ScreenControlScope.values()), false)));
	}

	private LiteralArgumentBuilder<CommandSourceStack> layoutCommands() {
		LiteralArgumentBuilder<CommandSourceStack> layout = Commands.literal("layout").then(Commands.literal("adjust-next").executes(context -> {
			layoutEditor.arm();
			return message(context, "Layout adjustment armed. Open a supported inventory, drag the outlined groups, then close to save.");
		})).then(Commands.literal("cancel").executes(context -> {
			layoutEditor.cancelPending();
			return message(context, "Pending layout adjustment canceled.");
		}));
		layout.then(Commands.literal("offset").then(
				offsetAnchors(Commands.argument("screen-class", StringArgumentType.string()).suggests(this::suggestScreenClasses), this::explicitScreen)));
		layout.then(offsetAnchors(Commands.literal("offset-last"), this::lastScreen));
		for (boolean reset : List.of(false, true)) {
			String name = reset ? "reset" : "show";
			layout.then(Commands.literal(name).then(layoutAnchors(
					Commands.argument("screen-class", StringArgumentType.string()).suggests(this::suggestScreenClasses), this::explicitScreen, reset)));
			layout.then(layoutAnchors(Commands.literal(name + "-last"), this::lastScreen, reset));
		}
		return layout;
	}

	private <T extends ArgumentBuilder<CommandSourceStack, T>> T offsetAnchors(T node, Function<CommandContext<CommandSourceStack>, String> screen) {
		for (Anchor anchor : Anchor.values()) {
			node.then(Commands.literal(anchor.commandName()).then(Commands.argument("x", IntegerArgumentType.integer(-10000, 10000))
					.then(Commands.argument("y", IntegerArgumentType.integer(-10000, 10000)).executes(context -> {
						String className = screen.apply(context);
						if (className == null) {
							return 0;
						}
						ScreenLayoutOffsets.set(className, anchor,
								new Offset(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "y")));
						ScreenLayoutOffsets.save();
						return showLayout(context, className, List.of(anchor), false);
					}))));
		}
		return node;
	}

	private <T extends ArgumentBuilder<CommandSourceStack, T>> T layoutAnchors(T node, Function<CommandContext<CommandSourceStack>, String> screen,
			boolean reset) {
		node.executes(context -> showLayout(context, screen.apply(context), List.of(Anchor.values()), reset));
		for (Anchor anchor : Anchor.values()) {
			node.then(Commands.literal(anchor.commandName()).executes(context -> showLayout(context, screen.apply(context), List.of(anchor), reset)));
		}
		return node;
	}

	private int showLayout(CommandContext<CommandSourceStack> context, @Nullable String className, List<Anchor> anchors, boolean reset) {
		if (className == null) {
			return 0;
		}
		for (Anchor anchor : anchors) {
			if (reset) {
				ScreenLayoutOffsets.set(className, anchor, Offset.ZERO);
			}
			Offset offset = ScreenLayoutOffsets.get(className, anchor);
			message(context, className + " | " + anchor.commandName() + " = " + offset.x() + "," + offset.y());
		}
		if (reset) {
			ScreenLayoutOffsets.save();
		}
		return 1;
	}

	private int showRecent(CommandContext<CommandSourceStack> context, int count) {
		List<ClientScreenHistory.RecentScreen> recent = screenHistory.recent(count);
		if (recent.isEmpty()) {
			return message(context, "No container screens have been opened this session.");
		}
		for (ClientScreenHistory.RecentScreen screen : recent) {
			message(context, screen.screenClassName() + " | " + screen.title() + " | " + screen.menuClassName() + " | container: "
					+ decision(screen.container()) + " | transfers: " + decision(screen.transfers()) + " | player-sort: " + decision(screen.playerSort()));
		}
		return recent.size();
	}

	private String decision(EligibilityDecision decision) {
		return (decision.eligible() ? "allowed" : "blocked") + " (" + decision.reason() + ")";
	}

	@Nullable
	private String lastScreen(CommandContext<CommandSourceStack> context) {
		if (screenHistory.last().isEmpty()) {
			message(context, "No container screen is available in this session history.");
			return null;
		}
		return screenHistory.last().orElseThrow().screenClassName();
	}

	private String explicitScreen(CommandContext<CommandSourceStack> context) {
		return StringArgumentType.getString(context, "screen-class");
	}

	private int updateRule(CommandContext<CommandSourceStack> context, @Nullable String className, RuleAction action, List<ScreenControlScope> scopes,
			boolean legacy) {
		if (className == null) {
			return 0;
		}
		if (legacy) {
			changeRules(Config.CLIENT.includeScreenClasses, Config.CLIENT.excludeScreenClasses, className, action);
			for (ScreenControlScope scope : List.of(ScreenControlScope.CONTAINER, ScreenControlScope.TRANSFERS)) {
				changeRules(scope.included(), scope.excluded(), className, RuleAction.RESET);
			}
		} else {
			for (ScreenControlScope scope : scopes) {
				changeRules(scope.included(), scope.excluded(), className, action);
			}
		}
		String groups = legacy ? "container + transfers" : String.join(", ", scopes.stream().map(ScreenControlScope::commandName).toList());
		return message(context, action + " " + className + " [" + groups + "]. Changes apply when the next container opens.");
	}

	private void changeRules(ModConfigSpec.ConfigValue<List<? extends String>> include, ModConfigSpec.ConfigValue<List<? extends String>> exclude,
			String className, RuleAction action) {
		List<String> included = new ArrayList<>(new LinkedHashSet<>(include.get()));
		List<String> excluded = new ArrayList<>(new LinkedHashSet<>(exclude.get()));
		included.removeIf(rule -> rule.trim().equals(className));
		excluded.removeIf(rule -> rule.trim().equals(className));
		if (action == RuleAction.ALLOW) {
			included.add(className);
		} else if (action == RuleAction.BLOCK) {
			excluded.add(className);
		}
		include.set(List.copyOf(included));
		exclude.set(List.copyOf(excluded));
		include.save();
		exclude.save();
	}

	private int showRules(CommandContext<CommandSourceStack> context) {
		message(context,
				"Legacy container + transfers: allow " + Config.CLIENT.includeScreenClasses.get() + "; block " + Config.CLIENT.excludeScreenClasses.get());
		for (ScreenControlScope scope : ScreenControlScope.values()) {
			message(context, scope.commandName() + ": allow " + scope.included().get() + "; block " + scope.excluded().get());
		}
		return 1;
	}

	private CompletableFuture<Suggestions> suggestScreenClasses(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		String remaining = builder.getRemainingLowerCase();
		if (remaining.startsWith("\"")) {
			remaining = remaining.substring(1);
		}
		StringRange range = StringRange.between(builder.getStart(), builder.getInput().length());
		List<Suggestion> suggestions = new ArrayList<>();
		for (String className : screenClassNames()) {
			if (className.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				suggestions.add(new Suggestion(range, StringArgumentType.escapeIfRequired(className)));
			}
		}
		return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
	}

	private List<String> screenClassNames() {
		LinkedHashSet<String> names = new LinkedHashSet<>();
		for (ClientScreenHistory.RecentScreen screen : screenHistory.recent(ClientScreenHistory.MAX_ENTRIES)) {
			names.add(screen.screenClassName());
		}
		names.addAll(Config.CLIENT.includeScreenClasses.get());
		names.addAll(Config.CLIENT.excludeScreenClasses.get());
		for (ScreenControlScope scope : ScreenControlScope.values()) {
			names.addAll(scope.included().get());
			names.addAll(scope.excluded().get());
		}
		for (Anchor anchor : Anchor.values()) {
			anchor.config().get().forEach(entry -> names.add(entry.split("=", 2)[0].trim()));
		}
		Config.CLIENT.playerSortOffsetOverrides.get().forEach(entry -> names.add(entry.split("=", 2)[0].trim()));
		return names.stream().map(String::trim).filter(name -> !name.isEmpty() && !name.contains("*")).distinct().toList();
	}

	private int message(CommandContext<CommandSourceStack> context, String text) {
		context.getSource().sendSystemMessage(Component.literal(text));
		return 1;
	}

	private enum RuleAction {
		ALLOW, BLOCK, RESET
	}
}
