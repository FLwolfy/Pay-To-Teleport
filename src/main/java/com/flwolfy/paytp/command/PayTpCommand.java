package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.PayTpTeleportType;
import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.flwolfy.paytp.util.PayTpCalculator;
import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpMessageSender;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

public class PayTpCommand {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static PayTpConfigManager configManager;
  private static PayTpLangManager langManager;
  private static PayTpBackManager backManager;
  private static PayTpRequestManager requestManager;
  private static PayTpHomeManager homeManager;
  private static PayTpWarpManager warpManager;

  private static PayTpConfigData configData;

  private PayTpCommand() {}

  /**
   * Resolves and stores the managers required by command handlers.
   */
  public static void init() {
    // Init manager singletons
    configManager = PayTpConfigManager.getInstance();
    langManager = PayTpLangManager.getInstance();
    backManager = PayTpBackManager.getInstance();
    requestManager = PayTpRequestManager.getInstance();
    homeManager = PayTpHomeManager.getInstance();
    warpManager = PayTpWarpManager.getInstance();
  }

  /**
   * Reloads configuration and propagates configurable values to dependent managers.
   */
  public static void reload() {
    configManager.reload();

    // Config data
    configData = configManager.data();

    // Config content
    langManager.setLanguage(configData.general().language());
    backManager.setMaxBackStack(configData.back().maxBackStack());
    warpManager.resetTimers();
    warpManager.setMaxInactiveTicks(configData.warp().maxInactiveTicks());
    warpManager.setCheckPeriodTicks(configData.warp().checkPeriodTicks());
  }

  /**
   * Registers every enabled PayTp command with Brigadier.
   *
   * @param dispatcher the server command dispatcher
   */
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // ===== /ptphelp =====
    String helpCmd = configData.general().helpCommand();
    if (!helpCmd.isEmpty()) {
      dispatcher.register(Commands.literal(helpCmd)
          .executes(PayTpCommand::payTpHelp)
      );
    }

    // ===== /ptp (dimension) <pos> =====
    String mainCmd = configData.teleport().coordinateCommand();
    if (!mainCmd.isEmpty()) {
      LiteralArgumentBuilder<CommandSourceStack> coordinateCommand =
          Commands.literal(mainCmd)
              .then(Commands.argument("pos", Vec3Argument.vec3())
                  .executes(PayTpCommand::payTpCoords));

      if (configData.teleport().allowCrossDim()) {
        coordinateCommand.then(Commands.argument("dimension", DimensionArgument.dimension())
            .then(Commands.argument("pos", Vec3Argument.vec3())
                .executes(PayTpCommand::payTpDimCoords)
            )
        );
      }
      dispatcher.register(coordinateCommand);
    }

    // ===== /ptpback =====
    String backCmd = configData.back().backCommand();
    if (!backCmd.isEmpty()) {
      dispatcher.register(Commands.literal(backCmd)
          .executes(PayTpCommand::payTpBack)
      );
    }

    // ===== /ptpto <player> =====
    String tpToCmd = configData.request().requestCommand().toCommand();
    if (!tpToCmd.isEmpty()) {
      dispatcher.register(Commands.literal(tpToCmd)
          .then(Commands.argument("target", EntityArgument.player())
              .executes(PayTpCommand::payTpPlayer))
      );
    }

    // ===== /ptphere <player> =====
    String tpHereCmd = configData.request().requestCommand().hereCommand();
    if (!tpHereCmd.isEmpty()) {
      dispatcher.register(Commands.literal(tpHereCmd)
          .then(Commands.argument("target", EntityArgument.player())
              .executes(PayTpCommand::payTpPlayerHere))
      );
    }

    // ===== /ptpaccept (player) =====
    String acceptCmd = configData.request().requestCommand().acceptCommand();
    if (!acceptCmd.isEmpty()) {
      dispatcher.register(Commands.literal(acceptCmd)
          .executes(PayTpCommand::payTpAcceptLatest)
          .then(Commands.argument("sender", EntityArgument.player())
              .executes(PayTpCommand::payTpAccept))
      );
    }

    // ===== /ptpdeny (player) =====
    String denyCmd = configData.request().requestCommand().denyCommand();
    if (!denyCmd.isEmpty()) {
      dispatcher.register(Commands.literal(denyCmd)
          .executes(PayTpCommand::payTpDenyLatest)
          .then(Commands.argument("sender", EntityArgument.player())
              .executes(PayTpCommand::payTpDeny))
      );
    }

