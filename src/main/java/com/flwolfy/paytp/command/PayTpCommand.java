package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.config.PayTpConfigManager;
import com.flwolfy.paytp.config.PayTpConfigData;
import com.flwolfy.paytp.util.PayTpCalculator;

import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpMessageSender;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.MinecraftServer;
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
  private static PayTpRequestManager requestManager;
  private static PayTpHomeManager homeManager;

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    configData = PayTpConfigManager.getInstance().data();
    requestManager = PayTpRequestManager.getInstance();
    homeManager = PayTpHomeManager.getInstance();

    PayTpMessageSender.changeLanguage(configData.language());

    dispatcher.register(CommandManager.literal(configData.commandName())
        // ===== /ptp (help) =====
        .executes(PayTpCommand::payTpHelp)
        // ===== /ptp <player> =====
        .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpPlayer))
        // ===== /ptp <pos> =====
        .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
            .executes(PayTpCommand::payTpCoords))
        // ===== /ptp <dimension> <pos> =====
        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                .executes(PayTpCommand::payTpDimCoords)
            )
        )
    );

    dispatcher.register(CommandManager.literal(configData.acceptName())
        // ===== /ptpa =====
        .executes(PayTpCommand::payTpAccept)
    );

    dispatcher.register(CommandManager.literal(configData.denyName())
        // ===== /ptpd =====
        .executes(PayTpCommand::payTpDeny)
    );

    dispatcher.register(CommandManager.literal(configData.cancelName())
        // ===== /ptpc =====
        .executes(PayTpCommand::payTpCancel)
    );

    dispatcher.register(CommandManager.literal(configData.homeName())
        // ===== /ptphome =====
        .executes(PayTpCommand::payTpHome)
        // ===== /ptphome set =====
        .then(CommandManager.literal("set")
            .executes(PayTpCommand::payTpSetHome))
    );
  }

  private static int payTpHelp(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    PayTpMessageSender.msgHelp(
        player,
        configData.commandName(),
        configData.commandName(),
        configData.commandName(),
        configData.acceptName(),
        configData.denyName(),
        configData.cancelName(),
        configData.homeName(),
        configData.homeName() + " set"
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCoords(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");
    return teleport(player, targetPos, player.getServerWorld(), false);
  }

  private static int payTpDimCoords(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    ServerWorld targetDim = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
    Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");
    return teleport(player, targetPos, targetDim, false);
  }

  private static int payTpPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
    if (sender == null) return 0;
    if (target == null) {
      PayTpMessageSender.msgNoTargetFound(sender);
      return 0;
    }
    if (sender == target) {
      PayTpMessageSender.msgSelfTp(sender);
      return 0;
    }

    requestManager.sendRequest(sender, target, () -> {
      if (teleport(sender, target.getPos(), target.getServerWorld(), false) == 1) {
        PayTpMessageSender.msgTpAccepted(target, sender.getName());
      } else {
        PayTpMessageSender.msgRequesterNotEnough(target);
      }
    }, () -> {
      PayTpMessageSender.msgCancelTp(target, sender.getName());
      PayTpMessageSender.msgTpCanceled(sender, target.getName());
    }, configData.expireTime());

    PayTpMessageSender.msgTpRequestSent(sender, target.getName());
    PayTpMessageSender.msgTpRequestReceived(
        target,
        sender.getName(),
        configData.acceptName(),
        configData.denyName(),
        configData.expireTime()
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpAccept(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    if (!requestManager.accept(receiver)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeny(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    if (!requestManager.cancelByTarget(receiver)) {
      PayTpMessageSender.msgNoDenyRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCancel(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;

    if (!requestManager.cancelBySender(sender)) {
      PayTpMessageSender.msgNoCancelRequest(sender);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpHome(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;
    MinecraftServer server = player.getServer();
    if (server == null) return 0;

    if (!homeManager.hasHome(player)) {
      PayTpMessageSender.msgHomeNotSet(player);
      return 0;
    }

    PayTpHomeManager.PayTpHomeData home = homeManager.getHome(player);
    ServerWorld targetWorld = player.getServer().getWorld(home.dimension());
    if (targetWorld == null) return 0;

    int result = teleport(player, home.pos(), targetWorld, true);
    PayTpMessageSender.msgTpHome(player);

    return result;
  }

  private static int payTpSetHome(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    homeManager.setHome(player);
    PayTpMessageSender.msgHomeSet(player);

    return Command.SINGLE_SUCCESS;
  }

  private static int teleport(ServerPlayerEntity player, Vec3d targetPos, ServerWorld targetWorld, boolean home) {
    int price = PayTpCalculator.calculatePrice(
        configData.baseRadius(),
        configData.rate(),
        configData.crossDimMultiplier(),
        home ? configData.homeMultiplier() : 1,
        configData.minPrice(),
        configData.maxPrice(),
        player.getPos(),
        targetPos,
        player.getServerWorld().getRegistryKey(),
        targetWorld.getRegistryKey()
    );

    int balance = PayTpCalculator.checkBalance(configData.currencyItem(), player, configData.flags());

    if (balance < price) {
      PayTpMessageSender.msgTpFailed(
          player,
          PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName(),
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
          PayTpMessageSender.msgTpSucceeded(
              player,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName(),
              price
          );
        }
    );

    player.teleportTo(teleportTarget);
    return Command.SINGLE_SUCCESS;
  }

}
