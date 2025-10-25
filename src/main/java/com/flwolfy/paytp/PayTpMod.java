package com.flwolfy.paytp;

import com.flwolfy.paytp.command.PayTpBackManager;
import com.flwolfy.paytp.command.PayTpCommand;

import com.flwolfy.paytp.command.PayTpHomeManager;
import com.flwolfy.paytp.command.PayTpRequestManager;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.data.lang.PayTpLangManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.network.ServerPlayerEntity;

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
		// Init manager singletons
		PayTpConfigManager.getInstance();
		PayTpLangManager.getInstance();
		PayTpBackManager.getInstance();
		PayTpHomeManager.getInstance();
		PayTpRequestManager.getInstance();

		// Init command
		PayTpCommand.init();

		// Register events
		registerEvents();

		// Log complete
		LOGGER.info("PayTpMod initialized!");
	}

	private void registerEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			PayTpCommand.reload();
			PayTpCommand.register(server.getCommandManager().getDispatcher());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			PayTpBackManager.getInstance().clearHistory(handler.player);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, livingEntity) -> {
			if (entity instanceof ServerPlayerEntity player) {
				PayTpBackManager.getInstance().pushSingle(player, new PayTpData(player.getWorld().getRegistryKey(), player.getPos()));
			}
		});
	}
}