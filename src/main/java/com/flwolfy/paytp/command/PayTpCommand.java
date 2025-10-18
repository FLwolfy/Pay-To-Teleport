package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.config.PayTpConfig;
import com.flwolfy.paytp.config.PayTpConfigData;
import com.flwolfy.paytp.util.PayTpCalculator;

import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpMessageHandler;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

public class PayTpCommand {

  private static final Logger LOGGER = PayTpMod.LOGGER;

  private static PayTpConfigData configData;
  private static PayTpRequest requestManager;

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    configData = PayTpConfig.getInstance().data();
    requestManager = PayTpRequest.getInstance();

    PayTpMessageHandler.changeLanguage(configData.language());

    dispatcher.register(CommandManager.literal(configData.commandName())
        // ===== /ptp <pos> =====
        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
            .executes(PayTpCommand::payTpCoords))
        // ===== /ptp <player> =====
        .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpPlayer))
    );

    dispatcher.register(CommandManager.literal(configData.acceptName())
        // ===== /ptpa =====
        .executes(PayTpCommand::payTpAccept)
    );

    dispatcher.register(CommandManager.literal(configData.cancelName())
        // ===== /ptpc =====
        .executes(PayTpCommand::payTpCancel)
    );
  }

  private static int payTpCoords(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player != null) {
      Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");
      return teleport(player, targetPos);
    }
    return 0;
  }

  private static int payTpPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");

    if (sender != null && target != null && sender != target) {
      requestManager.sendRequest(sender, target, () -> {
        if (teleport(sender, target.getPos()) == 0) {
          PayTpMessageHandler.msgRequesterNotEnough(target);
        }
      }, () -> {
        PayTpMessageHandler.msgTpCanceled(sender);
        PayTpMessageHandler.msgTpCanceled(target);
      }, configData.expireTime());

      PayTpMessageHandler.msgTpRequestSent(sender, target.getName().getString());
      PayTpMessageHandler.msgTpRequestReceived(
          target,
          sender.getName().getString(),
          configData.acceptName(),
          configData.cancelName(),
          configData.expireTime()
      );
    } else if (sender != null) {
      PayTpMessageHandler.msgNoTargetFound(sender);
    }

    return 0;
  }

  private static int payTpAccept(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver != null) {
      if (!requestManager.accept(receiver)) {
        PayTpMessageHandler.msgNoAcceptRequest(receiver);
        return 0;
      }
      return Command.SINGLE_SUCCESS;
    }
    return 0;
  }

  private static int payTpCancel(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver != null) {
      if (requestManager.cancel(receiver)) {
        return Command.SINGLE_SUCCESS;
      } else {
        PayTpMessageHandler.msgNoCancelRequest(receiver);
        return 0;
      }
    }
    return 0;
  }

  private static int teleport(ServerPlayerEntity player, Vec3d to) {
    int price = PayTpCalculator.calculatePrice(configData.baseRadius(), configData.rate(), configData.minPrice(), configData.maxPrice(), player.getPos(), to);
    int balance = PayTpCalculator.checkBalance(configData.currencyItem(), player, configData.flags());

    if (balance >= price) {
      if (!PayTpCalculator.proceedPayment(configData.currencyItem(), player, price, configData.flags())) {
        LOGGER.error("Payment proceed failed");
      }
      player.requestTeleport(to.x, to.y, to.z);
      PayTpMessageHandler.msgTpSucceeded(
          player,
          PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
          price
      );
    } else {
      PayTpMessageHandler.msgTpFailed(
          player,
          PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
          price,
          balance
      );
    }

    return balance >= price ? Command.SINGLE_SUCCESS : 0;
  }
}
