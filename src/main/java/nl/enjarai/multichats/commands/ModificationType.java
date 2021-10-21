package nl.enjarai.multichats.commands;

import nl.enjarai.multichats.MultiChats;

public enum ModificationType {
    SET_OWNER("player", false, MultiChats.CONFIG.messages.modifyOwner),
    ADD_MANAGER("player", false, MultiChats.CONFIG.messages.modifyAddManager),
    REMOVE_MANAGER("player", false, MultiChats.CONFIG.messages.modifyRemoveManager),
    PREFIX("string", false, MultiChats.CONFIG.messages.modifyPrefix),
    DISPLAY_NAME("string", true, MultiChats.CONFIG.messages.modifyDisplayName),
    DISPLAY_NAME_SHORT("string", true, MultiChats.CONFIG.messages.modifyDisplayNameShort);

    public final String argumentType;
    public final boolean formatStringArg;
    public final String message;

    ModificationType(String argumentType, boolean formatStringArg, String message) {

        this.argumentType = argumentType;
        this.formatStringArg = formatStringArg;
        this.message = message;
    }
}
