package nl.enjarai.multichats.commands;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import nl.enjarai.multichats.types.Group;
import nl.enjarai.multichats.types.GroupPermissionLevel;

import java.util.function.Predicate;

import static nl.enjarai.multichats.MultiChats.DATABASE;

public class Predicates {
    public static Predicate<ServerCommandSource> inGroupPredicate() {
        return inGroupPredicate(GroupPermissionLevel.MEMBER);
    }

    public static Predicate<ServerCommandSource> inGroupPredicate(GroupPermissionLevel permissionLevel) {
        return player -> {
            try {
                return Permissions.check(player, "multichats.admin") ||
                        !DATABASE.getGroups(player.getPlayer().getUuid(), permissionLevel).isEmpty();
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return false;
            }
        };
    }

    public static Predicate<ServerCommandSource> inGroupPredicate(Group group) {
        return inGroupPredicate(group, GroupPermissionLevel.MEMBER);
    }

    public static Predicate<ServerCommandSource> inGroupPredicate(Group group, GroupPermissionLevel permissionLevel) {
        return player -> {
            try {
                return Permissions.check(player, "multichats.admin") ||
                        group.checkAccess(player.getPlayer().getUuid(), permissionLevel);
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
                return false;
            }
        };
    }

    public static Predicate<ServerCommandSource> isPlayerPredicate() {
        return source -> {
            try {
                return source.getPlayer() != null;
            } catch (CommandSyntaxException e) {
                return false;
            }
        };
    }
}
