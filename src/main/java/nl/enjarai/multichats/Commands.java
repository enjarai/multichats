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
import net.minecraft.util.Formatting;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

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
                    .suggests((ctx, builder) -> suggestMatching(switchChatCompletion(ctx), builder))
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
                        .suggests((ctx, builder) -> suggestMatching(MultiChats.CONFIG.chats.keySet(), builder))
                    )
                )
            );
        });
    }

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

        ConfigManager.Chat chat = MultiChats.CONFIG.chats.get(chatName);
        if (chat == null) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noChatError), false);
            return 1;
        }

        // Continue command if chat is null (so default) or player has permission node, otherwise respond with error
        if (!Permissions.check(player, "multichats.chat." + chat)) {
            ctx.getSource().sendFeedback(TextParser.parse(MultiChats.CONFIG.messages.noPermissionError), false);
            return 1;
        }

        PlayerChatTracker.setToChat(player, chat);


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("chat", TextParser.parse(chat.displayName));
        placeholders.put("prefix", new LiteralText(chat.prefix));

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

        if (MultiChats.CONFIG.chats.get(chatName) != null) {
            ctx.getSource().sendFeedback(TextParser.parse("<red>That chat already exists"), true);
            return 1;
        }

        MultiChats.CONFIG.chats.put(chatName, new ConfigManager.Chat(
                displayName,
                displayNameShort,
                prefix,
                null
        ));
        MultiChats.CONFIG.saveConfigFile(MultiChats.CONFIG_FILE);

        ctx.getSource().sendFeedback(TextParser.parse("Chat \"" + chatName + "\" created\nTo assign this chat to players use the node <green>multichats.chat." + chatName), true);
        return 0;
    }

    private static int deleteChat(CommandContext<ServerCommandSource> ctx) {
        String chatName = ctx.getArgument("name", String.class);

        if (MultiChats.CONFIG.chats.get(chatName) == null) {
            ctx.getSource().sendFeedback(TextParser.parse("<red>That chat doesn't exist"), true);
            return 1;
        }

        MultiChats.CONFIG.chats.remove(chatName);
        MultiChats.CONFIG.saveConfigFile(MultiChats.CONFIG_FILE);

        ctx.getSource().sendFeedback(TextParser.parse("Chat \"" + chatName + "\" deleted"), true);
        return 0;
    }

    private static Set<String> switchChatCompletion(CommandContext<ServerCommandSource> ctx) {
        // Selects only the chats the player has access to, to show in tab-completion
        return MultiChats.CONFIG.chats.keySet().stream().filter(
                i -> {
                    try {
                        return Permissions.check(ctx.getSource().getPlayer(), "multichats.chat." + i);
                    } catch (CommandSyntaxException e) {
                        return false;
                    }
                }
        ).collect(Collectors.toSet());
    }
}
