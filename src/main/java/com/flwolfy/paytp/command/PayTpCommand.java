package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpMultiplierFlags;
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

  private static PayTpConfigManager configManager;
  private static PayTpBackManager backManager;
  private static PayTpRequestManager requestManager;
  private static PayTpHomeManager homeManager;

  private PayTpCommand() {}

  public static void reload() {
    configManager = PayTpConfigManager.getInstance();
    backManager = PayTpBackManager.getInstance();
    requestManager = PayTpRequestManager.getInstance();
    homeManager = PayTpHomeManager.getInstance();

    // Language
    PayTpLangManager.getInstance().setLanguage(configManager.data().general().language());
    PayTpBackManager.getInstance().setMaxBackStack(configManager.data().back().maxBackStack());
  }

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(CommandManager.literal(configManager.data().general().mainCommand())
        // ===== /ptp (help) =====
        .executes(PayTpCommand::payTpHelp)
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

    dispatcher.register(CommandManager.literal(configManager.data().back().backCommand())
        // ===== /ptpback =====
        .executes(PayTpCommand::payTpBack)
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().toCommand())
        // ===== /ptpto <player> =====
        .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpPlayer))
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().hereCommand())
        // ===== /ptphere <player> =====
        .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpPlayerHere))
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().acceptCommand())
        // ===== /ptpaccept =====
        .executes(PayTpCommand::payTpAcceptLatest)
        // ===== /ptpaccept <player> =====
        .then(CommandManager.argument("sender", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpAccept))
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().denyCommand())
        // ===== /ptpdeny =====
        .executes(PayTpCommand::payTpDenyLatest)
        // ===== /ptpdeny <player> =====
        .then(CommandManager.argument("sender", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpDeny))
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().cancelCommand())
        // ===== /ptpcancel =====
        .executes(PayTpCommand::payTpCancelLatest)
        // ===== /ptpcancel <player> =====
        .then(CommandManager.argument("target", net.minecraft.command.argument.EntityArgumentType.player())
            .executes(PayTpCommand::payTpCancel))
    );

    dispatcher.register(CommandManager.literal(configManager.data().home().homeCommand())
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
        configManager.data().general().mainCommand(),
        configManager.data().general().mainCommand(),
        configManager.data().general().mainCommand(),
        configManager.data().back().backCommand(),
        configManager.data().request().requestCommand().acceptCommand(),
        configManager.data().request().requestCommand().denyCommand(),
        configManager.data().request().requestCommand().cancelCommand(),
        configManager.data().home().homeCommand(),
        configManager.data().home().homeCommand() + " set"
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCoords(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");

    if (player == null) return 0;

    PayTpData payTpData = new PayTpData(player.getServerWorld().getRegistryKey(), targetPos);
    return teleport(
        player,
        payTpData,
        true,
        Flags.NO_FLAG
    );
  }

  private static int payTpDimCoords(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    ServerWorld targetDim = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
    Vec3d targetPos = Vec3ArgumentType.getVec3(ctx, "pos");

    if (player == null) return 0;

    PayTpData payTpData = new PayTpData(targetDim.getRegistryKey(), targetPos);

    int multiplierFlags = player.getServerWorld() == targetDim ?
        Flags.NO_FLAG :
        Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION);

    return teleport(
        player,
        payTpData,
        true,
        multiplierFlags
    );
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
      PayTpData targetTp = new PayTpData(target.getServerWorld().getRegistryKey(), target.getPos());

      int multiplierFlags = sender.getServerWorld() == target.getServerWorld() ?
          Flags.NO_FLAG :
          Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION);

      int result = teleport(
          sender,
          targetTp,
          true,
          multiplierFlags
      );

      if (result == 1) {
        PayTpMessageSender.msgTpAccepted(target, sender.getName());
      } else {
        PayTpMessageSender.msgRequesterNotEnough(target);
      }

    }, () -> {
      PayTpMessageSender.msgCancelTp(target, sender.getName());
      PayTpMessageSender.msgTpCanceled(sender, target.getName());
    }, configManager.data().request().expireTime());

    PayTpMessageSender.msgTpRequestSent(sender, target.getName());
    PayTpMessageSender.msgTpRequestReceived(
        target,
        sender.getName(),
        configManager.data().request().requestCommand().acceptCommand() + " " + sender.getName().getString(),
        configManager.data().request().requestCommand().denyCommand() + " " + sender.getName().getString(),
        configManager.data().request().expireTime(),
        false
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpPlayerHere(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;
    PayTpData senderTp = new PayTpData(sender.getServerWorld().getRegistryKey(), sender.getPos());

    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
    if (target == null) {
      PayTpMessageSender.msgNoTargetFound(sender);
      return 0;
    }
    if (sender == target) {
      PayTpMessageSender.msgSelfTp(sender);
      return 0;
    }

    requestManager.sendRequest(sender, target, () -> {

      int multiplierFlags = sender.getServerWorld() == target.getServerWorld() ?
          Flags.NO_FLAG :
          Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION);

      teleport(
          target,
          senderTp,
          true,
          multiplierFlags
      );

    }, () -> {
      PayTpMessageSender.msgCancelTp(target, sender.getName());
      PayTpMessageSender.msgTpCanceled(sender, target.getName());
    }, configManager.data().request().expireTime());

    PayTpMessageSender.msgTpRequestSent(sender, target.getName());
    PayTpMessageSender.msgTpRequestReceived(
        target,
        sender.getName(),
        configManager.data().request().requestCommand().acceptCommand() + " " + sender.getName().getString(),
        configManager.data().request().requestCommand().denyCommand() + " " + sender.getName().getString(),
        configManager.data().request().expireTime(),
        true
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpAccept(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    ServerPlayerEntity sender = EntityArgumentType.getPlayer(ctx, "sender");
    if (!requestManager.accept(receiver, sender)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpAcceptLatest(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    if (!requestManager.acceptLatest(receiver)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeny(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    ServerPlayerEntity sender = EntityArgumentType.getPlayer(ctx, "sender");
    if (!requestManager.deny(receiver, sender)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDenyLatest(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    if (!requestManager.denyLatest(receiver)) {
      PayTpMessageSender.msgNoDenyRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCancel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;

    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
    if (!requestManager.cancel(sender, target)) {
      PayTpMessageSender.msgNoCancelRequest(sender);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCancelLatest(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;

    if (!requestManager.cancelLatest(sender)) {
      PayTpMessageSender.msgNoCancelRequest(sender);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpBack(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    PayTpData targetTp = backManager.popLastTp(player);
    if (targetTp == null) {
      PayTpMessageSender.msgNoBack(player);
      return 0;
    }

    int multiplierFlags = player.getServerWorld().getRegistryKey() == targetTp.world() ?
        Flags.combine(PayTpMultiplierFlags.BACK) :
        Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION, PayTpMultiplierFlags.BACK);

    int result = teleport(
        player,
        targetTp,
        false,
        multiplierFlags
    );

    if (result == 0) {
      backManager.pushSingle(player, targetTp);
    }

    return result;
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

    PayTpData home = homeManager.getHome(player);

    int multiplierFlags = player.getServerWorld().getRegistryKey() == home.world() ?
        Flags.combine(PayTpMultiplierFlags.HOME) :
        Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION, PayTpMultiplierFlags.HOME);

    int result = teleport(
        player,
        home,
        true,
        multiplierFlags
    );

    if (result == 1) {
      PayTpMessageSender.msgTpHome(player);
    }

    return result;
  }

  private static int payTpSetHome(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    homeManager.setHome(player);
    PayTpMessageSender.msgHomeSet(player);

    return Command.SINGLE_SUCCESS;
  }

  private static int teleport(
      ServerPlayerEntity player,
      PayTpData targetData,
      boolean recordToBackStack,
      int multiplierFlags
  ) {
    // ---------------------------------
    // Fetch teleport info
    // ---------------------------------
    MinecraftServer server = player.getServer();
    if (server == null) {
      LOGGER.error("Failed to teleport to null server");
      return 0;
    }

    ServerWorld targetWorld = server.getWorld(targetData.world());
    if (targetWorld == null) {
      LOGGER.error("Failed to teleport to null world");
      return 0;
    }

    ServerWorld fromWorld = player.getServerWorld();
    PayTpData fromData = new PayTpData(fromWorld.getRegistryKey(), player.getPos());

    // ---------------------------------
    // Check payment
    // ---------------------------------
    double distance = PayTpCalculator.calculateDistance(targetData, fromData);
    int price = PayTpCalculator.calculatePrice(
        distance,
        configManager.data().price().parameter().baseRadius(),
        configManager.data().price().parameter().rate(),
        configManager.data().calculateMultiplier(multiplierFlags),
        configManager.data().price().parameter().minPrice(),
        configManager.data().price().parameter().maxPrice()
    );

    int balance = PayTpCalculator.checkBalance(configManager.data().price().currencyItem(), player, configManager.data().combineSettingFlags());
    if (balance < price) {
      PayTpMessageSender.msgTpFailed(
          player,
          PayTpItemHandler.getItemByStringId(configManager.data().price().currencyItem()).getName(),
          price,
          balance
      );
      return 0;
    }

    // ---------------------------------
    // Record to back stack
    // ---------------------------------
    if (recordToBackStack) {
      backManager.pushPair(player, fromData, targetData);
    }

    // ---------------------------------
    // Proceed payment
    // ---------------------------------
    if (!PayTpCalculator.proceedPayment(configManager.data().price().currencyItem(), player, price, configManager.data().combineSettingFlags())) {
      LOGGER.error("Payment proceed failed");
      return 0;
    }

    // ---------------------------------
    // Execute teleport
    // ---------------------------------
    TeleportTarget teleportTarget = new TeleportTarget(
        targetWorld,
        targetData.pos(),
        player.getVelocity(),
        player.getYaw(),
        player.getPitch(),
        entity -> {
          // Effect
          if (configManager.data().setting().effect().particleEffect()) {
            targetWorld.sendEntityStatus(player, (byte)46);
          }

          // Message
          if (Flags.check(multiplierFlags, PayTpMultiplierFlags.BACK)) {
            PayTpMessageSender.msgTpBackSucceeded(
                player,
                PayTpItemHandler.getItemByStringId(configManager.data().price().currencyItem()).getName(),
                price
            );
          } else {
            PayTpMessageSender.msgTpSucceeded(
                player,
                PayTpItemHandler.getItemByStringId(configManager.data().price().currencyItem()).getName(),
                price
            );
          }
        }
    );

    player.teleportTo(teleportTarget);
    return Command.SINGLE_SUCCESS;
  }

}
