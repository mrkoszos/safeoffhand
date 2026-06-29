package com.mrkoszos.safeoffhand.mixin;

import com.mrkoszos.safeoffhand.client.SafeOffhandClient;
import com.mrkoszos.safeoffhand.config.SafeOffhandConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {

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

		if (hotbarSlot < 0 || hotbarSlot > 8) {
			return;
		}

		if (config.isHotbarSlotAllowed(hotbarSlot)) {
			return;
		}

		SafeOffhandClient.log("Blocked in-game offhand swap from hotbar slot " + (hotbarSlot + 1) + ".");
		SafeOffhandClient.showBlockedMessage(hotbarSlot);

		ci.cancel();
	}
}