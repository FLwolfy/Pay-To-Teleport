package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.lang.PayTpLangManager;
import com.flwolfy.paytp.flag.Flags;
import com.flwolfy.paytp.flag.PayTpMultiplierFlags;
import com.flwolfy.paytp.util.PayTpCalculator;
import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpMessageSender;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import org.slf4j.Logger;

public class PayTpCommand {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final String HELP_COMMAND = "ptp";

  private static PayTpConfigManager configManager;
  private static PayTpLangManager langManager;
  private static PayTpBackManager backManager;
  private static PayTpRequestManager requestManager;
  private static PayTpHomeManager homeManager;
  private static PayTpWarpManager warpManager;
  
  private static PayTpConfigData configData;

  private PayTpCommand() {}
  
  public static void init() {
    // Init manager singletons
    configManager = PayTpConfigManager.getInstance();
    langManager = PayTpLangManager.getInstance();
    backManager = PayTpBackManager.getInstance();
    requestManager = PayTpRequestManager.getInstance();
    homeManager = PayTpHomeManager.getInstance();
    warpManager = PayTpWarpManager.getInstance();
  }

  public static void reload() {
    // Config data
    configData = configManager.data();

    // Config content
    langManager.setLanguage(configData.general().language());
    backManager.setMaxBackStack(configData.back().maxBackStack());
    warpManager.setMaxInactiveTicks(configData.warp().maxInactiveTicks());
    warpManager.setCheckPeriodTicks(configData.warp().checkPeriodTicks());
  }

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    // ===== /ptp =====
    dispatcher.register(CommandManager.literal(HELP_COMMAND)
        .executes(PayTpCommand::payTpHelp)
    );

