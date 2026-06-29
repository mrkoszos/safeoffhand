package com.mrkoszos.safeoffhand.client;

import com.mrkoszos.safeoffhand.config.SafeOffhandConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeOffhandClient implements ClientModInitializer {
	public static final String MOD_ID = "safeoffhand";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		AutoConfig.register(SafeOffhandConfig.class, GsonConfigSerializer::new);

		log("Safe Offhand client initialized.");
	}

	public static SafeOffhandConfig getConfig() {
		return AutoConfig.getConfigHolder(SafeOffhandConfig.class).getConfig();
	}

	public static void saveConfig() {
		AutoConfig.getConfigHolder(SafeOffhandConfig.class).save();
	}

	public static void showBlockedMessage(int hotbarSlot) {
		SafeOffhandConfig config = getConfig();

		if (!config.showActionbarMessages) {
			return;
		}

		Minecraft client = Minecraft.getInstance();

		if (client.player == null) {
			return;
		}

		Component message = Component
				.literal("Safe Offhand: Slot " + (hotbarSlot + 1) + " is locked")
				.withStyle(ChatFormatting.RED);

		client.player.connection.setActionBarText(
				new ClientboundSetActionBarTextPacket(message)
		);
	}

	public static void log(String message) {
		if (getConfig().developerLogs) {
			LOGGER.info(message);
		}
	}
}