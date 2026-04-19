package net.p3pp3rf1y.sophisticatedinventoryinteractions;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.common.CommonEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SophisticatedInventoryInteractions.MOD_ID)
public class SophisticatedInventoryInteractions {
	public static final String MOD_ID = "sophisticatedinventoryinteractions";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public SophisticatedInventoryInteractions(IEventBus modBus, Dist dist, ModContainer container) {
		container.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		if (dist == Dist.CLIENT && !ModList.get().isLoaded("configured")) {
			container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		}
		CommonEventHandler.registerHandlers(modBus);
		if (dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers();
		}
	}

	public static ResourceLocation getRL(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
