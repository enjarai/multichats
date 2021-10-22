package nl.enjarai.multichats.mixin;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import nl.enjarai.multichats.Helpers;
import nl.enjarai.multichats.MultiChats;
import nl.enjarai.multichats.PlayerChatTracker;
import nl.enjarai.multichats.types.Group;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;
	@Shadow
	private MinecraftServer server;

	@Shadow private int messageCooldown;

	@Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", shift = At.Shift.BEFORE), cancellable = true)
	public void broadcastChatMessage(ChatMessageC2SPacket packet, CallbackInfo info) {
		String message = packet.getChatMessage();

//		// Check for global prefix TODO
//		if (message.startsWith(MultiChats.CONFIG.globalPrefix)) {
//			ChatMessageC2SPacket test = new ChatMessageC2SPacket("64w6");
//			return;
//		}

		// Check for prefixes

		for (Group group : MultiChats.DATABASE.getGroups(player.getUuid())) {
			if (group.prefix != null && message.toLowerCase().startsWith(group.prefix)) {
				Helpers.sendToChat(group, player, message.substring(group.prefix.length()));
				info.cancel();
				return;
			}
		}

		// Check what group the player is in, then forward to that
		Group group = PlayerChatTracker.isInChat(player);
		if (!message.startsWith("/") && group != null) {
			Helpers.sendToChat(group, player, message);
			info.cancel();
		}
	}
}
