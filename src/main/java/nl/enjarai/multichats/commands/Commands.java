package nl.enjarai.multichats.commands;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import nl.enjarai.multichats.ConfigManager;
import nl.enjarai.multichats.MultiChats;
import nl.enjarai.multichats.PlayerChatTracker;
import nl.enjarai.multichats.types.Group;
import nl.enjarai.multichats.types.GroupPermissionLevel;

import java.util.*;
import java.util.regex.Pattern;

import static net.minecraft.command.CommandSource.suggestMatching;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static nl.enjarai.multichats.MultiChats.*;
import static nl.enjarai.multichats.commands.Predicates.inGroupPredicate;

public class Commands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralCommandNode<ServerCommandSource> switchchat = dispatcher.register(literal("switchchat")
                .requires(Permissions.require("multichats.commands.switchchat", 0))
                .requires(Predicates.isPlayerPredicate())
                .executes(Commands::switchChatDefaultCommand)
                .then(argument("chat", StringArgumentType.string())
                    .executes(Commands::switchChatCommand)
                    .suggests((ctx, builder) -> CommandSource.suggestMatching(
                            DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid()), builder))
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
                        .suggests((ctx, builder) -> suggestMatching(DATABASE.getGroupNames(), builder))
                    )
                )
            );
            LiteralCommandNode<ServerCommandSource> alliance = dispatcher.register(literal("alliance")
                .requires(Permissions.require("multichats.commands.alliance", true))
                .requires(Predicates.isPlayerPredicate())
                .executes(Commands::help)
                .then(literal("create")
                    .requires(Permissions.require("multichats.commands.alliance.create", true))
                    .then(argument("name", StringArgumentType.string())
                        .executes(Commands::createGroup)
                    )
                )
                .then(literal("delete")
                    // .requires(inGroupPredicate(GroupPermissionLevel.OWNER))
                    .then(argument("name", StringArgumentType.string())
                        .executes(Commands::deleteGroup)
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                Permissions.check(ctx.getSource(), "multichats.admin.delete") ?
                                DATABASE.getGroupNames() :
                                DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid(), GroupPermissionLevel.OWNER), builder))
                    )
                )
                .then(literal("invite")
                    // .requires(inGroupPredicate(GroupPermissionLevel.MANAGER))
                    .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                Permissions.check(ctx.getSource(), "multichats.admin.invite") ?
                                DATABASE.getGroupNames() :
                                DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid(), GroupPermissionLevel.MANAGER), builder))
                        .then(playerArgument("player")
                            .executes(Commands::inviteToGroup)
                        )
                    )
                )
                .then(literal("kick")
                    // .requires(inGroupPredicate(GroupPermissionLevel.MANAGER))
                    .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                            Permissions.check(ctx.getSource(), "multichats.admin.kick") ?
                                    DATABASE.getGroupNames() :
                                    DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid(), GroupPermissionLevel.MANAGER), builder))
                        .then(playerArgument("player")
                            .executes(Commands::kickFromGroup)
                        )
                    )
                )
                .then(literal("accept")
                    .executes(ctx -> acceptInvite(ctx, true))
                )
                .then(literal("deny")
                    .executes(ctx -> acceptInvite(ctx, false))
                )
                .then(literal("leave")
                    // .requires(inGroupPredicate())
                    .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid()), builder))
                        .executes(Commands::leaveGroup)
                    )
                )
                .then(literal("info")
                    .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                DATABASE.getGroupNames(), builder))
                        .executes(Commands::groupInfoMembers)
                    )
                )
                .then(literal("home")
                    .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                            DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid()), builder))
                        .executes(Commands::tpHome)
                    )
                    .executes(Commands::tpHome)
                )
                .then(literal("primary")
                    .then(literal("reset")
                        .executes(ctx -> setPrimaryGroup(ctx, true))
                    )
                    .then(literal("set")
                        .then(argument("name", StringArgumentType.string())
                            .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                    DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid()), builder))
                            .executes(ctx -> setPrimaryGroup(ctx, false))
                        )
                    )
                )
                .then(literal("modify")
                    // .requires(inGroupPredicate(GroupPermissionLevel.OWNER))
                    .then(argument("name", StringArgumentType.string())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                Permissions.check(ctx.getSource(), "multichats.admin.modify") ?
                                DATABASE.getGroupNames() :
                                DATABASE.getGroupNames(ctx.getSource().getPlayer().getUuid(), GroupPermissionLevel.OWNER), builder))
                        .then(literal("giveOwnership")
                            .then(playerArgument("player")
                                .executes(ctx -> modifyGroup(ctx, ModificationType.SET_OWNER))
                            )
                        )
                        .then(literal("manager")
                            .then(literal("add")
                                .then(playerArgument("player")
                                    .executes(ctx -> modifyGroup(ctx, ModificationType.ADD_MANAGER))
                                )
                            )
                            .then(literal("remove")
                                .then(playerArgument("player")
                                    .executes(ctx -> modifyGroup(ctx, ModificationType.REMOVE_MANAGER))
                                )
                            )
                        )
                        .then(literal("messagePrefix")
                            .then(literal("reset")
                                .executes(ctx -> modifyGroup(ctx, ModificationType.PREFIX_RESET))
                            )
                            .then(literal("set")
                                .then(argument("string", StringArgumentType.string())
                                    .executes(ctx -> modifyGroup(ctx, ModificationType.PREFIX))
                                )
                            )
                        )
                        .then(literal("displayName")
                            .then(argument("string", StringArgumentType.string())
                                .executes(ctx -> modifyGroup(ctx, ModificationType.DISPLAY_NAME))
                            )
                        )
                        .then(literal("displayNameShort")
                            .then(argument("string", StringArgumentType.string())
                                .executes(ctx -> modifyGroup(ctx, ModificationType.DISPLAY_NAME_SHORT))
                            )
                        )
                        .then(literal("home")
                                .then(literal("unset")
                                        .executes(ctx -> modifyGroup(ctx, ModificationType.UNSETHOME))
                                )
                                .then(literal("set")
                                        .executes(ctx -> modifyGroup(ctx, ModificationType.SETHOME))
                                )
                        )
                    )
                )
            );
            dispatcher.register(literal("al")
                .executes(Commands::help)
                .redirect(alliance)
            );
        });
    }

    private static TimerManager PRIMARY_GROUP_TIMER = new TimerManager(CONFIG.primaryGroupSwitchCooldownSeconds);

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(TextParser.parse(
                "<dark_aqua><bold>MultiChats v" + VERSION + " by enjarai</bold>\n<aqua>" +
                        "  This command is used to manage your alliances,\n" +
                        "  this is a custom feature designed to help manage\n" +
                        "  cities and player groups.\n" +
                        "  \n" +
                        "  More information can be found at:\n" +
                        "  <blue><underlined><url:'https://enjarai.nl/multichats/'>https://enjarai.nl/multichats/</url></underlined></blue>"
        ), false);
        return 0;
    }


    private static int createGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = ctx.getArgument("name", String.class)
                .toLowerCase(Locale.ROOT).replaceAll("\\s","");

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        if (DATABASE.getGroup(name) != null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.existsError), true);
            return 1;
        }

        if (!DATABASE.getGroups(player.getUuid(), GroupPermissionLevel.OWNER).isEmpty()) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.cantOwnTwoGroupsError), true);
            return 1;
        }

        Group group = new Group(name);
        if (!(
                group.save() &&
                group.addMember(player.getUuid(), GroupPermissionLevel.OWNER, true)
        )) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
            return 1;
        }


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.groupCreated),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    private static int deleteGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = ctx.getArgument("name", String.class);

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        Group group = DATABASE.getGroup(name);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        if (!(Permissions.check(player, "multichats.admin.delete") || group.checkOwner(player.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
            return 1;
        }

        if (!group.delete()) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
            return 1;
        }


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.groupDeleted),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    private static int inviteToGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = ctx.getArgument("name", String.class);
        String playerName = ctx.getArgument("player", String.class);

        ServerPlayerEntity inviteFrom = ctx.getSource().getPlayer();
        if (inviteFrom == null) { return 1; }

        Group group = DATABASE.getGroup(name);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        if (!(Permissions.check(inviteFrom, "multichats.admin.invite") || group.checkManager(inviteFrom.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
            return 1;
        }

        ServerPlayerEntity inviteTo = SERVER.getPlayerManager().getPlayer(playerName);
        if (inviteTo == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.cantFindPlayerError), true);
            return 1;
        }

        if (group.checkAccess(inviteTo.getUuid())) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.inGroupError), true);
            return 1;
        }

        INVITE_MANAGER.putInvite(inviteTo.getUuid(), group, inviteFrom);


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("playerFrom", inviteFrom.getDisplayName());
        placeholders.put("playerTo", inviteTo.getDisplayName());
        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.sentInvite),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        inviteTo.sendMessage(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.receivedInvite),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), false);
        return 0;
    }

    private static int acceptInvite(CommandContext<ServerCommandSource> ctx, boolean accept) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        InviteManager.Invite invite = INVITE_MANAGER.hasInvites(player.getUuid());
        if (invite == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noInvitesError), true);
            return 1;
        }

        String messageTo;
        String messageFrom;
        if (accept) {
            if (!(invite.group.addMember(player.getUuid(), DATABASE.getPrimaryGroup(player.getUuid()) == null))) {
                ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
                return 1;
            }

            messageTo = CONFIG.messages.inviteAcceptedTo;
            messageFrom = CONFIG.messages.inviteAcceptedFrom;
        } else {
            messageTo = CONFIG.messages.inviteDeniedTo;
            messageFrom = CONFIG.messages.inviteDeniedFrom;
        }


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("playerFrom", invite.from.getDisplayName());
        placeholders.put("playerTo", player.getDisplayName());
        placeholders.put("group", invite.group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(messageTo),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        invite.from.sendMessage(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(messageFrom),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), false);
        return 0;
    }

    private static int leaveGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = ctx.getArgument("name", String.class);

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        Group group = DATABASE.getGroup(name);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        if (!(group.checkAccess(player.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.notInGroupError), true);
            return 1;
        }

        if (group.checkOwner(player.getUuid())) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.groupOwnerCantLeaveError), true);
            return 1;
        }

        if (!(group.removeMember(player.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
            return 1;
        }


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.groupLeft),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    private static int modifyGroup(CommandContext<ServerCommandSource> ctx, ModificationType type) throws CommandSyntaxException {
        String name = ctx.getArgument("name", String.class);

        ServerPlayerEntity player = ctx.getSource().getPlayer();

        Group group = DATABASE.getGroup(name);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        if (!(Permissions.check(player, "multichats.admin.modify") || group.checkOwner(player.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
            return 1;
        }

        HashMap<String, Text> placeholders = new HashMap<>();

        boolean success = false;
        String error = null;
        if (type.argumentType == null) {
            switch (type) {
                case PREFIX_RESET -> {
                    group.prefix = null;
                    success = group.save();
                }
                case SETHOME -> {
                    if (group.eligibleForHome()) {
                        Vec3d pos = player.getPos();
                        group.setHome((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z),
                                player.getServerWorld().getRegistryKey().getValue().toString());
                        success = group.save();
                    } else {
                        error = CONFIG.messages.notEligibleForHomeError;
                    }
                }
                case UNSETHOME -> {
                    group.setHome(null, null);
                    success = group.save();
                }
            }
        } else {
            switch (type.argumentType) {
                case "string" -> {
                    String arg = ctx.getArgument("string", String.class);
                    placeholders.put("string", type.formatStringArg ? TextParser.parse(arg) : new LiteralText(arg));

                    switch (type) {
                        case PREFIX -> {
                            arg = arg.toLowerCase();
                            Pattern pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
                            if (arg.length() > 3 || arg.length() < 1 || pattern.matcher(arg).find()) {
                                error = CONFIG.messages.prefixTooLongError;
                                break;
                            }

                            group.prefix = arg + ":";
                            success = group.save();
                        }
                        case DISPLAY_NAME -> {
                            group.displayName = TextParser.parse(arg);
                            success = group.save();
                        }
                        case DISPLAY_NAME_SHORT -> {
                            group.displayNameShort = TextParser.parse(arg);
                            success = group.save();
                        }
                    }
                }
                case "player" -> {
                    ServerPlayerEntity arg = SERVER.getPlayerManager().getPlayer(ctx.getArgument("player", String.class));
                    if (arg == null) {
                        ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.cantFindPlayerError), true);
                        return 1;
                    }

                    placeholders.put("player", arg.getDisplayName());
                    UUID uuid = arg.getUuid();
                    if (!group.checkAccess(uuid)) {
                        ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.playerNotInGroupError), true);
                        return 1;
                    }

                    if (group.checkAccess(uuid, GroupPermissionLevel.OWNER)) {
                        ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
                        return 1;
                    }

                    switch (type) {
                        case SET_OWNER -> {
                            if (!DATABASE.getGroups(uuid, GroupPermissionLevel.OWNER).isEmpty()) {
                                error = CONFIG.messages.cantOwnTwoGroupsError;
                                break;
                            }

                            success = group.changeOwner(uuid);
                        }
                        case ADD_MANAGER -> {
                            if (group.checkAccess(uuid, GroupPermissionLevel.MANAGER)) {
                                error = CONFIG.messages.alreadyManagerError;
                                break;
                            }

                            success = group.addMember(uuid, GroupPermissionLevel.MANAGER);
                        }
                        case REMOVE_MANAGER -> {
                            if (!group.checkAccess(uuid, GroupPermissionLevel.MANAGER)) {
                                error = CONFIG.messages.notManagerError;
                                break;
                            }

                            success = group.addMember(uuid, GroupPermissionLevel.MEMBER);
                        }
                    }
                }
            }
        }

        if (error != null) {
            ctx.getSource().sendFeedback(TextParser.parse(error), true);
            return 1;
        }
        if (!success) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
            return 1;
        }


        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(type.message),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    private static int kickFromGroup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String name = ctx.getArgument("name", String.class);
        String playerName = ctx.getArgument("player", String.class);

        ServerPlayerEntity sourcePlayer = ctx.getSource().getPlayer();
        if (sourcePlayer == null) { return 1; }

        Group group = DATABASE.getGroup(name);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        if (!(Permissions.check(sourcePlayer, "multichats.admin.kick") || group.checkManager(sourcePlayer.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
            return 1;
        }

        GameProfile kickPlayer = SERVER.getUserCache().findByName(playerName).orElse(null);
        if (kickPlayer == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.cantFindPlayerError), true);
            return 1;
        }

        if (!group.checkAccess(kickPlayer.getId())) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.playerNotInGroupError), true);
            return 1;
        }

        if (group.checkManager(kickPlayer.getId())) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
            return 1;
        }

        DATABASE.removeUserFromGroup(kickPlayer.getId(), group);


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("player", new LiteralText(kickPlayer.getName()));
        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.groupMemberKicked),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    private static int groupInfoMembers(CommandContext<ServerCommandSource> ctx) {
        String name = ctx.getArgument("name", String.class);

        Group group = DATABASE.getGroup(name);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        HashMap<String, Text> p1 = new HashMap<>();

        p1.put("group", group.displayName);
        p1.put("prefix", new LiteralText(group.prefix == null ? "Unset" : group.prefix));
        p1.put("home", new LiteralText( // this is a mess but it works
                group.eligibleForHome() ?
                (group.homePos == null ?
                        "Unset" :
                        "%d, %d, %d".formatted((int) group.homePos.x, (int) group.homePos.y, (int) group.homePos.z)
                ) :
                "Not eligible (%d/%d members required)".formatted(group.getPrimaryMembers().size(), CONFIG.membersRequiredForHome)
        ));

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.groupInfo),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                p1
        ), true);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.groupMemberList),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                p1
        ), true);

        UserCache cache = SERVER.getUserCache();
        for (Map.Entry<UUID, GroupPermissionLevel> set : group.getMembers().entrySet()) {
            GameProfile player = cache.getByUuid(set.getKey()).orElse(null);
            if (player == null) { continue; }

            HashMap<String, Text> p2 = new HashMap<>();

            p2.put("permissionLevel", TextParser.parse(set.getValue().displayName));
            p2.put("player", new LiteralText(player.getName()));

            ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                    TextParser.parse(CONFIG.messages.groupMemberListEntry),
                    PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                    p2
            ), true);
        }
        return 0;
    }

    private static int setPrimaryGroup(CommandContext<ServerCommandSource> ctx, boolean reset) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        HashMap<String, Text> placeholders = new HashMap<>();

        UUID uuid = player.getUuid();
        if (reset) {
            if (!(DATABASE.changePrimaryGroup(uuid, null))) {
                ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
                return 1;
            }
        } else {
            String name = ctx.getArgument("name", String.class);

            Group group = DATABASE.getGroup(name);
            if (group == null) {
                ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
                return 1;
            }

            if (!(group.checkAccess(uuid))) {
                ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.notInGroupError), true);
                return 1;
            }

            if (!PRIMARY_GROUP_TIMER.canTrigger(uuid)) {
                ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.waitToSwitchPrimaryError), true);
                return 1;
            }

            if (!(DATABASE.changePrimaryGroup(uuid, group))) {
                ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.unknownError), true);
                return 1;
            }

            placeholders.put("group", group.displayName);
        }


        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(reset ? CONFIG.messages.groupPrimaryReset : CONFIG.messages.groupSetToPrimary),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    private static int tpHome(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();

        Group group;
        try {
            String name = ctx.getArgument("name", String.class);
            group = DATABASE.getGroup(name);
        } catch (IllegalArgumentException e) {
            group = DATABASE.getPrimaryGroup(player.getUuid());
        }

        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), true);
            return 1;
        }

        if (!(Permissions.check(player, "multichats.admin.home") || group.checkPrimary(player.getUuid()))) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionError), true);
            return 1;
        }

        if (!group.eligibleForHome()) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.notEligibleForHomeError), true);
            return 1;
        }

        if (group.homePos == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noHomeSetError), true);
            return 1;
        }

        player.teleport(group.getHomeDim(), group.homePos.x + 0.5, group.homePos.y, group.homePos.z + 0.5,
                player.getYaw(), player.getPitch());


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.teleportedHome),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), true);
        return 0;
    }

    // old stuff

    private static int switchChatDefaultCommand(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        PlayerChatTracker.setToChat(player, null);
        ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.switchedGlobal), false);
        return 0;
    }

    private static int switchChatCommand(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String chatName = ctx.getArgument("chat", String.class);

        ServerPlayerEntity player = ctx.getSource().getPlayer();
        

        Group group = DATABASE.getGroup(chatName);
        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noGroupError), false);
            return 1;
        }

        // Continue command if group is null (so default) or player has permission node, otherwise respond with error
        if (!group.checkAccess(player.getUuid())) {
            ctx.getSource().sendFeedback(TextParser.parse(CONFIG.messages.noPermissionChatError), false);
            return 1;
        }

        PlayerChatTracker.setToChat(player, group);


        HashMap<String, Text> placeholders = new HashMap<>();

        placeholders.put("group", group.displayName);
        placeholders.put("prefix", new LiteralText(group.prefix));

        ctx.getSource().sendFeedback(PlaceholderAPI.parsePredefinedText(
                TextParser.parse(CONFIG.messages.switched + (group.prefix != null ? "\n" + CONFIG.messages.switchedPrefix : "")),
                PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                placeholders
        ), false);
        return 0;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        CONFIG = ConfigManager.loadConfigFile(MultiChats.CONFIG_FILE);
        PRIMARY_GROUP_TIMER = new TimerManager(CONFIG.primaryGroupSwitchCooldownSeconds);
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

        if (DATABASE.getGroup(chatName) != null) {
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
        Group group = DATABASE.getGroup(chatName);

        if (group == null) {
            ctx.getSource().sendFeedback(TextParser.parse("<red>That chat doesn't exist"), true);
            return 1;
        }

        group.delete();

        ctx.getSource().sendFeedback(TextParser.parse("Group \"" + chatName + "\" deleted"), true);
        return 0;
    }


    public static RequiredArgumentBuilder<ServerCommandSource, String> playerArgument(String name) {
        return CommandManager.argument(name, StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                    for (String player : ctx.getSource().getServer().getPlayerNames()) {
                        if (player.toLowerCase(Locale.ROOT).contains(remaining)) {
                            builder.suggest(player);
                        }
                    }

                    return builder.buildFuture();
                });
    }
}
