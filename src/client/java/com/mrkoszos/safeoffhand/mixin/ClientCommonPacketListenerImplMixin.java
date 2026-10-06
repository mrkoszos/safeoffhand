package com.mrkoszos.safeoffhand.mixin;

import com.mrkoszos.safeoffhand.client.SafeOffhandClient;
import com.mrkoszos.safeoffhand.config.SafeOffhandConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {

	@Shadow
	public abstract void send(Packet<?> packet);

	@Inject(method = "send", at = @At("HEAD"), cancellable = true)
	private void safeoffhand$blockInGameOffhandSwap(Packet<?> packet, CallbackInfo ci) {
		if (!(packet instanceof ServerboundPlayerActionPacket actionPacket)) {
			return;
		}

		if (actionPacket.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
			return;
		}

		SafeOffhandConfig config = SafeOffhandClient.getConfig();

		if (!config.enabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		int hotbarSlot = client.player.getInventory().getSelectedSlot();

		if (!safeoffhand$shouldBlockSwap(config, hotbarSlot, this::send)) {
			return;
		}

		SafeOffhandClient.log("Blocked in-game offhand swap from hotbar slot " + (hotbarSlot + 1) + ".");
		SafeOffhandClient.showBlockedMessage(hotbarSlot);

		ci.cancel();
	}

	@Unique
	private static boolean safeoffhand$shouldBlockSwap(SafeOffhandConfig config, int hotbarSlot,
			Consumer<Packet<?>> send) {
		if (!config.enabled || hotbarSlot < 0 || hotbarSlot > 8) {
			return false;
		}
		if (!config.isHotbarSlotAllowed(hotbarSlot)) {
			return true;
		}
		// The swap has no slot index. Synchronize the checked slot on this same
		// connection before the original send continues, even if vanilla's cache agrees.
		send.accept(new ServerboundSetCarriedItemPacket(hotbarSlot));
		return false;
	}
}
