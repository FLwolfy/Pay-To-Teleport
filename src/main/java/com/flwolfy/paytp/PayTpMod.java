package com.flwolfy.paytp;

import com.flwolfy.paytp.command.back.PayTpBackManager;
import com.flwolfy.paytp.command.PayTpCommand;
import com.flwolfy.paytp.command.home.PayTpHomeManager;
import com.flwolfy.paytp.command.warp.PayTpWarpManager;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.util.PayTpMessageSender;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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
			PayTpCommand.register(server.getCommands().getDispatcher());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
      PayTpBackManager.getInstance().clearHistory(handler.player);
    });

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, livingEntity) -> {
			if (entity instanceof ServerPlayer player) {
				PayTpBackManager.getInstance().pushSingle(player, new PayTpData(player.level().dimension(), player.position()));
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register(
				PayTpHomeManager.getInstance()::handleRespawn
		);

		ServerTickEvents.END_LEVEL_TICK.register(world -> {
			if (!world.dimension().equals(Level.OVERWORLD)) return;
			PayTpWarpManager.getInstance().checkWarpState(world.getServer(), name -> {
				for (ServerPlayer onlinePlayer : world.getServer().getPlayerList().getPlayers()) {
					PayTpMessageSender.msgWarpDeletedServer(
							onlinePlayer,
							name
					);
				}
			});
		});
	}
}
