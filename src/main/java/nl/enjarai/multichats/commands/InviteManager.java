package nl.enjarai.multichats.commands;

import net.minecraft.server.network.ServerPlayerEntity;
import nl.enjarai.multichats.types.Group;

import java.util.HashMap;
import java.util.UUID;

public class InviteManager {
    private final HashMap<UUID, Invite> invites = new HashMap<>();

    public void putInvite(UUID uuid, Group group, ServerPlayerEntity from) {
        invites.put(uuid, new Invite(group, from));
    }

    public Invite hasInvites(UUID uuid) {
        Invite invite = invites.get(uuid);
        if (invite != null) {
            invite.group = invite.group.refresh();
            invites.remove(uuid);
        }
        return invite;
    }

    public static class Invite {
        public Group group;
        public ServerPlayerEntity from;
        public long time;

        public Invite(Group group, ServerPlayerEntity from) {
            this.group = group;
            this.from = from;
            this.time = System.currentTimeMillis() / 1000L;
        }
    }
}
