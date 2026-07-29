package com.flwolfy.paytp.command;

import com.flwolfy.paytp.PayTpMod;
import com.flwolfy.paytp.command.back.PayTpBackManager;
import com.flwolfy.paytp.command.home.PayTpHomeManager;
import com.flwolfy.paytp.command.request.PayTpRequestManager;
import com.flwolfy.paytp.command.warp.PayTpWarpManager;
import com.flwolfy.paytp.command.warp.PayTpWarpNameArgument;
import com.flwolfy.paytp.data.config.PayTpConfigData;
import com.flwolfy.paytp.data.config.PayTpConfigManager;
import com.flwolfy.paytp.data.warp.PayTpWarpPermission;
import com.flwolfy.paytp.data.PayTpData;
import com.flwolfy.paytp.data.PayTpPlayer;
import com.flwolfy.paytp.data.PayTpTeleportContext;
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
import net.minecraft.commands.SharedSuggestionProvider;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

public class PayTpCommand {

  private static final Logger LOGGER = PayTpMod.LOGGER;
  private static final String POSITION_ARGUMENT = "x> <y> <z";
  private static PayTpConfigManager configManager;
  private static PayTpLangManager langManager;
  private static PayTpBackManager backManager;
  private static PayTpRequestManager requestManager;
  private static PayTpHomeManager homeManager;
  private static PayTpWarpManager warpManager;

  private static PayTpConfigData configData;

  private PayTpCommand() {}

  /**
   * Describes the outcome of a PayTp teleport operation.
   */
  private enum PayTpTeleportResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    CROSS_DIMENSION_DISABLED,
    FAILED;

