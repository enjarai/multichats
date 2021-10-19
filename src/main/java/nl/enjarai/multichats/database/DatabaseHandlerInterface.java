package nl.enjarai.multichats.database;

import nl.enjarai.multichats.types.Group;
import nl.enjarai.multichats.types.GroupPermissionLevel;

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
    List<String> getGroupNames();
    List<String> getGroupNames(UUID uuid);

    Group getGroup(String name);
    Group getPrimaryGroup(UUID uuid);


    boolean addUserToGroup(UUID uuid, Group group, boolean primary, GroupPermissionLevel permissionLevel);

    boolean removeUserFromGroup(UUID uuid, Group group);

    boolean changePrimaryGroup(UUID uuid, Group group);
    boolean checkAccess(Group group, UUID uuid);

    GroupPermissionLevel getPermissionLevel(Group group, UUID uuid);
}
