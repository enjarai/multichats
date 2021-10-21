package nl.enjarai.multichats.types;

public enum GroupPermissionLevel {
    MEMBER(0, false, "<green>MEMBER"),
    MANAGER(1, true, "<blue>MANAGER"),
    OWNER(2, true, "<purple>OWNER");

    public final int dbInt;
    public final boolean canManage;
    public final String displayName;

    GroupPermissionLevel(int dbInt, boolean canManage, String displayName) {
        this.dbInt = dbInt;
        this.canManage = canManage;
        this.displayName = displayName;
    }
}