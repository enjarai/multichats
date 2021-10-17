package nl.enjarai.multichats;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class MultiChats implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LogManager.getLogger("MultiChats");
	public static MinecraftServer SERVER;
	public static final File CONFIG_FILE = new File("config/multichats.json");
	public static ConfigManager CONFIG = ConfigManager.loadConfigFile(CONFIG_FILE);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerLifecycleEvents.SERVER_STARTING.register(MultiChats::onServerStarting);

		Commands.register();

		LOGGER.info("Whomst pingeth?");
	}

	private static void onServerStarting(MinecraftServer server) {
		SERVER = server;
	}
}
