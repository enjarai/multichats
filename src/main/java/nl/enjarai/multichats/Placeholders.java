package nl.enjarai.multichats;

import com.mojang.datafixers.kinds.IdF;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.PlaceholderResult;
import eu.pb4.placeholders.TextParser;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import nl.enjarai.multichats.types.Group;

public class Placeholders {
    public static void register() {
        PlaceholderAPI.register(new Identifier("multichats", "group_name"), (ctx) -> {
            if (ctx.hasPlayer()) {
                Group group = MultiChats.DATABASE.getPrimaryGroup(ctx.getPlayer().getUuid());
                return PlaceholderResult.value(
                        group != null ? group.displayName : TextParser.parse(MultiChats.CONFIG.defaultGroupName));
            } else {
                return PlaceholderResult.invalid("No player!");
            }
        });

        PlaceholderAPI.register(new Identifier("multichats", "spacer"), (ctx) -> {
            if (ctx.hasPlayer()) {
                Group group = MultiChats.DATABASE.getPrimaryGroup(ctx.getPlayer().getUuid());
                return PlaceholderResult.value(group != null ? new LiteralText(" ") : LiteralText.EMPTY);
            } else {
                return PlaceholderResult.invalid("No player!");
            }
        });
    }
}
