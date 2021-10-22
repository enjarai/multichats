package nl.enjarai.multichats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting() // Makes the json use new lines instead of being a "one-liner"
            .serializeNulls() // Makes fields with `null` value to be written as well.
            .disableHtmlEscaping() // We'll be able to use custom chars without them being saved differently
            .create();

    // Config values
    public String chatFormat = "<dark_aqua><hover:'Click to send message to this chat'><cmd:${prefix}>[${group}]</cmd></hover></dark_aqua> ${player} <dark_gray>» <white>${message}";
//    public String globalPrefix = "$";
    public String defaultGroupName = "<dark_gray>Wanderer";

    public Messages messages = new Messages();
    public static class Messages {
        // Errors
        public String unknownError = "<red>By some wizardry you've found an error message that shouldn't be seen!\nplease contact enjarai on the disc because this is a bug";
        public String noPlayerError = "<red>This command should be executed by a player";
        public String noPermissionChatError = "<red>You dont have permission to access that chat";
        public String noPermissionError = "<red>You dont have permission to do that";
        public String noGroupError = "<red>This alliance does not exist";
        public String noInvitesError = "<red>You have no open invites";
        public String notInGroupError = "<red>You arent part of that alliance";
        public String inGroupError = "<red>That player is already part of that alliance";
        public String cantFindPlayerError = "<red>Couldn't find that player";
        public String existsError = "<red>This alliance already exists";
        public String groupOwnerCantLeaveError = "<red>The alliance owner cant leave the alliance";
        public String cantOwnTwoGroupsError = "<red>A player can only own one alliance";
        public String playerNotInGroupError = "<red>That player is not a part of this alliance";
        public String alreadyManagerError = "<red>That player is already a manager";
        public String notManagerError = "<red>That player is not a manager";
        public String prefixTooLongError = "<red>That prefix is invalid";
        // General messages
        public String switched = "<dark_aqua>Switched chat to <yellow>${group}</yellow>";
        public String switchedPrefix = "<dark_aqua>You can also prefix your message with \"<yellow>${prefix}</yellow>\" to send it to this chat";
        public String switchedGlobal = "<dark_aqua>Switched to global chat";
        public String groupCreated = "<dark_aqua>Successfully created the alliance <yellow>${group}</yellow>!\nAdd members with <white>/alliance invite</white> and \nchange settings with <white>/alliance modify</white>.";
        public String groupDeleted = "<dark_aqua>The alliance <yellow>${group}</yellow> was deleted.";
        public String groupLeft = "<dark_aqua>You left <yellow>${group}</yellow>.";
        public String groupSetToPrimary = "<dark_aqua>Set <yellow>${group}</yellow> as your primary alliance.";
        public String groupPrimaryReset = "<dark_aqua>Reset your primary alliance.";
        public String sentInvite = "<dark_aqua><yellow>${playerTo}</yellow> was invited to <yellow>${group}</yellow>,\nthey can accept with <white>/alliance accept</white>";
        public String receivedInvite = "<dark_aqua>You received an invite from <yellow>${playerFrom}</yellow> to join <yellow>${group}</yellow>\n<green><hover:Click here to accept><run_cmd:'/alliance accept'>[Accept]</run_cmd></hover></green> <red><hover:Click here to deny><run_cmd:'/alliance deny'>[Deny]</run_cmd></hover></red>";
        public String inviteAcceptedFrom = "<dark_aqua><yellow>${playerTo}</yellow> has <green>accepted</green> your invite!";
        public String inviteDeniedFrom = "<dark_aqua><yellow>${playerTo}</yellow> has <red>rejected</red> your invite.";
        public String inviteAcceptedTo = "<dark_aqua>You <green>accepted</green> <yellow>${playerFrom}</yellow>'s invite!";
        public String inviteDeniedTo = "<dark_aqua>You <red>rejected</red> <yellow>${playerFrom}</yellow>'s invite.";
        public String modifyOwner = "<dark_aqua>Transferred ownership of <yellow>${group}</yellow> to <yellow>${player}</yellow>.";
        public String modifyAddManager = "<dark_aqua>Made <yellow>${player}</yellow> a manager of <yellow>${group}</yellow>.";
        public String modifyRemoveManager = "<dark_aqua>Made <yellow>${player}</yellow> no longer a manager of <yellow>${group}</yellow>.";
        public String modifyPrefix = "<dark_aqua>Set the prefix of <yellow>${group}</yellow> to <yellow>${string}</yellow>.";
        public String modifyDisplayName = "<dark_aqua>Set the display name of <yellow>${group}</yellow> to <yellow>${string}</yellow>.";
        public String modifyDisplayNameShort = "<dark_aqua>Set the short display name of <yellow>${group}</yellow> to <yellow>${string}</yellow>.";
        public String resetPrefix = "<dark_aqua>The prefix of <yellow>${group}</yellow> has been reset.";
        public String groupMemberList = "<dark_aqua><yellow>${group}</yellow> has the following members:";
        public String groupMemberListEntry = "<aqua>  ${permissionLevel}: <yellow>${player}</yellow>";
        public String groupMemberKicked = "<dark_aqua><yellow>${player}</yellow> was kicked from <yellow>${group}</yellow>.";
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
