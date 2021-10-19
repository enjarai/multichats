package nl.enjarai.multichats;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import nl.enjarai.multichats.types.Group;
import nl.enjarai.multichats.types.GroupPermissionLevel;

import java.util.HashMap;

public class Helpers {
    public static void sendToChat(Group group, ServerPlayerEntity sendingPlayer, String message) {
        Text chatFormat = TextParser.parse(group.getFormat());
        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("player", sendingPlayer.getDisplayName());
        placeholders.put("group", group.displayNameShort);
        placeholders.put("message", new LiteralText(message));
        placeholders.put("prefix", new LiteralText(group.prefix));

        Text output = PlaceholderAPI.parsePredefinedText(
                chatFormat,
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        );
        MultiChats.LOGGER.info(output.getString());

        MultiChats.SERVER.getPlayerManager().getPlayerList().forEach(player -> {
            if (group.checkAccess(player.getUuid()) || Permissions.check(player, "multichats.receive_all")) {
                player.sendMessage(output, false);
            }
        });
    }

    public static GroupPermissionLevel getPermissionLevelFromInt(int permissionInt) {
        return switch (permissionInt) {
            case 0 -> GroupPermissionLevel.MEMBER;
            case 1 -> GroupPermissionLevel.MANAGER;
            case 2 -> GroupPermissionLevel.OWNER;
            default -> null;
        };
    }
}
