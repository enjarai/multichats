package nl.enjarai.multichats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nl.enjarai.multichats.types.Group;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting() // Makes the json use new lines instead of being a "one-liner"
            .serializeNulls() // Makes fields with `null` value to be written as well.
            .disableHtmlEscaping() // We'll be able to use custom chars without them being saved differently
            .create();

    // Config values
    public String chatFormat = "<dark_aqua><hover:'Click to send message to this chat'><cmd:${prefix}>[${group}]</cmd></hover></dark_aqua> ${player} <dark_gray>» <white>${message}";
//    public String globalPrefix = "$";

    public Messages messages = new Messages();
    public static class Messages {
        public String unknownError = "<red>By some wizardry you've found an error message that shouldn't be seen!\nplease contact enjarai on the disc because this is a bug";
        public String noPlayerError = "<red>This command should be executed by a player";
        public String noPermissionError = "<red>You dont have permission to access that chat";
        public String noGroupError = "<red>This alliance does not exist";
        public String existsError = "<red>This alliance already exists";
        public String switched = "<dark_aqua>Switched chat to ${group}\nYou can also prefix your message with \"${prefix}\" to send it to this chat";
        public String switchedGlobal = "<dark_aqua>Switched to global chat";
        public String groupCreated = "<dark_aqua>Successfully created the alliance ${group}!\nAdd members with <white>/alliance invite</white> and \nchange settings with <white>/alliance modify</white>.";
    }

    // Reading and saving

    /**
     * Loads config file.
     *
     * @param file file to load the config file from.
     * @return ConfigManager object
     */
    public static ConfigManager loadConfigFile(File file) {
        ConfigManager config = null;

        if (file.exists()) {
            // An existing config is present, we should use its values
            try (BufferedReader fileReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            )) {
                // Parses the config file and puts the values into config object
                config = gson.fromJson(fileReader, ConfigManager.class);
            } catch (IOException e) {
                throw new RuntimeException("[MultiChat] Problem occurred when trying to load config: ", e);
            }
        }
        // gson.fromJson() can return null if file is empty
        if (config == null) {
            config = new ConfigManager();
        }

        // Saves the file in order to write new fields if they were added
        config.saveConfigFile(file);
        return config;
    }

    /**
     * Saves the config to the given file.
     *
     * @param file file to save config to
     */
    public void saveConfigFile(File file) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
