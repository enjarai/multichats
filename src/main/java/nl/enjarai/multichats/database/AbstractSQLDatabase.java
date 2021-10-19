package nl.enjarai.multichats.database;

import net.minecraft.text.Text;
import nl.enjarai.multichats.Helpers;
import nl.enjarai.multichats.types.Group;
import nl.enjarai.multichats.types.GroupPermissionLevel;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractSQLDatabase implements DatabaseHandlerInterface {
    protected Connection CONNECTION;
    protected Statement STATEMENT;


    protected abstract String getGroupTableCreation();
    protected abstract String getUserTableCreation();

    public void createTables() throws SQLException {
        STATEMENT.execute(getGroupTableCreation());
        STATEMENT.execute(getUserTableCreation());
    }

    public void closeConnection() {
        try {
            CONNECTION.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Group management

    @Override
    public boolean saveGroup(Group group) {
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "INSERT OR REPLACE INTO Groups VALUES (NULL, ?, ?, ?, ?);");
            prepStmt.setString(1, group.name);
            prepStmt.setString(2, Text.Serializer.toJson(group.displayName));
            prepStmt.setString(3, Text.Serializer.toJson(group.displayNameShort));
            prepStmt.setString(4, group.prefix);

            prepStmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteGroup(Group group) {
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "DELETE FROM Groups WHERE name=?;");
            prepStmt.setString(1, group.name);

            prepStmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<Group> getGroups() {
        List<Group> list = new LinkedList<>();
        try {
            String query = "SELECT * FROM Groups;";
            ResultSet result = STATEMENT.executeQuery(query);
            Group group;

            if (result.next()) {
                do {
                group = new Group(result.getString("name"));
                group.displayName = Text.Serializer.fromJson(result.getString("displayName"));
                group.displayNameShort = Text.Serializer.fromJson(result.getString("displayNameShort"));
                group.prefix = result.getString("prefix");

                list.add(group);
                } while (result.next());
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    @Override
    public List<Group> getGroups(UUID uuid) {
        List<Group> list = new LinkedList<>();
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "SELECT * FROM Groups WHERE name=(SELECT groupName FROM Users WHERE uuid=?);");
            prepStmt.setString(1, uuid.toString());

            ResultSet result = prepStmt.executeQuery();
            Group group;

            if (result.next()) {
                do {
                    group = new Group(result.getString("name"));
                    group.displayName = Text.Serializer.fromJson(result.getString("displayName"));
                    group.displayNameShort = Text.Serializer.fromJson(result.getString("displayNameShort"));
                    group.prefix = result.getString("prefix");

                    list.add(group);
                } while (result.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    @Override
    public List<String> getGroupNames() {
        List<String> list = new LinkedList<>();
        try {
            String query = "SELECT name FROM Groups;";
            ResultSet result = STATEMENT.executeQuery(query);

            if (result.next()) {
                do {
                    list.add(result.getString("name"));
                } while (result.next());
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    @Override
    public List<String> getGroupNames(UUID uuid) {
        List<String> list = new LinkedList<>();
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "SELECT name FROM Groups WHERE name=(SELECT groupName FROM Users WHERE uuid=?);");
            prepStmt.setString(1, uuid.toString());

            ResultSet result = prepStmt.executeQuery();

            if (result.next()) {
                do {
                    list.add(result.getString("name"));
                } while (result.next());
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    @Override
    public Group getGroup(String name) {
        Group group;
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "SELECT * FROM Groups WHERE name=? LIMIT 1;");
            prepStmt.setString(1, name);

            ResultSet result = prepStmt.executeQuery();

            if (result.next()) {
                group = new Group(result.getString("name"));
                group.displayName = Text.Serializer.fromJson(result.getString("displayName"));
                group.displayNameShort = Text.Serializer.fromJson(result.getString("displayNameShort"));
                group.prefix = result.getString("prefix");
            } else {
                group = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return group;
    }

    // User management

    @Override
    public Group getPrimaryGroup(UUID uuid) {
        Group group;
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "SELECT * FROM Groups WHERE name=(SELECT groupName FROM Users WHERE uuid=?, isPrimary=true LIMIT 1) LIMIT 1;");
            prepStmt.setString(1, uuid.toString());

            ResultSet result = prepStmt.executeQuery();

            if (result.next()) {
                group = new Group(result.getString("name"));
                group.displayName = Text.Serializer.fromJson(result.getString("displayName"));
                group.displayNameShort = Text.Serializer.fromJson(result.getString("displayNameShort"));
                group.prefix = result.getString("prefix");
            } else {
                group = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return group;
    }

    @Override
    public boolean addUserToGroup(UUID uuid, Group group, boolean primary, GroupPermissionLevel permissionLevel) {
        try {
            Group currentPrimary = getPrimaryGroup(uuid);

            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "INSERT OR UPDATE INTO Users VALUES (NULL, ?, ?, ?, ?);");
            prepStmt.setString(1, uuid.toString());
            prepStmt.setString(2, group.name);
            prepStmt.setInt(3, permissionLevel.dbInt);
            prepStmt.setBoolean(4, primary);

            prepStmt.executeUpdate();

            // If this user already has a primary group, and it's not this one, fix the duplicate with changePrimaryGroup.
            if (primary && currentPrimary != null && !Objects.equals(currentPrimary.name, group.name)) {
                return changePrimaryGroup(uuid, group);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean removeUserFromGroup(UUID uuid, Group group) {
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "DELETE FROM Users WHERE uuid=?, groupName=?;");
            prepStmt.setString(1, uuid.toString());
            prepStmt.setString(2, group.name);

            prepStmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean changePrimaryGroup(UUID uuid, Group group) {
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "UPDATE Users SET primary=false WHERE uuid=?;" +
                    "UPDATE Users SET primary=true WHERE uuid=?, groupName=?;");
            prepStmt.setString(1, uuid.toString());
            prepStmt.setString(1, uuid.toString());
            prepStmt.setString(2, group.name);

            prepStmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean checkAccess(Group group, UUID uuid) {
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "SELECT * FROM Users WHERE uuid=?, groupName=? LIMIT 1;");
            prepStmt.setString(1, uuid.toString());
            prepStmt.setString(2, group.name);

            ResultSet result = prepStmt.executeQuery();

            return result.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public GroupPermissionLevel getPermissionLevel(Group group, UUID uuid) {
        try {
            PreparedStatement prepStmt = CONNECTION.prepareStatement(
                    "SELECT permissionLevel FROM Users WHERE uuid=?, groupName=? LIMIT 1;");
            prepStmt.setString(1, uuid.toString());
            prepStmt.setString(2, group.name);

            ResultSet result = prepStmt.executeQuery();

            return Helpers.getPermissionLevelFromInt(result.getInt("permissionLevel"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}