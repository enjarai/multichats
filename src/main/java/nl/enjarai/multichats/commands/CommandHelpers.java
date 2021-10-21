package nl.enjarai.multichats.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.placeholders.TextParser;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import nl.enjarai.multichats.MultiChats;

public class CommandHelpers {
    public static ServerPlayerEntity checkPlayer(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player;

        try {
            player = ctx.getSource().getPlayer();
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noPlayerError), true);
            return null;
        }

        return player;
    }
}
