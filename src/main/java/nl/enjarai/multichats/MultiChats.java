package nl.enjarai.multichats;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.PlaceholderResult;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import nl.enjarai.multichats.commands.Commands;
import nl.enjarai.multichats.commands.InviteManager;
import nl.enjarai.multichats.database.DatabaseHandlerInterface;
import nl.enjarai.multichats.database.SQLiteDatabase;
import nl.enjarai.multichats.types.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.SQLException;

public class MultiChats implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LogManager.getLogger("MultiChats");
	public static MinecraftServer SERVER;

	public static final String VERSION = "2.1.1";
	public static final File CONFIG_FILE = new File("config/multichats.json");
	public static final String DATABASE_FILE ="config/multichats.db";

	public static ConfigManager CONFIG = ConfigManager.loadConfigFile(CONFIG_FILE);
	public static DatabaseHandlerInterface DATABASE;

	public static InviteManager INVITE_MANAGER = new InviteManager();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		ServerLifecycleEvents.SERVER_STARTING.register(MultiChats::onServerStarting);

		Commands.register();

		Placeholders.register();

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			if (DATABASE != null) {
				DATABASE.closeConnection();
			}
			SERVER = null;
			DATABASE = null;
		});

		LOGGER.info("Whomst pingeth?");
	}

	private static void onServerStarting(MinecraftServer server) {
		SERVER = server;

		try {
			DATABASE = new SQLiteDatabase(DATABASE_FILE);
			LOGGER.info("MultiChats database loaded.");
		} catch (SQLException e) {
			e.printStackTrace();

			LOGGER.error("Couldn't connect to database! Stopping server...");
			server.stop(false);
		}
	}
}