    public int commandResult() {
      return this == SUCCESS ? Command.SINGLE_SUCCESS : 0;
    }
  }

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
              .then(Commands.argument(POSITION_ARGUMENT, Vec3Argument.vec3())
                  .suggests((context, builder) -> builder.buildFuture())
                  .executes(PayTpCommand::payTpCoords));

      if (configData.teleport().allowCrossDim()) {
        coordinateCommand.then(Commands.argument("dimension", DimensionArgument.dimension())
            .then(Commands.argument(POSITION_ARGUMENT, Vec3Argument.vec3())
                .suggests((context, builder) -> builder.buildFuture())
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
          .then(Commands.argument("target", StringArgumentType.word())
              .suggests(PayTpCommand::onlinePlayerSuggest)
              .executes(PayTpCommand::payTpPlayer))
      );
    }

    // ===== /ptphere <player> =====
    String tpHereCmd = configData.request().requestCommand().hereCommand();
    if (!tpHereCmd.isEmpty()) {
      dispatcher.register(Commands.literal(tpHereCmd)
          .then(Commands.argument("target", StringArgumentType.word())
              .suggests(PayTpCommand::onlinePlayerSuggest)
              .executes(PayTpCommand::payTpPlayerHere))
      );
    }

    // ===== /ptpaccept (player) =====
    String acceptCmd = configData.request().requestCommand().acceptCommand();
    if (!acceptCmd.isEmpty()) {
      dispatcher.register(Commands.literal(acceptCmd)
          .executes(PayTpCommand::payTpAcceptLatest)
          .then(Commands.argument("sender", StringArgumentType.word())
              .suggests(PayTpCommand::onlinePlayerSuggest)
              .executes(PayTpCommand::payTpAccept))
      );
    }

    // ===== /ptpdeny (player) =====
    String denyCmd = configData.request().requestCommand().denyCommand();
    if (!denyCmd.isEmpty()) {
      dispatcher.register(Commands.literal(denyCmd)
          .executes(PayTpCommand::payTpDenyLatest)
          .then(Commands.argument("sender", StringArgumentType.word())
              .suggests(PayTpCommand::onlinePlayerSuggest)
              .executes(PayTpCommand::payTpDeny))
      );
    }

    // ===== /ptpcancel (player) =====
    String cancelCmd = configData.request().requestCommand().cancelCommand();
    if (!cancelCmd.isEmpty()) {
      dispatcher.register(Commands.literal(cancelCmd)
          .executes(PayTpCommand::payTpCancelLatest)
          .then(Commands.argument("target", StringArgumentType.word())
              .suggests(PayTpCommand::onlinePlayerSuggest)
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
            .then(Commands.argument("name", PayTpWarpNameArgument.warpName())
                .executes(ctx -> payTpCreateWarp(ctx, false))
                .then(Commands.literal("private")
                    .executes(ctx -> payTpCreateWarp(ctx, false)))
                .then(Commands.literal("public")
                    .executes(ctx -> payTpCreateWarp(ctx, true)))
                .then(Commands.literal("server")
                    .requires(PayTpCommand::canManageServerWarps)
                    .executes(PayTpCommand::payTpCreateServerWarp))
            )
        )
        .then(Commands.literal("delete")
            .then(Commands.argument("name", PayTpWarpNameArgument.warpName())
                .suggests(PayTpCommand::payTpDeleteWarpSuggest)
                .executes(PayTpCommand::payTpDeleteWarp)
                .then(Commands.literal("forced")
                    .requires(PayTpCommand::canManageServerWarps)
                    .executes(PayTpCommand::payTpDeleteWarpForced))
            )
        )
        .then(Commands.literal("rename")
            .then(Commands.argument("name", PayTpWarpNameArgument.warpName())
                .suggests(PayTpCommand::payTpOwnedWarpSuggest)
                .then(Commands.argument("newName", PayTpWarpNameArgument.warpName())
                    .executes(PayTpCommand::payTpRenameWarp)
                )
            )
        )
        .then(Commands.literal("invite")
            .then(Commands.argument("name", PayTpWarpNameArgument.warpName())
                .suggests(PayTpCommand::payTpOwnedPrivateWarpSuggest)
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(PayTpCommand::onlinePlayerSuggest)
                    .executes(PayTpCommand::payTpInviteWarp)
                )
            )
        )
        .then(Commands.literal("exclude")
            .then(Commands.argument("name", PayTpWarpNameArgument.warpName())
                .suggests(PayTpCommand::payTpOwnedPrivateWarpSuggest)
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests(PayTpCommand::onlinePlayerSuggest)
                    .executes(PayTpCommand::payTpExcludeWarp)
                )
            )
        )
        .then(Commands.literal("list")
            .executes(ctx -> payTpListWarp(ctx, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> payTpListWarp(ctx, IntegerArgumentType.getInteger(ctx, "page")))
            )
            .then(Commands.literal("all")
                .executes(ctx -> payTpListWarp(
                    ctx,
                    null,
                    1
                ))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> payTpListWarp(
                        ctx,
                        null,
                        IntegerArgumentType.getInteger(ctx, "page")
                    ))
                )
            )
            .then(Commands.literal("public")
                .executes(ctx -> payTpListWarp(
                    ctx,
                    PayTpWarpManager.AccessType.PUBLIC,
                    1
                ))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> payTpListWarp(
                        ctx,
                        PayTpWarpManager.AccessType.PUBLIC,
                        IntegerArgumentType.getInteger(ctx, "page")
                    ))
                )
            )
            .then(Commands.literal("server")
                .executes(ctx -> payTpListWarp(
                    ctx,
                    PayTpWarpManager.AccessType.SERVER,
                    1
                ))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> payTpListWarp(
                        ctx,
                        PayTpWarpManager.AccessType.SERVER,
                        IntegerArgumentType.getInteger(ctx, "page")
                    ))
                )
            )
            .then(Commands.literal("owned")
                .executes(ctx -> payTpListWarp(
                    ctx,
                    PayTpWarpManager.AccessType.OWNED,
                    1
                ))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> payTpListWarp(
                        ctx,
                        PayTpWarpManager.AccessType.OWNED,
                        IntegerArgumentType.getInteger(ctx, "page")
                    ))
                )
            )
            .then(Commands.literal("invited")
                .executes(ctx -> payTpListWarp(
                    ctx,
                    PayTpWarpManager.AccessType.INVITED,
                    1
                ))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> payTpListWarp(
                        ctx,
                        PayTpWarpManager.AccessType.INVITED,
                        IntegerArgumentType.getInteger(ctx, "page")
                    ))
                )
            )
        )
        .then(Commands.argument("name", StringArgumentType.greedyString())
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
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " rename",
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " invite",
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " exclude",
        configData.warp().warpCommand().isEmpty() ? "" : configData.warp().warpCommand() + " list"
    );

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCoords(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    Vec3 targetPos = Vec3Argument.getVec3(ctx, POSITION_ARGUMENT);

    if (player == null) return 0;

    PayTpData payTpData = new PayTpData(player.level().dimension(), targetPos);
    return teleport(
        player,
        payTpData,
        true,
        PayTpTeleportContext.coordinate(new PayTpTeleportContext.Coordinate())
    ).commandResult();
  }

  private static int payTpDimCoords(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayer();
    ServerLevel targetDim = DimensionArgument.getDimension(ctx, "dimension");
    Vec3 targetPos = Vec3Argument.getVec3(ctx, POSITION_ARGUMENT);

    if (player == null) return 0;

    PayTpData payTpData = new PayTpData(targetDim.dimension(), targetPos);

    return teleport(
        player,
        payTpData,
        true,
        PayTpTeleportContext.coordinate(new PayTpTeleportContext.Coordinate())
    ).commandResult();
  }

  private static int payTpPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer sender = ctx.getSource().getPlayer();
    ServerPlayer target = getOnlinePlayer(ctx, "target");

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

      PayTpTeleportResult result = teleport(
          sender,
          targetTp,
          true,
          PayTpTeleportContext.request(new PayTpTeleportContext.Request(
              new PayTpPlayer(
                  target.getUUID().toString(),
                  target.getName().getString()
              ),
              true
          ))
      );

      if (result == PayTpTeleportResult.SUCCESS) {
        PayTpMessageSender.msgTpAccepted(target, sender.getName());
      } else if (result == PayTpTeleportResult.INSUFFICIENT_FUNDS) {
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

    ServerPlayer target = getOnlinePlayer(ctx, "target");
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
          PayTpTeleportContext.request(new PayTpTeleportContext.Request(
              new PayTpPlayer(
                  sender.getUUID().toString(),
                  sender.getName().getString()
              ),
              false
          ))
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

    ServerPlayer sender = getOnlinePlayer(ctx, "sender");
    if (sender == null) {
      PayTpMessageSender.msgNoTargetFound(receiver);
      return 0;
    }
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

    ServerPlayer sender = getOnlinePlayer(ctx, "sender");
    if (sender == null) {
      PayTpMessageSender.msgNoTargetFound(receiver);
      return 0;
    }
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

    ServerPlayer target = getOnlinePlayer(ctx, "target");
    if (target == null) {
      PayTpMessageSender.msgNoTargetFound(sender);
      return 0;
    }
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

    PayTpTeleportResult result = teleport(
        player,
        targetTp,
        false,
        PayTpTeleportContext.back(new PayTpTeleportContext.Back())
    );

    if (result != PayTpTeleportResult.SUCCESS) {
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

    PayTpTeleportResult result = teleport(
        player,
        home,
        true,
        PayTpTeleportContext.home(new PayTpTeleportContext.Home())
    );

    if (result == PayTpTeleportResult.SUCCESS) {
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
    PayTpWarpManager.WarpView warp = warpManager.getWarpView(player, name);
    if (warp == null || !warp.accessible()) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }

    return PayTpCommand.teleport(
        player,
        warp.destination(),
        true,
        PayTpTeleportContext.warp(new PayTpTeleportContext.Warp(
            warp.name(),
            warp.scriptAccessType(),
            warp.ownerId() == null
                ? null
                : new PayTpPlayer(
                    warp.ownerId().toString(),
                    warp.ownerName()
                )
        ))
    ).commandResult();
  }

  private static CompletableFuture<Suggestions> payTpOwnedWarpSuggest(
      CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder
  ) {
    return suggestOwnedWarps(context, builder, false);
  }

  private static CompletableFuture<Suggestions> payTpDeleteWarpSuggest(
      CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder
  ) {
    ServerPlayer player = context.getSource().getPlayer();
    if (player == null) return builder.buildFuture();

    boolean admin = canManageServerWarps(context.getSource());
    List<String> names = admin
        ? warpManager.getAllWarpNames(player)
        : warpManager.getOwnedWarpNames(player, false);
    for (String name : names) {
      builder.suggest(StringArgumentType.escapeIfRequired(name));
    }
    return builder.buildFuture();
  }

  private static boolean canManageServerWarps(CommandSourceStack source) {
    PayTpWarpPermission permission =
        configData.warp().serverWarpPermission();
    return switch (permission) {
      case ALL -> true;
      case MODERATORS ->
          Commands.hasPermission(Commands.LEVEL_MODERATORS).test(source);
      case GAMEMASTERS ->
          Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
      case ADMINS ->
          Commands.hasPermission(Commands.LEVEL_ADMINS).test(source);
      case OWNERS ->
          Commands.hasPermission(Commands.LEVEL_OWNERS).test(source);
    };
  }

  private static CompletableFuture<Suggestions> payTpOwnedPrivateWarpSuggest(
      CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder
  ) {
    return suggestOwnedWarps(context, builder, true);
  }

  private static CompletableFuture<Suggestions> suggestOwnedWarps(
      CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder,
      boolean privateOnly
  ) {
    ServerPlayer player = context.getSource().getPlayer();
    if (player == null) return builder.buildFuture();

    for (String name : warpManager.getOwnedWarpNames(player, privateOnly)) {
      builder.suggest(StringArgumentType.escapeIfRequired(name));
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> onlinePlayerSuggest(
      CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder
  ) {
    return SharedSuggestionProvider.suggest(
        context.getSource().getOnlinePlayerNames(),
        builder
    );
  }

  private static ServerPlayer getOnlinePlayer(
      CommandContext<CommandSourceStack> context,
      String argumentName
  ) {
    String playerName = StringArgumentType.getString(context, argumentName);
    return context.getSource()
        .getServer()
        .getPlayerList()
        .getPlayerByName(playerName);
  }

  private static int payTpCreateWarp(
      CommandContext<CommandSourceStack> ctx,
      boolean publicWarp
  ) {
    ServerPlayer player = ctx.getSource().getPlayer();
    MinecraftServer server = ctx.getSource().getServer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");

    if (warpManager.hasWarp(player, name)) {
      PayTpMessageSender.msgWarpExist(player, name);
      return 0;
    }

    PayTpWarpManager.OperationResult createResult =
        warpManager.createWarp(player, name, publicWarp);
    if (createResult != PayTpWarpManager.OperationResult.SUCCESS) {
      if (createResult == PayTpWarpManager.OperationResult.BEACON_OCCUPIED) {
        PayTpMessageSender.msgWarpBeaconOccupied(player, name);
      } else if (createResult == PayTpWarpManager.OperationResult.ALREADY_EXISTS) {
        PayTpMessageSender.msgWarpExist(player, name);
      } else {
        PayTpMessageSender.msgWarpBeaconInactive(player, name);
      }
      return 0;
    }

    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      PayTpMessageSender.msgWarpCreated(
          onlinePlayer,
          player,
          name,
          publicWarp
      );
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpCreateServerWarp(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    MinecraftServer server = ctx.getSource().getServer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    PayTpWarpManager.OperationResult result =
        warpManager.createServerWarp(player, name);
    if (result != PayTpWarpManager.OperationResult.SUCCESS) {
      PayTpMessageSender.msgWarpExist(player, name);
      return 0;
    }

    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      PayTpMessageSender.msgServerWarpCreated(
          onlinePlayer,
          player,
          name
      );
    }
    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeleteWarp(CommandContext<CommandSourceStack> ctx) {
    MinecraftServer server = ctx.getSource().getServer();
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    if (warpManager.getWarp(player, name) == null) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }
    if (warpManager.isServer(player, name)) {
      if (canManageServerWarps(ctx.getSource())) {
        PayTpMessageSender.msgServerWarpDeleteRequiresForced(player, name);
      } else {
        PayTpMessageSender.msgServerWarpNoPermission(player, name);
      }
      return 0;
    }
    if (!warpManager.isOwner(player, name)) {
      PayTpMessageSender.msgWarpNotOwner(player, name);
      return 0;
    }

    warpManager.deleteWarp(player, name);

    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      PayTpMessageSender.msgWarpDeleted(onlinePlayer, player, name);
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpDeleteWarpForced(CommandContext<CommandSourceStack> ctx) {
    MinecraftServer server = ctx.getSource().getServer();
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    boolean serverWarp = warpManager.isServer(player, name);
    if (!warpManager.deleteWarpForced(player, name)) {
      PayTpMessageSender.msgNoWarp(player, name);
      return 0;
    }

    for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
      if (serverWarp) {
        PayTpMessageSender.msgServerWarpDeleted(
            onlinePlayer,
            player,
            name
        );
      } else {
        PayTpMessageSender.msgWarpForceDeleted(onlinePlayer, player, name);
      }
    }
    return Command.SINGLE_SUCCESS;
  }

  private static int payTpListWarp(CommandContext<CommandSourceStack> ctx, int page) {
    return payTpListWarp(ctx, null, page);
  }

  private static int payTpListWarp(
      CommandContext<CommandSourceStack> ctx,
      PayTpWarpManager.AccessType filter,
      int page
  ) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    var warps = warpManager.getVisibleWarps(player, filter);
    if (warps.isEmpty()) {
      PayTpMessageSender.msgEmptyWarp(player, filter);
    } else {
      PayTpMessageSender.msgWarpList(
          player,
          warps,
          configData.warp().warpCommand(),
          configData.warp().warpCommand() + " list",
          filter,
          page
      );
    }

    return Command.SINGLE_SUCCESS;
  }

  private static int payTpRenameWarp(
      CommandContext<CommandSourceStack> ctx
  ) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    String newName = StringArgumentType.getString(ctx, "newName");
    PayTpWarpManager.OperationResult result =
        warpManager.renameWarp(player, name, newName);

    switch (result) {
      case SUCCESS -> PayTpMessageSender.msgWarpRenamed(player, name, newName);
      case ALREADY_EXISTS -> PayTpMessageSender.msgWarpExist(player, newName);
      case SERVER_WARP -> {
        if (canManageServerWarps(ctx.getSource())) {
          PayTpMessageSender.msgServerWarpRenameFailed(player, name);
        } else {
          PayTpMessageSender.msgServerWarpNoPermission(player, name);
        }
      }
      case NOT_OWNER -> PayTpMessageSender.msgWarpNotOwner(player, name);
      default -> PayTpMessageSender.msgNoWarp(player, name);
    }
    return result == PayTpWarpManager.OperationResult.SUCCESS
        ? Command.SINGLE_SUCCESS
        : 0;
  }

  private static int payTpInviteWarp(
      CommandContext<CommandSourceStack> ctx
  ) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    ServerPlayer target = getOnlinePlayer(ctx, "target");
    if (target == null) {
      PayTpMessageSender.msgNoTargetFound(player);
      return 0;
    }
    PayTpWarpManager.OperationResult result =
        warpManager.invite(player, name, target);
    sendWarpInviteResult(player, target, name, result, true);
    return result == PayTpWarpManager.OperationResult.SUCCESS
        ? Command.SINGLE_SUCCESS
        : 0;
  }

  private static int payTpExcludeWarp(
      CommandContext<CommandSourceStack> ctx
  ) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    String name = StringArgumentType.getString(ctx, "name");
    ServerPlayer target = getOnlinePlayer(ctx, "target");
    if (target == null) {
      PayTpMessageSender.msgNoTargetFound(player);
      return 0;
    }
    PayTpWarpManager.OperationResult result =
        warpManager.exclude(player, name, target);
    sendWarpInviteResult(player, target, name, result, false);
    return result == PayTpWarpManager.OperationResult.SUCCESS
        ? Command.SINGLE_SUCCESS
        : 0;
  }

  private static void sendWarpInviteResult(
      ServerPlayer player,
      ServerPlayer target,
      String name,
      PayTpWarpManager.OperationResult result,
      boolean invite
  ) {
    switch (result) {
      case SUCCESS -> {
        if (invite) {
          PayTpMessageSender.msgWarpInvited(player, target, name);
        } else {
          PayTpMessageSender.msgWarpExcluded(player, target, name);
        }
      }
      case PUBLIC_WARP -> PayTpMessageSender.msgWarpPublicOnly(player, name);
      case ALREADY_INVITED -> PayTpMessageSender.msgWarpAlreadyInvited(player, target, name);
      case NOT_INVITED -> PayTpMessageSender.msgWarpNotInvited(player, target, name);
      case SELF_EXCLUDE -> PayTpMessageSender.msgWarpExcludeSelf(player, name);
      case NOT_OWNER -> PayTpMessageSender.msgWarpNotOwner(player, name);
      default -> PayTpMessageSender.msgNoWarp(player, name);
    }
  }

  private static PayTpTeleportResult teleport(
      ServerPlayer player,
      PayTpData targetData,
      boolean recordToBackStack,
      PayTpTeleportContext teleportContext
  ) {
    // ---------------------------------
    // Fetch teleport info
    // ---------------------------------
    MinecraftServer server = player.level().getServer();
    ServerLevel targetWorld = server.getLevel(targetData.world());
    if (targetWorld == null) {
      LOGGER.error("Failed to teleport to null world");
      return PayTpTeleportResult.FAILED;
    }

    ServerLevel fromWorld = player.level();
    PayTpData fromData = new PayTpData(fromWorld.dimension(), player.position());

    // ---------------------------------
    // Check dimension
    // ---------------------------------
    if (!configData.teleport().allowCrossDim()
        && !fromData.world().equals(targetData.world())) {
      PayTpMessageSender.msgCrossDimensionDisabled(player);
      return PayTpTeleportResult.CROSS_DIMENSION_DISABLED;
    }

    // ---------------------------------
    // Check payment
    // ---------------------------------
    int price;
    try {
      price = PayTpCalculator.calculatePrice(
          fromData,
          targetData,
          teleportContext,
          player,
          configData.price()
      );
      if (price < 0) {
        LOGGER.warn("Price algorithm canceled payment with result {}", price);
        PayTpMessageSender.msgPaymentError(player);
        return PayTpTeleportResult.FAILED;
      }

      int balance = PayTpCalculator.checkBalance(
          configData.price().currencyItem(),
          player,
          configData.deductionFlags()
      );
      if (balance < price) {
        PayTpMessageSender.msgTpFailed(
            player,
            (new ItemStack(PayTpItemHandler.getItemByStringId(
                configData.price().currencyItem()
            ))).getHoverName(),
            price,
            balance
        );

        return PayTpTeleportResult.INSUFFICIENT_FUNDS;
      }

      if (!PayTpCalculator.proceedPayment(
          configData.price().currencyItem(),
          player,
          price,
          configData.deductionFlags()
      )) {
        LOGGER.error("Payment proceed failed");
        PayTpMessageSender.msgPaymentError(player);
        return PayTpTeleportResult.FAILED;
      }
    } catch (Exception e) {
      LOGGER.error("Payment process failed", e);
      PayTpMessageSender.msgPaymentError(player);
      return PayTpTeleportResult.FAILED;
    }

    // ---------------------------------
    // Record to back stack
    // ---------------------------------
    if (recordToBackStack) {
      backManager.pushPair(player, fromData, targetData);
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
          if (teleportContext.back() != null) {
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
    return PayTpTeleportResult.SUCCESS;
  }

}
