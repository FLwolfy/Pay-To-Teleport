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
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

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
        // ===== /ptp <dimension> <pos> =====
        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                .executes(PayTpCommand::payTpDimCoords)
            )
        )
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
      return teleport(player, targetPos, player.getServerWorld());
    }
    return 0;
  }

  private static int payTpDimCoords(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player != null) {
      ServerWorld targetDim = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
      Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");
      return teleport(player, targetPos, targetDim);
    }
    return 0;
  }

  private static int payTpPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");

    if (sender != null && target != null && sender != target) {
      requestManager.sendRequest(sender, target, () -> {
        if (teleport(sender, target.getPos(), target.getServerWorld()) == 0) {
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

  private static int teleport(ServerPlayerEntity player, Vec3d targetPos, ServerWorld targetWorld) {
    int price = PayTpCalculator.calculatePrice(
        configData.baseRadius(),
        configData.rate(),
        configData.crossDimMultiplier(),
        configData.minPrice(),
        configData.maxPrice(),
        player.getPos(),
        targetPos,
        player.getServerWorld().getRegistryKey(),
        targetWorld.getRegistryKey()
    );

    int balance = PayTpCalculator.checkBalance(configData.currencyItem(), player, configData.flags());

    if (balance < price) {
      PayTpMessageHandler.msgTpFailed(
          player,
          PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
          price,
          balance
      );
      return 0;
    }

    if (!PayTpCalculator.proceedPayment(configData.currencyItem(), player, price, configData.flags())) {
      LOGGER.error("Payment proceed failed");
      return 0;
    }

    TeleportTarget teleportTarget = new TeleportTarget(
        targetWorld,
        targetPos,
        player.getVelocity(),
        player.getYaw(),
        player.getPitch(),
        entity -> {
          targetWorld.sendEntityStatus(player, (byte)46);
          PayTpMessageHandler.msgTpSucceeded(
              player,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
              price
          );
        }
    );

    player.teleportTo(teleportTarget);
    return Command.SINGLE_SUCCESS;
  }

}
