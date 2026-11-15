package net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets.Anchor;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.layout.ScreenLayoutOffsets.Offset;

import javax.annotation.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class LayoutAdjustmentEditor {
	private static final int TOOLBAR_Y = 18;
	private static final int TOOLBAR_HEIGHT = 18;
	private static final int TOOLBAR_BUTTON_WIDTH = 112;
	private boolean pending;
	@Nullable
	private Screen activeScreen;
	private List<Group> groups = List.of();
	private final Map<Anchor, List<? extends String>> originalEntries = new EnumMap<>(Anchor.class);
	@Nullable
	private Anchor dragging;
	private double dragStartX;
	private double dragStartY;
	private Offset dragStartOffset = Offset.ZERO;

	public void arm() {
		pending = true;
	}

	public void cancelPending() {
		pending = false;
	}

	public boolean isActive(Screen screen) {
		return activeScreen == screen;
	}

	public void render(Screen screen, List<Group> currentGroups, GuiGraphics graphics, int mouseX, int mouseY) {
		if (activeScreen == null && pending && !currentGroups.isEmpty()) {
			pending = false;
			activeScreen = screen;
			for (Anchor anchor : Anchor.values()) {
				originalEntries.put(anchor, List.copyOf(anchor.config().get()));
			}
		}
		if (!isActive(screen)) {
			return;
		}
		groups = currentGroups;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.drawCenteredString(Minecraft.getInstance().font, text("instructions"), screen.width / 2, 5, 0xFFFFFFFF);
		for (int index = 0; index < 3; index++) {
			Bounds bounds = toolbarBounds(screen, index);
			graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), bounds.contains(mouseX, mouseY) ? 0xEE666666 : 0xEE333333);
			graphics.drawCenteredString(Minecraft.getInstance().font, text(switch (index) {
				case 0 -> "reset_container";
				case 1 -> "reset_player";
				default -> "cancel";
			}), bounds.x() + bounds.width() / 2, bounds.y() + 5, 0xFFFFFFFF);
		}
		for (Group group : groups) {
			Bounds bounds = group.bounds().get();
			int color = group.anchor() == Anchor.CONTAINER ? 0xFF55FFFF : 0xFFFFFF55;
			graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), color);
			Offset offset = ScreenLayoutOffsets.get(screen.getClass().getName(), group.anchor());
			graphics.drawString(Minecraft.getInstance().font, text(group.anchor().commandName()).append(" (" + offset.x() + ", " + offset.y() + ")"),
					bounds.x(), bounds.y() - 10, color);
		}
		graphics.pose().popPose();
	}

	public boolean mousePressed(Screen screen, double x, double y, int button) {
		if (!isActive(screen)) {
			return false;
		}
		if (button != 0) {
			return true;
		}
		for (int index = 0; index < 3; index++) {
			if (toolbarBounds(screen, index).contains(x, y)) {
				if (index == 2) {
					cancelChanges(screen);
				} else {
					setOffset(screen, index == 0 ? Anchor.CONTAINER : Anchor.PLAYER, Offset.ZERO);
				}
				return true;
			}
		}
		for (int index = groups.size() - 1; index >= 0; index--) {
			Group group = groups.get(index);
			if (group.bounds().get().contains(x, y)) {
				dragging = group.anchor();
				dragStartX = x;
				dragStartY = y;
				dragStartOffset = ScreenLayoutOffsets.get(screen.getClass().getName(), dragging);
				break;
			}
		}
		return true;
	}

	public boolean mouseDragged(Screen screen, double x, double y, int button) {
		if (!isActive(screen)) {
			return false;
		}
		if (dragging != null && button == 0) {
			setOffset(screen, dragging, dragStartOffset.plus((int) Math.round(x - dragStartX), (int) Math.round(y - dragStartY)));
		}
		return true;
	}

	public boolean mouseReleased(Screen screen) {
		if (!isActive(screen)) {
			return false;
		}
		dragging = null;
		return true;
	}

	private void setOffset(Screen screen, Anchor anchor, Offset offset) {
		Offset previous = ScreenLayoutOffsets.get(screen.getClass().getName(), anchor);
		ScreenLayoutOffsets.set(screen.getClass().getName(), anchor, offset);
		groups.stream().filter(group -> group.anchor() == anchor).forEach(group -> group.move().accept(offset.x() - previous.x(), offset.y() - previous.y()));
	}

	private void cancelChanges(Screen screen) {
		for (Anchor anchor : Anchor.values()) {
			setOffset(screen, anchor, ScreenLayoutOffsets.parse(screen.getClass().getName(), originalEntries.get(anchor)));
			anchor.config().set(originalEntries.get(anchor));
		}
		finish();
	}

	public void onClosing(Screen screen) {
		if (isActive(screen)) {
			ScreenLayoutOffsets.save();
			finish();
		}
	}

	private void finish() {
		activeScreen = null;
		dragging = null;
		groups = List.of();
		originalEntries.clear();
	}

	private Bounds toolbarBounds(Screen screen, int index) {
		int width = Math.min(TOOLBAR_BUTTON_WIDTH, (screen.width - 8) / 3);
		return new Bounds((screen.width - width * 3) / 2 + width * index, TOOLBAR_Y, width - 2, TOOLBAR_HEIGHT);
	}

	private MutableComponent text(String key) {
		return Component.translatable("gui.sophisticatedinventoryinteractions.layout." + key);
	}

	public record Group(Anchor anchor, Supplier<Bounds> bounds, BiConsumer<Integer, Integer> move) {
	}

	public record Bounds(int x, int y, int width, int height) {
		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}

		public boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
		}
	}
}
