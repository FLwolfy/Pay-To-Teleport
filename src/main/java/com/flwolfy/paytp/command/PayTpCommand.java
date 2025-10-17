package com.flwolfy.paytp.command;

import com.flwolfy.paytp.config.PayTpConfig;
import com.flwolfy.paytp.config.PayTpConfigData;
import com.flwolfy.paytp.util.PayTpCalculator;

import com.flwolfy.paytp.util.PayTpItemHandler;
import com.flwolfy.paytp.util.PayTpTextFormatter;

import com.flwolfy.paytp.util.PayTpTextLoader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.ControlFlowAware.Command;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
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
  private static PayTpTextLoader textLoader;

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    configData = PayTpConfig.getInstance().data();
    requestManager = PayTpRequest.getInstance();
    textLoader = PayTpTextLoader.getInstance(configData.language());

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
          MutableText failedMessage = Text.empty()
              .append(PayTpTextFormatter.format(textLoader.getText("paytp.teleport"), PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
                  textLoader.getText("paytp.failed")
              )).append(PayTpTextFormatter.format(textLoader.getText("paytp.target-not-enough")));
          target.sendMessage(failedMessage, false);
        }
      }, () -> {
        sender.sendMessage(PayTpTextFormatter.format(textLoader.getText("paytp.cancel")), false);
        target.sendMessage(PayTpTextFormatter.format(textLoader.getText("paytp.cancel")), false);
      }, configData.expireTime());

      sender.sendMessage(PayTpTextFormatter.format(textLoader.getText("paytp.request"),
          target.getName()
      ), false);

      MutableText requestMessage = (MutableText) PayTpTextFormatter.format(textLoader.getText("paytp.receive"),
          sender.getName(),
          configData.expireTime()
      );
      requestMessage.append(textLoader.getText("paytp.accept").setStyle(
          Style.EMPTY.withColor(PayTpTextFormatter.DEFAULT_HIGHLIGHT_COLOR)
              .withClickEvent(new ClickEvent(
                  ClickEvent.Action.RUN_COMMAND,
                  "/" + configData.acceptName()
              ))
              .withHoverEvent(new HoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  PayTpTextFormatter.format(textLoader.getText("paytp.hover"),
                      textLoader.getText("paytp.accept")
                  )
              ))
      ));
      requestMessage.append(textLoader.getText("paytp.deny").setStyle(
          Style.EMPTY.withColor(PayTpTextFormatter.DEFAULT_WARN_COLOR)
              .withClickEvent(new ClickEvent(
                  ClickEvent.Action.RUN_COMMAND,
                  "/" + configData.cancelName()
              ))
              .withHoverEvent(new HoverEvent(
                  HoverEvent.Action.SHOW_TEXT,
                  PayTpTextFormatter.format(textLoader.getText("paytp.hover"),
                      PayTpTextFormatter.DEFAULT_TEXT_COLOR,
                      PayTpTextFormatter.DEFAULT_WARN_COLOR,
                      textLoader.getText("paytp.deny")
                  )
              ))
      ));

      target.sendMessage(requestMessage, false);
    } else if (sender != null) {
      sender.sendMessage(PayTpTextFormatter.format(textLoader.getText("paytp.no-target")), false);
    }

    return 0;
  }

  private static int payTpAccept(CommandContext<ServerCommandSource> ctx) {
    ServerPlayerEntity receiver = ctx.getSource().getPlayer();
    if (receiver != null) {
      if (!requestManager.accept(receiver)) {
        receiver.sendMessage(PayTpTextFormatter.format(textLoader.getText("paytp.no-accept")), false);
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
        receiver.sendMessage(PayTpTextFormatter.format(textLoader.getText("paytp.no-cancel")), false);
        return 0;
      }
    }
    return 0;
  }

  private static int teleport(ServerPlayerEntity player, Vec3d to) {
    int price = PayTpCalculator.calculatePrice(configData.baseRadius(), configData.rate(), configData.minPrice(), configData.maxPrice(), player.getPos(), to);
    int balance = PayTpCalculator.checkBalance(configData.currencyItem(), player, configData.flags());

    MutableText message = Text.empty();
    if (balance >= price) {
      PayTpCalculator.proceedPayment(configData.currencyItem(), player, price, configData.flags());
      player.requestTeleport(to.x, to.y, to.z);

      message = message
          .append(PayTpTextFormatter.format(textLoader.getText("paytp.teleport"),
              textLoader.getText("paytp.success")
          ))
          .append(Text.literal("\n"))
          .append(PayTpTextFormatter.format(textLoader.getText("paytp.consume"),
              price,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName()
          ));
    } else {
      message = message
          .append(PayTpTextFormatter.format(textLoader.getText("paytp.teleport"), PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
              textLoader.getText("paytp.failed")
          ))
          .append(Text.literal("\n"))
          .append(PayTpTextFormatter.format(textLoader.getText("paytp.not-enough"), PayTpTextFormatter.DEFAULT_TEXT_COLOR, PayTpTextFormatter.DEFAULT_WARN_COLOR,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName(),
              price,
              PayTpItemHandler.getItemByStringId(configData.currencyItem()).getName(),
              balance
          ));
    }

    player.sendMessage(message, false);
    return balance >= price ? Command.SINGLE_SUCCESS : 0;
  }
}
