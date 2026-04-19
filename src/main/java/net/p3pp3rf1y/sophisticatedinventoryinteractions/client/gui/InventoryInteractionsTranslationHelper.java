package net.p3pp3rf1y.sophisticatedinventoryinteractions.client.gui;

import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.SophisticatedInventoryInteractions;

public class InventoryInteractionsTranslationHelper extends TranslationHelper {
	public static final InventoryInteractionsTranslationHelper INSTANCE = new InventoryInteractionsTranslationHelper();
	private static final String CONFIG_PREFIX = SophisticatedInventoryInteractions.MOD_ID + ".configuration.";

	private InventoryInteractionsTranslationHelper() {
		super(SophisticatedInventoryInteractions.MOD_ID);
	}

	public String translConfig(String configKey) {
		return CONFIG_PREFIX + configKey;
	}
}
