package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.data.PayTpData;
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

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    configManager = PayTpConfigManager.getInstance();
    backManager = PayTpBackManager.getInstance();
    requestManager = PayTpRequestManager.getInstance();
    homeManager = PayTpHomeManager.getInstance();

    dispatcher.register(CommandManager.literal(configManager.data().general().mainCommand())
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

    dispatcher.register(CommandManager.literal(configManager.data().back().backCommand())
        // ===== /ptpback =====
        .executes(PayTpCommand::payTpBack)
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().acceptCommand())
        // ===== /ptpaccept =====
        .executes(PayTpCommand::payTpAccept)
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().denyCommand())
        // ===== /ptpdeny =====
        .executes(PayTpCommand::payTpDeny)
    );

    dispatcher.register(CommandManager.literal(configManager.data().request().requestCommand().cancelCommand())
        // ===== /ptpcancel =====
        .executes(PayTpCommand::payTpCancel)
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

    PayTpData payTpData = new PayTpData(player.getServerWorld(), targetPos);
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

    PayTpData payTpData = new PayTpData(targetDim, targetPos);

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
      PayTpData targetTp = new PayTpData(target.getServerWorld(), target.getPos());

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
        configManager.data().request().requestCommand().acceptCommand(),
        configManager.data().request().requestCommand().denyCommand(),
        configManager.data().request().expireTime()
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

  private static int payTpBack(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    PayTpData targetTp = backManager.popLastTp(player);
    if (targetTp == null) {
      PayTpMessageSender.msgNoBack(player);
      return 0;
    }

    @SuppressWarnings("resource")
    int multiplierFlags = player.getServerWorld() == targetTp.world() ?
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

    PayTpHomeManager.PayTpHomeData home = homeManager.getHome(player);
    ServerWorld targetWorld = player.getServer().getWorld(home.dimension());
    if (targetWorld == null) return 0;

    PayTpData targetTp = new PayTpData(targetWorld, home.pos());

    int multiplierFlags = player.getServerWorld() == targetWorld ?
        Flags.combine(PayTpMultiplierFlags.HOME) :
        Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION, PayTpMultiplierFlags.HOME);

    int result = teleport(
        player,
        targetTp,
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
    ServerWorld fromWorld = player.getServerWorld();
    ServerWorld targetWorld = targetData.world();
    PayTpData fromData = new PayTpData(fromWorld, player.getPos());
    PayTpData toData = new PayTpData(targetWorld, targetData.pos());

    // ---------------------------------
    // Check payment
    // ---------------------------------
    int price = PayTpCalculator.calculatePrice(
        configManager.data().price().parameter().baseRadius(),
        configManager.data().price().parameter().rate(),
        configManager.data().calculateMultiplier(multiplierFlags),
        configManager.data().price().parameter().minPrice(),
        configManager.data().price().parameter().maxPrice(),
        fromData,
        toData
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
      backManager.pushPair(player, fromData, toData);
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
