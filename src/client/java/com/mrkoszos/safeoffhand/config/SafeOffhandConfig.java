package com.mrkoszos.safeoffhand.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "safeoffhand")
public class SafeOffhandConfig implements ConfigData {

	@ConfigEntry.Gui.Tooltip
	public boolean enabled = true;

	@ConfigEntry.Gui.Tooltip
	public boolean showActionbarMessages = false;

	public boolean developerLogs = false;

	/*
	 * Bitmask for unlocked hotbar slots.
	 *
	 * Slot 1 = bit 0
	 * Slot 2 = bit 1
	 * Slot 3 = bit 2
	 * Slot 4 = bit 3
	 * Slot 5 = bit 4
	 * Slot 6 = bit 5
	 * Slot 7 = bit 6
	 * Slot 8 = bit 7
	 * Slot 9 = bit 8
	 *
	 * 0 means every hotbar slot is locked.
	 */
	public int unlockedHotbarSlots = 0;

	public boolean isHotbarSlotAllowed(int hotbarSlot) {
		if (hotbarSlot < 0 || hotbarSlot > 8) {
			return false;
		}

		return (unlockedHotbarSlots & (1 << hotbarSlot)) != 0;
	}

	public void setHotbarSlotAllowed(int hotbarSlot, boolean allowed) {
		if (hotbarSlot < 0 || hotbarSlot > 8) {
			return;
		}

		if (allowed) {
			unlockedHotbarSlots |= (1 << hotbarSlot);
		} else {
			unlockedHotbarSlots &= ~(1 << hotbarSlot);
		}
	}
}