    // ===== /ptpcancel (player) =====
    String cancelCmd = configData.request().requestCommand().cancelCommand();
    if (!cancelCmd.isEmpty()) {
      dispatcher.register(Commands.literal(cancelCmd)
          .executes(PayTpCommand::payTpCancelLatest)
          .then(Commands.argument("target", EntityArgument.player())
              .executes(PayTpCommand::payTpCancel))
      );
    }

    // ===== /ptphome =====
    String homeCmd = configData.home().homeCommand();
    if (!homeCmd.isEmpty()) {
      dispatcher.register(Commands.literal(homeCmd)
          .executes(PayTpCommand::payTpHome)
          .then(Commands.literal("set")
              .executes(PayTpCommand::payTpSetHome))
      );
    }

    // ===== /ptpwarp =====
    String warpCmd = configData.warp().warpCommand();
    dispatcher.register(Commands.literal(warpCmd)
        .then(Commands.literal("create")
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(PayTpCommand::payTpCreateWarp)
            )
        )
        .then(Commands.literal("delete")
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .executes(PayTpCommand::payTpDeleteWarp)
            )
        )
        .then(Commands.literal("list")
            .executes(ctx -> payTpListWarp(ctx, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> payTpListWarp(ctx, IntegerArgumentType.getInteger(ctx, "page")))
            )
        )
        .then(Commands.argument("name", StringArgumentType.greedyString())
            .suggests(PayTpCommand::payTpWarpSuggest)
            .executes(PayTpCommand::payTpWarp)
        )
    );

  }

  private static int payTpHelp(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    PayTpMessageSender.msgHelp(
        player,
        configData.teleport().coordinateCommand(),
        configData.teleport().allowCrossDim(),
        configData.back().backCommand(),
        configData.request().requestCommand().toCommand(),
        configData.request().requestCommand().hereCommand(),
        configData.request().requestCommand().acceptCommand(),
        configData.request().requestCommand().denyCommand(),
        configData.request().requestCommand().cancelCommand(),
        configData.home().homeCommand(),
        configData.home().homeCommand().isEmpty() ? "" : configData.home().homeCommand() + " set",
        configData.warp().warpCommand(),
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " create",
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " delete",
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " list"
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCoords(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    Vec3 targetPos = Vec3Argument.getVec3(ctx, "pos");

    if (player == null) return 0;

    PayTpData payTpData = new PayTpData(player.level().dimension(), targetPos);
    return teleport(
        player,
        payTpData,
        true,
        PayTpTeleportType.COORDINATE,
        ""
    ).commandResult();
  }

  private static int payTpDimCoords(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayer();
    ServerLevel targetDim = DimensionArgument.getDimension(ctx, "dimension");
    Vec3 targetPos = Vec3Argument.getVec3(ctx, "pos");

    if (player == null) return 0;

    PayTpData payTpData = new PayTpData(targetDim.dimension(), targetPos);

    return teleport(
        player,
        payTpData,
        true,
        PayTpTeleportType.COORDINATE,
        ""
    ).commandResult();
  }

  private static int payTpPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer sender = ctx.getSource().getPlayer();
    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

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
      PayTpData targetTp = new PayTpData(target.level().dimension(), target.position());

      TeleportResult result = teleport(
          sender,
          targetTp,
          true,
          PayTpTeleportType.REQUEST,
          target.getName().getString()
      );

      if (result == TeleportResult.SUCCESS) {
        PayTpMessageSender.msgTpAccepted(target, sender.getName());
      } else if (result == TeleportResult.INSUFFICIENT_FUNDS) {
        PayTpMessageSender.msgRequesterNotEnough(target);
      }

    }, () -> {
      PayTpMessageSender.msgCancelTp(target, sender.getName());
      PayTpMessageSender.msgTpCanceled(sender, target.getName());
    }, configData.request().expireTime());

    PayTpMessageSender.msgTpRequestSent(sender, target.getName());
    PayTpMessageSender.msgTpRequestReceived(
        target,
        sender.getName(),
        configData.request().requestCommand().acceptCommand() + " " + sender.getName().getString(),
        configData.request().requestCommand().denyCommand() + " " + sender.getName().getString(),
        configData.request().expireTime(),
        false
    );

    if (configData.general().effect().soundEffect()) {
      target.level().playSound(
          null,
          target,
          SoundEvents.PLAYER_LEVELUP,
          SoundSource.PLAYERS,
          1.0f,
          2.0f
      );
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpPlayerHere(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;
    PayTpData senderTp = new PayTpData(sender.level().dimension(), sender.position());

    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
    if (target == null) {
      PayTpMessageSender.msgNoTargetFound(sender);
      return 0;
    }
    if (sender == target) {
      PayTpMessageSender.msgSelfTp(sender);
      return 0;
    }

    requestManager.sendRequest(sender, target, () -> {

      teleport(
          target,
          senderTp,
          true,
          PayTpTeleportType.REQUEST,
          sender.getName().getString()
      );

    }, () -> {
      PayTpMessageSender.msgCancelTp(target, sender.getName());
      PayTpMessageSender.msgTpCanceled(sender, target.getName());
    }, configData.request().expireTime());

    PayTpMessageSender.msgTpRequestSent(sender, target.getName());
    PayTpMessageSender.msgTpRequestReceived(
        target,
        sender.getName(),
        configData.request().requestCommand().acceptCommand() + " " + sender.getName().getString(),
        configData.request().requestCommand().denyCommand() + " " + sender.getName().getString(),
        configData.request().expireTime(),
        true
    );

    if (configData.general().effect().soundEffect()) {
      target.level().playSound(
          null,
          target,
          SoundEvents.PLAYER_LEVELUP,
          SoundSource.PLAYERS,
          1.0f,
          2.0f
      );
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpAccept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    ServerPlayer sender = EntityArgument.getPlayer(ctx, "sender");
    if (!requestManager.accept(receiver, sender)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpAcceptLatest(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    if (!requestManager.acceptLatest(receiver)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeny(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    ServerPlayer sender = EntityArgument.getPlayer(ctx, "sender");
    if (!requestManager.deny(receiver, sender)) {
      PayTpMessageSender.msgNoAcceptRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDenyLatest(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer receiver = ctx.getSource().getPlayer();
    if (receiver == null) return 0;

    if (!requestManager.denyLatest(receiver)) {
      PayTpMessageSender.msgNoDenyRequest(receiver);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCancel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;

    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
    if (!requestManager.cancel(sender, target)) {
      PayTpMessageSender.msgNoCancelRequest(sender);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCancelLatest(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer sender = ctx.getSource().getPlayer();
    if (sender == null) return 0;

    if (!requestManager.cancelLatest(sender)) {
      PayTpMessageSender.msgNoCancelRequest(sender);
      return 0;
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpBack(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    PayTpData targetTp = backManager.popLastTp(player);
    if (targetTp == null) {
      PayTpMessageSender.msgNoBack(player);
      return 0;
    }

    TeleportResult result = teleport(
        player,
        targetTp,
        false,
        PayTpTeleportType.BACK,
        ""
    );

    if (result != TeleportResult.SUCCESS) {
      backManager.pushSingle(player, targetTp);
    }

    return result.commandResult();
  }

  private static int payTpHome(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    if (!homeManager.hasHome(player)) {
      PayTpMessageSender.msgHomeNotSet(player);
      return 0;
    }

    PayTpData home = homeManager.getHome(player);

    TeleportResult result = teleport(
        player,
        home,
        true,
        PayTpTeleportType.HOME,
        ""
    );

    if (result == TeleportResult.SUCCESS) {
      PayTpMessageSender.msgTpHome(player);
    }

    return result.commandResult();
  }

  private static int payTpSetHome(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    homeManager.setHome(player);
    PayTpMessageSender.msgHomeSet(player);

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpWarp(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    PayTpData target = warpManager.getWarp(player, name);
    if (target == null) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }

    return PayTpCommand.teleport(
        player,
        target,
        true,
        PayTpTeleportType.WARP,
        ""
    ).commandResult();
  }

  private static CompletableFuture<Suggestions> payTpWarpSuggest(
      CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder
  ) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player = source.getPlayer();
    if (player == null) return builder.buildFuture();

    Map<String, PayTpData> warps = warpManager.getAllWarps(player);
    if (warps != null) {
      for (String name : warps.keySet()) {
        builder.suggest(name);
      }
    }

    return builder.buildFuture();
  }

  private static int payTpCreateWarp(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    MinecraftServer server = ctx.getSource().getServer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");

    if (warpManager.hasWarp(player, name)) {
      PayTpMessageSender.msgWarpExist(player, name);
      return 0;
    }

    if (!warpManager.createWarp(player, name)) {
      PayTpMessageSender.msgWarpCreateFailed(player, name);
      return 0;
    }

    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      PayTpMessageSender.msgWarpCreated(onlinePlayer, player, name);
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeleteWarp(CommandContext<CommandSourceStack> ctx) {
    MinecraftServer server = ctx.getSource().getServer();
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    if (!warpManager.deleteWarp(player, name)) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }

    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      PayTpMessageSender.msgWarpDeleted(onlinePlayer, player, name);
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpListWarp(CommandContext<CommandSourceStack> ctx, int page) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    Map<String, PayTpData> warps = warpManager.getAllWarps(player);
    if (warps.isEmpty()) {
      PayTpMessageSender.msgEmptyWarp(player);
    } else {
      PayTpMessageSender.msgWarpList(
          player,
          warps,
          configData.warp().warpCommand(),
          configData.warp().warpCommand() + " list",
          page
      );
    }

    return Command.SINGLE_SUCCESS;
  }

  private static TeleportResult teleport(
      ServerPlayer player,
      PayTpData targetData,
      boolean recordToBackStack,
      PayTpTeleportType teleportType,
      String otherPlayer
  ) {
    // ---------------------------------
    // Fetch teleport info
    // ---------------------------------
    MinecraftServer server = player.level().getServer();
    ServerLevel targetWorld = server.getLevel(targetData.world());
    if (targetWorld == null) {
      LOGGER.error("Failed to teleport to null world");
      return TeleportResult.FAILED;
    }

    ServerLevel fromWorld = player.level();
    PayTpData fromData = new PayTpData(fromWorld.dimension(), player.position());

    // ---------------------------------
    // Check dimension
    // ---------------------------------
    if (!configData.teleport().allowCrossDim()
        && !fromData.world().equals(targetData.world())) {
      PayTpMessageSender.msgCrossDimensionDisabled(player);
      return TeleportResult.CROSS_DIMENSION_DISABLED;
    }

    // ---------------------------------
    // Check payment
    // ---------------------------------
    int price = PayTpCalculator.calculatePrice(
        fromData,
        targetData,
        teleportType,
        player.getName().getString(),
        otherPlayer,
        configData.price()
    );

    int balance = PayTpCalculator.checkBalance(configData.price().currencyItem(), player, configData.combineSettingFlags());
    if (balance < price) {
      PayTpMessageSender.msgTpFailed(
          player,
          (new ItemStack(PayTpItemHandler.getItemByStringId(configData.price().currencyItem()))).getHoverName(),
          price,
          balance
      );

      return TeleportResult.INSUFFICIENT_FUNDS;
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
    if (!PayTpCalculator.proceedPayment(configData.price().currencyItem(), player, price, configData.combineSettingFlags())) {
      LOGGER.error("Payment proceed failed");
      return TeleportResult.FAILED;
    }

    // ---------------------------------
    // Pre-teleport effect
    // ---------------------------------
    // Particles
    if (configData.general().effect().particleEffect()) {
      fromWorld.broadcastEntityEvent(player, (byte)46);
    }

    // Sound
    if (configData.general().effect().soundEffect()) {
      fromWorld.playSound(
          null,
          new BlockPos(
              (int) Math.round(fromData.pos().x),
              (int) Math.round(fromData.pos().y),
              (int) Math.round(fromData.pos().z)
          ),
          SoundEvents.ENDER_EYE_DEATH,
          SoundSource.PLAYERS,
          1.0f,
          2.0f
      );
    }

    // ---------------------------------
    // Execute teleport
    // ---------------------------------
    TeleportTransition teleportTarget = new TeleportTransition(
        targetWorld,
        targetData.pos(),
        player.getDeltaMovement(),
        player.getYRot(),
        player.getXRot(),
        entity -> {
          ServerPlayer playerEntity = (ServerPlayer) entity;
          ServerLevel toWorld = server.getLevel(targetData.world());
          if (toWorld == null) {
            LOGGER.error("No world to teleport player {}.", player.getName());
            return;
          }

          // Particles
          if (configData.general().effect().particleEffect()) {
            toWorld.broadcastEntityEvent(playerEntity, (byte)46);
          }

          // Sound
          if (configData.general().effect().soundEffect()) {
            toWorld.playSound(
                null,
                playerEntity.blockPosition(),
                SoundEvents.PLAYER_TELEPORT,
                SoundSource.PLAYERS,
                1.0f,
                1.5f
            );
          }

          // Message
          if (teleportType == PayTpTeleportType.BACK) {
            PayTpMessageSender.msgTpBackSucceeded(
                playerEntity,
                (new ItemStack(PayTpItemHandler.getItemByStringId(configData.price().currencyItem()))).getHoverName(),
                price
            );
          } else {
            PayTpMessageSender.msgTpSucceeded(
                playerEntity,
                (new ItemStack(PayTpItemHandler.getItemByStringId(configData.price().currencyItem()))).getHoverName(),
                price
            );
          }
        }
    );

    player.teleport(teleportTarget);
    return TeleportResult.SUCCESS;
  }

  private enum TeleportResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    CROSS_DIMENSION_DISABLED,
    FAILED;

    private int commandResult() {
      return this == SUCCESS ? Command.SINGLE_SUCCESS : 0;
    }
  }

}
