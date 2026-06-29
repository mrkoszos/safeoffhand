package com.mrkoszos.safeoffhand.config;

import com.mrkoszos.safeoffhand.client.SafeOffhandClient;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SafeOffhandConfigScreen {
	private static final boolean SHOW_DEVELOPER_OPTIONS = false;

	public static Screen create(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.literal("Safe Offhand Config"));

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();

		ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
		ConfigCategory hotbar = builder.getOrCreateCategory(Component.literal("Hotbar Slots"));

		SafeOffhandConfig config = AutoConfig
				.getConfigHolder(SafeOffhandConfig.class)
				.getConfig();

		general.addEntry(
				entryBuilder.startBooleanToggle(
								Component.literal("Enable mod"),
								config.enabled
						)
						.setDefaultValue(true)
						.setTooltip(Component.literal("Enables Safe Offhand."))
						.setSaveConsumer(value -> config.enabled = value)
						.build()
		);

		general.addEntry(
				entryBuilder.startBooleanToggle(
								Component.literal("Show actionbar messages"),
								config.showActionbarMessages
						)
						.setDefaultValue(false)
						.setTooltip(Component.literal("Shows an actionbar message when an offhand swap is blocked."))
						.setSaveConsumer(value -> config.showActionbarMessages = value)
						.build()
		);

		if (SHOW_DEVELOPER_OPTIONS) {
			general.addEntry(
					entryBuilder.startBooleanToggle(
									Component.literal("Developer logs"),
									config.developerLogs
							)
							.setDefaultValue(false)
							.setTooltip(Component.literal("Prints extra Safe Offhand debug messages to the game log."))
							.setSaveConsumer(value -> config.developerLogs = value)
							.build()
			);
		}

		for (int i = 0; i < 9; i++) {
			final int hotbarSlot = i;
			final int displaySlot = i + 1;

			hotbar.addEntry(
					entryBuilder.startEnumSelector(
									Component.literal("Slot " + displaySlot),
									SlotState.class,
									config.isHotbarSlotAllowed(hotbarSlot) ? SlotState.UNLOCKED : SlotState.LOCKED
							)
							.setDefaultValue(SlotState.LOCKED)
							.setEnumNameProvider(value -> ((SlotState) value).getDisplayName())
							.setTooltip(
									Component.literal("Locked prevents this slot from swapping to the offhand."),
									Component.literal("Unlocked allows this slot to swap to the offhand.")
							)
							.setSaveConsumer(value ->
									config.setHotbarSlotAllowed(hotbarSlot, value == SlotState.UNLOCKED)
							)
							.build()
			);
		}

		builder.setSavingRunnable(SafeOffhandClient::saveConfig);

		return builder.build();
	}

	private enum SlotState {
		LOCKED,
		UNLOCKED;

		private Component getDisplayName() {
			return switch (this) {
				case LOCKED -> Component.literal("Locked").withStyle(ChatFormatting.RED);
				case UNLOCKED -> Component.literal("Unlocked").withStyle(ChatFormatting.GREEN);
			};
		}
	}
}