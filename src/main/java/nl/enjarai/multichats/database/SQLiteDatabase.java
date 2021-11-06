package nl.enjarai.multichats.database;

import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDatabase extends AbstractSQLDatabase {
    public SQLiteDatabase(String database) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");

        CONNECTION = DriverManager.getConnection("jdbc:sqlite:" + database);

        STATEMENT = CONNECTION.createStatement();
        this.init();
    }

    @Override
    protected String getGroupTableCreation() {
        return "CREATE TABLE IF NOT EXISTS Groups (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name varchar(64) NOT NULL UNIQUE, displayName varchar(128), displayNameShort varchar(128), " +
                "prefix varchar(64), tpX INT, tpY INT, tpZ INT, tpDimension varchar(128))";
    }

    @Override
    protected String getUserTableCreation() {
        return "CREATE TABLE IF NOT EXISTS Users (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid varchar(36) NOT NULL, groupName varchar(64) NOT NULL, permissionLevel INT NOT NULL, " +
                "isPrimary BOOL)";
    }
}
