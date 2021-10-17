package nl.enjarai.multichats;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.regex.Pattern;

public class ChatHelpers {
    public static void sendToChat(ConfigManager.Chat chat, ServerPlayerEntity sendingPlayer, String message) {
        Text chatFormat = TextParser.parse(chat.getFormat());
        HashMap<String, Text> placeholders = new HashMap<>();
        String chatName = chat.getChatName();

        placeholders.put("player", sendingPlayer.getDisplayName());
        placeholders.put("chat", TextParser.parse(chat.displayNameShort));
        placeholders.put("message", new LiteralText(message));
        placeholders.put("prefix", new LiteralText(chat.prefix));

        MultiChats.SERVER.getPlayerManager().getPlayerList().forEach(player -> {
            if (Permissions.check(player, "multichats.chat." + chatName)) {
                Text output = PlaceholderAPI.parsePredefinedText(
                        chatFormat,
                        PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                        placeholders
                );
                player.sendMessage(output, false);
            }
        });
    }
}
