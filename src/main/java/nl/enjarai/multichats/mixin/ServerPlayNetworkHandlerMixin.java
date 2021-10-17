package nl.enjarai.multichats.mixin;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import nl.enjarai.multichats.ChatHelpers;
import nl.enjarai.multichats.ConfigManager;
import nl.enjarai.multichats.MultiChats;
import nl.enjarai.multichats.PlayerChatTracker;
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

	@Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", shift = At.Shift.BEFORE), cancellable = true)
	public void broadcastChatMessage(ChatMessageC2SPacket packet, CallbackInfo info) {
		String message = packet.getChatMessage();

		// Check for prefixes
		for (Map.Entry<String, ConfigManager.Chat> entry : MultiChats.CONFIG.chats.entrySet()) {
			ConfigManager.Chat chat = entry.getValue();
			if (message.startsWith(chat.prefix) && Permissions.check(player, "multichats.chat." + entry.getKey())) {
				ChatHelpers.sendToChat(chat, player, message.substring(chat.prefix.length()));
				info.cancel();
				return;
			}
		}

		// Check what chat the player is in, then forward to that
		ConfigManager.Chat chat = PlayerChatTracker.isInChat(player);
		if (!message.startsWith("/") && chat != null) {
			ChatHelpers.sendToChat(chat, player, message);
			info.cancel();
		}
	}
}
