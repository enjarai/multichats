package nl.enjarai.multichats.types;

import com.mojang.datafixers.kinds.IdF;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import nl.enjarai.multichats.MultiChats;

import java.util.UUID;

public class Group {
    public String name;
    public Text displayName;
    public Text displayNameShort;

    public String prefix = null;
    public String formatOverride = null;

    public Group(String name) {
        this.name = name;
        this.displayName = new LiteralText(name);
        this.displayNameShort = new LiteralText(name);
    }

    public String getFormat() {
        return formatOverride == null ? MultiChats.CONFIG.chatFormat : formatOverride;
    }


    public boolean save() {
        return MultiChats.DATABASE.saveGroup(this);
    }

    public boolean delete() {
        return MultiChats.DATABASE.deleteGroup(this);
    }


    public boolean checkAccess(UUID uuid) {
        return MultiChats.DATABASE.checkAccess(this, uuid);
    }

    public boolean checkManager(UUID uuid) {
        return MultiChats.DATABASE.getPermissionLevel(this, uuid).canManage;
    }

    public boolean checkOwner(UUID uuid) {
        return MultiChats.DATABASE.getPermissionLevel(this, uuid) == GroupPermissionLevel.OWNER;
    }


    public void addMember(UUID uuid) {
        addMember(uuid, GroupPermissionLevel.MEMBER, false);
    }

    public void addMember(UUID uuid, boolean makePrimary) {
        addMember(uuid, GroupPermissionLevel.MEMBER, makePrimary);
    }

    public void addMember(UUID uuid, GroupPermissionLevel permissionLevel) {
        addMember(uuid, permissionLevel, false);
    }

    public void addMember(UUID uuid, GroupPermissionLevel permissionLevel, boolean makePrimary) {
        MultiChats.DATABASE.addUserToGroup(uuid, this, makePrimary, permissionLevel);
    }

    public void removeMember(UUID uuid) {
        MultiChats.DATABASE.removeUserFromGroup(uuid, this);
    }
}
