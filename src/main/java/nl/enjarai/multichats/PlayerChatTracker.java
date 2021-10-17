package nl.enjarai.multichats;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;

public class PlayerChatTracker {
    public static HashMap<PlayerEntity, ConfigManager.Chat> players = new HashMap<>();

    public static ConfigManager.Chat isInChat(PlayerEntity player) {
        return players.get(player);
    }

    public static void setToChat(PlayerEntity player, ConfigManager.Chat chat) {
        if (chat == null) {
            players.remove(player);
            return;
        }
        players.put(player, chat);
    }
}
