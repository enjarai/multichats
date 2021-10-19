package nl.enjarai.multichats;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import nl.enjarai.multichats.types.Group;

import java.util.HashMap;

import static net.minecraft.command.CommandSource.suggestMatching;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralCommandNode<ServerCommandSource> switchchat = dispatcher.register(literal("switchchat")
                .requires(Permissions.require("multichats.commands.switchchat", 0))
                .executes(Commands::switchChatDefaultCommand)
                .then(argument("chat", StringArgumentType.string())
                    .executes(Commands::switchChatCommand)
                    .suggests((ctx, builder) -> suggestMatching(MultiChats.DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid()), builder))
                )
            );
            dispatcher.register(literal("sc")
                    .executes(Commands::switchChatDefaultCommand)
                    .redirect(switchchat)
            );
            dispatcher.register(literal("multichats")
                .requires(Permissions.require("multichats.commands.multichats", 4))
                .then(literal("reload")
                    .executes(Commands::reloadConfig)
                )
                .then(literal("create")
                    .then(argument("name", StringArgumentType.string())
                        .then(argument("prefix", StringArgumentType.string())
                            .executes(Commands::createChat)
                            .then(argument("displayName", StringArgumentType.string())
                                .executes(Commands::createChat)
                                .then(argument("displayNameShort", StringArgumentType.string())
                                    .executes(Commands::createChat)
                                )
                            )
                        )
                    )
                )
                .then(literal("delete")
                    .then(argument("name", StringArgumentType.string())
                        .executes(Commands::deleteChat)
                        .suggests((ctx, builder) -> suggestMatching(MultiChats.DATABASE.getGroupNames(), builder))
                    )
                )
            );
            LiteralCommandNode<ServerCommandSource> alliance = dispatcher.register(literal("alliance")
                    .requires(Permissions.require("multichats.commands.alliance", true))
                    .then(literal("create")
                            .then(argument("name", StringArgumentType.string())
                                    .executes(Commands::createGroup)
                            )
                    )
            );
        });
    }

    private static int createGroup(CommandContext<ServerCommandSource> ctx) {
        String name = ctx.getArgument("name", String.class);

        if (MultiChats.DATABASE.getGroup(name) != null) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.existsError), true);
            return 1;
        }

        Group group = new Group(name);
        if (!group.save()) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.unknownError), true);
            return 1;
        }


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(MultiChats.CONFIG.messages.groupCreated),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    // old stuff

    private static int switchChatDefaultCommand(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player;

        try {
            player = ctx.getSource().getPlayer();
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noPlayerError), false);
            return 1;
        }

        PlayerChatTracker.setToChat(player, null);
        ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.switchedGlobal), false);
        return 0;
    }

    private static int switchChatCommand(CommandContext<ServerCommandSource> ctx) {
        String chatName = ctx.getArgument("chat", String.class);
        ServerPlayerEntity player;

        try {
            player = ctx.getSource().getPlayer();
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noPlayerError), false);
            return 1;
        }

        Group group = MultiChats.DATABASE.getGroup(chatName);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noGroupError), false);
            return 1;
        }

        // Continue command if group is null (so default) or player has permission node, otherwise respond with error
        if (!group.checkAccess(player.getUuid())) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noPermissionError), false);
            return 1;
        }

        PlayerChatTracker.setToChat(player, group);


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);
        placeholders.put("prefix", new LiteralText(group.prefix));

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(MultiChats.CONFIG.messages.switched),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), false);
        return 0;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        MultiChats.CONFIG = ConfigManager.loadConfigFile(MultiChats.CONFIG_FILE);
        ctx.getSource().sendFeedback(TextParser.parse("Reloaded config!"), true);
        return 0;
    }

    private static int createChat(CommandContext<ServerCommandSource> ctx) {
        String chatName = ctx.getArgument("name", String.class);
        String prefix = ctx.getArgument("prefix", String.class);

        String displayName;
        try {
            displayName = ctx.getArgument("displayName", String.class);
        } catch (Exception e) {
            displayName = chatName;
        }

        String displayNameShort;
        try {
            displayNameShort = ctx.getArgument("displayNameShort", String.class);
        } catch (Exception e) {
            displayNameShort = displayName;
        }

        if (MultiChats.DATABASE.getGroup(chatName) != null) {
            ctx.getSource().sendFeedback(TextParser.parse("<red>That group already exists"), true);
            return 1;
        }

        Group group = new Group(chatName);
        group.displayName = TextParser.parse(displayName);
        group.displayNameShort = TextParser.parse(displayNameShort);
        group.prefix = prefix;
        group.save();

        ctx.getSource().sendFeedback(TextParser.parse("Group \"" + chatName + "\" created\nTo assign this group to players use the node <green>multichats.chat." + chatName), true);
        return 0;
    }

    private static int deleteChat(CommandContext<ServerCommandSource> ctx) {
        String chatName = ctx.getArgument("name", String.class);
        Group group = MultiChats.DATABASE.getGroup(chatName);

        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse("<red>That chat doesn't exist"), true);
            return 1;
        }

        group.delete();

        ctx.getSource().sendFeedback(TextParser.parse("Group \"" + chatName + "\" deleted"), true);
        return 0;
    }
}
