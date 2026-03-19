package net.p3pp3rf1y.sophisticatedinventoryinteractions;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.client.ClientEventHandler;
import net.p3pp3rf1y.sophisticatedinventoryinteractions.network.InventoryInteractionsPacketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SophisticatedInventoryInteractions.MOD_ID)
public class SophisticatedInventoryInteractions {
	public static final String MOD_ID = "sophisticatedinventoryinteractions";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public SophisticatedInventoryInteractions() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ClientEventHandler.registerHandlers();
		}
		modBus.addListener(SophisticatedInventoryInteractions::setup);
	}

	private static void setup(FMLCommonSetupEvent event) {
		InventoryInteractionsPacketHandler.INSTANCE.init();
	}

	public static ResourceLocation getRL(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
