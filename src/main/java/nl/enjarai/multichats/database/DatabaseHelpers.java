package nl.enjarai.multichats.database;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import nl.enjarai.multichats.MultiChats;
import nl.enjarai.multichats.types.Group;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseHelpers {
    public static Group resultToGroup(ResultSet result) throws SQLException {
        Group group = new Group(result.getString("name"));

        group.displayName = Text.Serializer.fromJson(result.getString("displayName"));
        group.displayNameShort = Text.Serializer.fromJson(result.getString("displayNameShort"));
        group.prefix = result.getString("prefix");
        group.setHome(result.getInt("tpX"), result.getInt("tpY"), result.getInt("tpZ"),
                result.getString("tpDimension"));

        return group;
    }
}
