package nl.enjarai.multichats.types;

import net.minecraft.client.util.math.Vector3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import nl.enjarai.multichats.Helpers;
import nl.enjarai.multichats.MultiChats;

import java.util.HashMap;
import java.util.UUID;

public class Group {
    public String name;
    public Text displayName;
    public Text displayNameShort;

    public String prefix = null;
    public String formatOverride = null;
    public Vec3d homePos = null;
    public String homeDimension = null;

    public Group(String name) {
        this.name = name;
        this.displayName = new LiteralText(name);
        this.displayNameShort = new LiteralText(name);
    }

    public String getFormat() {
        return formatOverride == null ? MultiChats.CONFIG.chatFormat : formatOverride;
    }

    public void setHome(Vec3d pos, String dimension) {
        homePos = pos;
        homeDimension = dimension;
    }

    public void setHome(int x, int y, int z, String dimension) {
        if (Helpers.areAllEqual(0, x, y, z)) {
            setHome(null, null);
        } else {
            setHome(new Vec3d(x, y, z), dimension);
        }
    }

    public ServerWorld getHomeDim() {
        return MultiChats.SERVER.getWorld(RegistryKey.of(Registry.WORLD_KEY, Identifier.tryParse(homeDimension)));
    }


    public boolean save() {
        return MultiChats.DATABASE.saveGroup(this);
    }

    public boolean delete() {
        return MultiChats.DATABASE.deleteGroup(this);
    }

    public Group refresh() {
        return MultiChats.DATABASE.getGroup(name);
    }


    public boolean checkAccess(UUID uuid) {
        return MultiChats.DATABASE.checkAccess(this, uuid);
    }

    public boolean checkAccess(UUID uuid, GroupPermissionLevel permissionLevel) {
        return MultiChats.DATABASE.getPermissionLevel(this, uuid).dbInt >= permissionLevel.dbInt;
    }

    public boolean checkManager(UUID uuid) {
        return MultiChats.DATABASE.getPermissionLevel(this, uuid).canManage;
    }

    public boolean checkOwner(UUID uuid) {
        return MultiChats.DATABASE.getPermissionLevel(this, uuid) == GroupPermissionLevel.OWNER;
    }

    public boolean checkPrimary(UUID uuid) {
        return MultiChats.DATABASE.checkPrimary(this, uuid);
    }


    public HashMap<UUID, GroupPermissionLevel> getMembers() {
        return MultiChats.DATABASE.getMembers(this);
    }

    public HashMap<UUID, GroupPermissionLevel> getMembers(GroupPermissionLevel exactPermissionLevel) {
        return MultiChats.DATABASE.getMembers(this, exactPermissionLevel);
    }


    public boolean addMember(UUID uuid) {
        return addMember(uuid, GroupPermissionLevel.MEMBER, false);
    }

    public boolean addMember(UUID uuid, boolean makePrimary) {
        return addMember(uuid, GroupPermissionLevel.MEMBER, makePrimary);
    }

    public boolean addMember(UUID uuid, GroupPermissionLevel permissionLevel) {
        return addMember(uuid, permissionLevel, this.checkPrimary(uuid));
    }

    public boolean addMember(UUID uuid, GroupPermissionLevel permissionLevel, boolean makePrimary) {
        return MultiChats.DATABASE.addUserToGroup(uuid, this, makePrimary, permissionLevel);
    }

    public boolean removeMember(UUID uuid) {
        return MultiChats.DATABASE.removeUserFromGroup(uuid, this);
    }

    public boolean changeOwner(UUID uuid) {
        return MultiChats.DATABASE.changeGroupOwner(this, uuid);
    }
}
