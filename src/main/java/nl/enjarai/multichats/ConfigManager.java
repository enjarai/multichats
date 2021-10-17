package nl.enjarai.multichats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting() // Makes the json use new lines instead of being a "one-liner"
            .serializeNulls() // Makes fields with `null` value to be written as well.
            .disableHtmlEscaping() // We'll be able to use custom chars without them being saved differently
            .create();

    // Config values
    public String chatFormat = "<yellow><hover:'Click to send message to this chat'><cmd:${prefix}>[${chat}]</cmd></hover></yellow> ${player} <dark_gray>» <white>${message}";

    public Messages messages = new Messages();
    public static class Messages {
        public String noPlayerError = "<red>This command should be executed by a player";
        public String noPermissionError = "<red>You dont have permission to access that chat";
        public String noChatError = "<red>This chat does not exist";
        public String switched = "<yellow>Switched chat to ${chat}\nYou can also prefix your message with \"${prefix}\" to send it to this chat</yellow>";
        public String switchedGlobal = "<yellow>Switched to global chat";
    }

    public HashMap<String, Chat> chats = new HashMap<>() {{
        put("staff", new Chat("<dark_red><bold>Staff Chat", "<dark_red><bold>SC", "#", null));
    }};
    public static class Chat {
        public String displayName;
        public String displayNameShort;
        public String prefix;
        public String formatOverride;

        public Chat(String displayName, String displayNameShort, String prefix, String formatOverride) {
            this.displayName = displayName;
            this.displayNameShort = displayNameShort;
            this.prefix = prefix;
            this.formatOverride = formatOverride;
        }

        public String getFormat() {
            return formatOverride == null ? MultiChats.CONFIG.chatFormat : formatOverride;
        }

        public String getChatName() {
            // Couldn't find a better way to get the simple name for a Chat object
            for(Map.Entry<String, Chat> entry : MultiChats.CONFIG.chats.entrySet()) {
                if(entry.getValue() == this) {
                    return entry.getKey();
                }
            }
            return null;
        }
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
