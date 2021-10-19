package nl.enjarai.multichats.types;

public enum GroupPermissionLevel {
    MEMBER(0, false),
    MANAGER(1, true),
    OWNER(2, true);

    public final int dbInt;
    public final boolean canManage;

    GroupPermissionLevel(int dbInt, boolean canManage) {
        this.dbInt = dbInt;
        this.canManage = canManage;
    }
}