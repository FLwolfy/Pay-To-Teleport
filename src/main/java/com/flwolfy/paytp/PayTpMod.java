package com.flwolfy.paytp;

import com.flwolfy.paytp.command.PayTpCommand;

import com.flwolfy.paytp.command.PayTpRequest;
import com.flwolfy.paytp.config.PayTpConfig;
import com.flwolfy.paytp.config.PayTpLang;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayTpMod implements ModInitializer {

	public static final String MOD_ID = "pay-to-teleport";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * This code runs as soon as Minecraft is in a mod-load-ready state.
	 * However, some things (like resources) may still be uninitialized.
	 * Proceed with mild caution.
	 */
	@Override
	public void onInitialize() {
		// Init singletons
		PayTpRequest.getInstance();
		PayTpConfig.getInstance();
		PayTpLang.getInstance();

		// Register command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			PayTpCommand.register(dispatcher);
		});

		// Log complete
		LOGGER.info("PayTpMod initialized!");
	}
}