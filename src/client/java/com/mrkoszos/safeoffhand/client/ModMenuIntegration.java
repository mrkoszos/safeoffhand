package com.mrkoszos.safeoffhand.client;

import com.mrkoszos.safeoffhand.config.SafeOffhandConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
		return SafeOffhandConfigScreen::create;
	}
}