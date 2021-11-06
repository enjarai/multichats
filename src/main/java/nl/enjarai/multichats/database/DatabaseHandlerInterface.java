package nl.enjarai.multichats.database;

import nl.enjarai.multichats.types.Group;
import nl.enjarai.multichats.types.GroupPermissionLevel;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public interface DatabaseHandlerInterface {
    void closeConnection();


    /**
     * Saves a group to the database.
     * @param group Group object to save.
     * @return True if the action succeeded, false otherwise.
     */
    boolean saveGroup(Group group);
    /**
     * Deletes a group from the database.
     * @param group Group object to delete.
     * @return True if the action succeeded, false otherwise.
     */
    boolean deleteGroup(Group group);

    List<Group> getGroups();
    List<Group> getGroups(UUID uuid);
    List<Group> getGroups(UUID uuid, GroupPermissionLevel permissionLevel);

    List<String> getGroupNames();
    List<String> getGroupNames(UUID uuid);
    List<String> getGroupNames(UUID uuid, GroupPermissionLevel permissionLevel);

    Group getGroup(String name);
    boolean changeGroupOwner(Group group, UUID uuid);

    Group getPrimaryGroup(UUID uuid);


    boolean addUserToGroup(UUID uuid, Group group, boolean primary, GroupPermissionLevel permissionLevel);

    boolean removeUserFromGroup(UUID uuid, Group group);

    boolean changePrimaryGroup(UUID uuid, Group group);
    boolean checkAccess(Group group, UUID uuid);
    boolean checkPrimary(Group group, UUID uuid);

    GroupPermissionLevel getPermissionLevel(Group group, UUID uuid);

    HashMap<UUID, GroupPermissionLevel> getMembers(Group group);
    HashMap<UUID, GroupPermissionLevel> getMembers(Group group, GroupPermissionLevel exactPermissionLevel);

    HashMap<UUID, GroupPermissionLevel> getPrimaryMembers(Group group);
}