    // ===== /ptp (dimension) <pos> =====
    String mainCmd = configData.general().mainCommand();
    if (!mainCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(mainCmd)
          .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
              .executes(PayTpCommand::payTpCoords))
          .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
              .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                  .executes(PayTpCommand::payTpDimCoords)
              )
          )
      );
    }

    // ===== /ptpback =====
    String backCmd = configData.back().backCommand();
    if (!backCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(backCmd)
          .executes(PayTpCommand::payTpBack)
      );
    }

    // ===== /ptpto <player> =====
    String tpToCmd = configData.request().requestCommand().toCommand();
    if (!tpToCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(tpToCmd)
          .then(CommandManager.argument("target", EntityArgumentType.player())
              .executes(PayTpCommand::payTpPlayer))
      );
    }

    // ===== /ptphere <player> =====
    String tpHereCmd = configData.request().requestCommand().hereCommand();
    if (!tpHereCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(tpHereCmd)
          .then(CommandManager.argument("target", EntityArgumentType.player())
              .executes(PayTpCommand::payTpPlayerHere))
      );
    }

    // ===== /ptpaccept (player) =====
    String acceptCmd = configData.request().requestCommand().acceptCommand();
    if (!acceptCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(acceptCmd)
          .executes(PayTpCommand::payTpAcceptLatest)
          .then(CommandManager.argument("sender", EntityArgumentType.player())
              .executes(PayTpCommand::payTpAccept))
      );
    }

    // ===== /ptpdeny (player) =====
    String denyCmd = configData.request().requestCommand().denyCommand();
    if (!denyCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(denyCmd)
          .executes(PayTpCommand::payTpDenyLatest)
          .then(CommandManager.argument("sender", EntityArgumentType.player())
              .executes(PayTpCommand::payTpDeny))
      );
    }

    // ===== /ptpcancel (player) =====
    String cancelCmd = configData.request().requestCommand().cancelCommand();
    if (!cancelCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(cancelCmd)
          .executes(PayTpCommand::payTpCancelLatest)
          .then(CommandManager.argument("target", EntityArgumentType.player())
              .executes(PayTpCommand::payTpCancel))
      );
    }

    // ===== /ptphome =====
    String homeCmd = configData.home().homeCommand();
    if (!homeCmd.isEmpty()) {
      dispatcher.register(CommandManager.literal(homeCmd)
          .executes(PayTpCommand::payTpHome)
          .then(CommandManager.literal("set")
              .executes(PayTpCommand::payTpSetHome))
      );
    }

    // ===== /ptpwarp =====
    String warpCmd = configData.warp().warpCommand();
    dispatcher.register(CommandManager.literal(warpCmd)
        .then(CommandManager.literal("create")
            .then(CommandManager.argument("name", StringArgumentType.greedyString())
                .executes(PayTpCommand::payTpCreateWarp)
            )
        )
        .then(CommandManager.literal("delete")
            .then(CommandManager.argument("name", StringArgumentType.greedyString())
                .executes(PayTpCommand::payTpDeleteWarp)
            )
        )
        .then(CommandManager.literal("list")
            .executes(ctx -> payTpListWarp(ctx, 1))
            .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> payTpListWarp(ctx, IntegerArgumentType.getInteger(ctx, "page")))
            )
        )
        .then(CommandManager.argument("name", StringArgumentType.greedyString())
            .suggests(PayTpCommand::payTpWarpSuggest)
            .executes(PayTpCommand::payTpWarp)
        )
    );

  }

  private static int payTpHelp(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    PayTpMessageSender.msgHelp(
        player,
        configData.general().mainCommand(),
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

    if (configData.setting().effect().soundEffect()) {
      target.getServerWorld().playSoundFromEntity(
          null,
          target,
          SoundEvents.ENTITY_PLAYER_LEVELUP,
          SoundCategory.PLAYERS,
          1.0f,
          2.0f
      );
    }

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

    if (configData.setting().effect().soundEffect()) {
      target.getServerWorld().playSoundFromEntity(
          null,
          target,
          SoundEvents.ENTITY_PLAYER_LEVELUP,
          SoundCategory.PLAYERS,
          1.0f,
          2.0f
      );
    }

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
    MinecraftServer server = ctx.getSource().getServer();
    if (player == null) return 0;

    if (!homeManager.hasHome(player)) {
      PayTpMessageSender.msgHomeNotSet(player);
      return 0;
    }

    PayTpData home = homeManager.getHome(player);
    ServerWorld targetWorld = server.getWorld(home.world());
    if (targetWorld == null) return 0;

    PayTpData targetTp = new PayTpData(targetWorld.getRegistryKey(), home.pos());

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

  private static int payTpWarp(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    PayTpData target = warpManager.getWarp(player, name);
    if (target == null) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }

    int multiplierFlags = player.getServerWorld().getRegistryKey() == target.world() ?
        Flags.combine(PayTpMultiplierFlags.WARP) :
        Flags.combine(PayTpMultiplierFlags.CROSS_DIMENSION, PayTpMultiplierFlags.WARP);

    return PayTpCommand.teleport(
        player,
        target,
        true,
        multiplierFlags
    );
  }

  private static CompletableFuture<Suggestions> payTpWarpSuggest(
      CommandContext<ServerCommandSource> context,
      SuggestionsBuilder builder
  ) {
    ServerCommandSource source = context.getSource();
    ServerPlayerEntity player = source.getPlayer();
    if (player == null) return builder.buildFuture();

    Map<String, PayTpData> warps = warpManager.getAllWarps(player);
    if (warps != null) {
      for (String name : warps.keySet()) {
        builder.suggest(name);
      }
    }

    return builder.buildFuture();
  }

  private static int payTpCreateWarp(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
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

    for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
      PayTpMessageSender.msgWarpCreated(onlinePlayer, player, name);
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeleteWarp(CommandContext<ServerCommandSource> ctx) {
    MinecraftServer server = ctx.getSource().getServer();
    ServerPlayerEntity player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    if (!warpManager.deleteWarp(player, name)) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }

    for (ServerPlayerEntity onlinePlayer : server.getPlayerManager().getPlayerList()) {
      PayTpMessageSender.msgWarpDeleted(onlinePlayer, player, name);
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpListWarp(CommandContext<ServerCommandSource> ctx, int page) {
    ServerPlayerEntity player = ctx.getSource().getPlayer();
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
      LOGGER.error("No server found for player {}", player.getName());
      return 0;
    }

    ServerWorld targetWorld = server.getWorld(targetData.world());
    if (targetWorld == null) {
      LOGGER.error("No world found for player {}", player.getName());
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
        configData.price().parameter().baseRadius(),
        configData.price().parameter().rate(),
        configData.calculateMultiplier(multiplierFlags),
        configData.price().parameter().minPrice(),
        configData.price().parameter().maxPrice()
    );

    int balance = PayTpCalculator.checkBalance(configData.price().currencyItem(), player, configData.combineSettingFlags());
    if (balance < price) {
      PayTpMessageSender.msgTpFailed(
          player,
          PayTpItemHandler.getItemByStringId(configData.price().currencyItem()).getName(),
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
    if (!PayTpCalculator.proceedPayment(configData.price().currencyItem(), player, price, configData.combineSettingFlags())) {
      LOGGER.error("Payment proceed failed");
      return 0;
    }

    // ---------------------------------
    // Pre-teleport effect
    // ---------------------------------
    // Particles
    if (configData.setting().effect().particleEffect()) {
      fromWorld.sendEntityStatus(player, (byte)46);
    }

    // Sound
    if (configData.setting().effect().soundEffect()) {
      fromWorld.playSound(
          null,
          new BlockPos(
              (int) Math.round(fromData.pos().x),
              (int) Math.round(fromData.pos().y),
              (int) Math.round(fromData.pos().z)
          ),
          SoundEvents.ENTITY_ENDER_EYE_DEATH,
          SoundCategory.PLAYERS,
          1.0f,
          2.0f
      );
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
          ServerPlayerEntity playerEntity = (ServerPlayerEntity) entity;
          ServerWorld toWorld = server.getWorld(targetData.world());
          if (toWorld == null) {
            LOGGER.error("No world to teleport player {}.", player.getName());
            return;
          }

          // Particles
          if (configData.setting().effect().particleEffect()) {
            toWorld.sendEntityStatus(playerEntity, (byte)46);
          }

          // Sound
          if (configData.setting().effect().soundEffect()) {
            toWorld.playSound(
                null,
                playerEntity.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_TELEPORT,
                SoundCategory.PLAYERS,
                1.0f,
                1.5f
            );
          }

          // Message
          if (Flags.check(multiplierFlags, PayTpMultiplierFlags.BACK)) {
            PayTpMessageSender.msgTpBackSucceeded(
                playerEntity,
                PayTpItemHandler.getItemByStringId(configData.price().currencyItem()).getName(),
                price
            );
          } else {
            PayTpMessageSender.msgTpSucceeded(
                playerEntity,
                PayTpItemHandler.getItemByStringId(configData.price().currencyItem()).getName(),
                price
            );
          }
        }
    );

    player.teleportTo(teleportTarget);
    return Command.SINGLE_SUCCESS;
  }

}
