package nl.enjarai.multichats;

import net.minecraft.entity.player.PlayerEntity;
import nl.enjarai.multichats.types.Group;

import java.util.HashMap;

public class PlayerChatTracker {
    public static HashMap<PlayerEntity, Group> players = new HashMap<>();

    public static Group isInChat(PlayerEntity player) {
        return players.get(player);
    }

    public static void setToChat(PlayerEntity player, Group group) {
        if (group == null) {
            players.remove(player);
            return;
        }
        players.put(player, group);
    }
}
