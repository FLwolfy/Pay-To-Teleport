package com.flwolfy.paytp.command;

import com.flwolfy.paytp.config.PayTpConfig;
import com.flwolfy.paytp.config.PayTpConfigData;
import com.flwolfy.paytp.util.PayTpCalculator;

import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpTextFormatter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class PayTpCommand {

  private static PayTpConfigData configData;
  private static PayTpRequest requestManager;

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    configData = PayTpConfig.getInstance().data();
    requestManager = PayTpRequest.getInstance();

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
    PlayerEntity player = ctx.getSource().getPlayer();
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
          MutableText failedMessage = Text.empty()
              .append(PayTpTextFormatter.format("paytp.teleport", PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
                Text.translatable("paytp.failed").getString()
              )).append(PayTpTextFormatter.format("paytp.target-not-enough"));
          target.sendMessage(failedMessage, false);
        }
      }, () -> {
        sender.sendMessage(PayTpTextFormatter.format("paytp.cancel"), false);
        target.sendMessage(PayTpTextFormatter.format("paytp.cancel"), false);
      });

      sender.sendMessage(PayTpTextFormatter.format("paytp.request",
          target.getName().getString()
      ), false);

      MutableText requestMessage = (MutableText) PayTpTextFormatter.format("paytp.receive", sender.getName().getString());
      requestMessage.append(Text.translatable("paytp.accept").setStyle(
          Style.EMPTY.withColor(PayTpTextFormatter.DEFAULT_HIGHLIGHT_COLOR)
              .withClickEvent(new ClickEvent(
                  ClickEvent.Action.RUN_COMMAND,
                  "/" + configData.acceptName()
              ))
              .withHoverEvent(new HoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  PayTpTextFormatter.format("paytp.hover",
                      Text.translatable("paytp.accept").getString()
                  )
              ))
      ));
      requestMessage.append(Text.translatable("paytp.deny").setStyle(
          Style.EMPTY.withColor(PayTpTextFormatter.DEFAULT_WARN_COLOR)
              .withClickEvent(new ClickEvent(
                  ClickEvent.Action.RUN_COMMAND,
                  "/" + configData.cancelName()
              ))
              .withHoverEvent(new HoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  PayTpTextFormatter.format("paytp.hover",
                      PayTpTextFormatter.DEFAULT_TEXT_COLOR,
                      PayTpTextFormatter.DEFAULT_WARN_COLOR,
                      Text.translatable("paytp.deny").getString()
                  )
              ))
      ));

      target.sendMessage(requestMessage, false);
    } else if (sender != null) {
      sender.sendMessage(PayTpTextFormatter.format("paytp.no-target"), false);
    }

    return 0;
  }

  private static int payTpAccept(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver != null) {
      if (!requestManager.accept(receiver)) {
        receiver.sendMessage(PayTpTextFormatter.format("paytp.no-accept"), false);
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
        receiver.sendMessage(PayTpTextFormatter.format("paytp.no-cancel"), false);
        return 0;
      }
    }
    return 0;
  }

  private static int teleport(PlayerEntity player, Vec3d to) {
    int price = PayTpCalculator.calculatePrice(configData.baseRadius(), configData.rate(), configData.minPrice(), configData.maxPrice(), player.getPos(), to);
    int balance = PayTpCalculator.checkBalance(configData.currencyItem(), player, configData.flags());

    MutableText message = Text.empty();
    if (balance >= price) {
      PayTpCalculator.proceedPayment(configData.currencyItem(), player, price, configData.flags());
      player.teleport(to.x, to.y, to.z, true);

      message = message
          .append(PayTpTextFormatter.format("paytp.teleport",
              Text.translatable("paytp.success").getString()
          ))
          .append(Text.literal("\n"))
          .append(PayTpTextFormatter.format("paytp.consume",
              price,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString()
          ));
    } else {
      message = message
          .append(PayTpTextFormatter.format("paytp.teleport", PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
              Text.translatable("paytp.failed").getString()
          ))
          .append(Text.literal("\n"))
          .append(PayTpTextFormatter.format("paytp.not-enough", PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
              price,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName().getString(),
              balance
          ));
    }

    player.sendMessage(message, false);
    return balance >= price ? Command.SINGLE_SUCCESS : 0;
  }
}
